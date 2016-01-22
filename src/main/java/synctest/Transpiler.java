package synctest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Transpiler extends TreePathScanner<Object, Object> {

  private final List<VariableTree> variables = new ArrayList<>();
  private final StringBuilder imports = new StringBuilder();
  private Map<Integer, Frame> frames = new LinkedHashMap<>();
  private int nextId = 0;
  private Frame currentFrame;

  private interface Statement {
    void render(String padding, StringBuilder buffer);
  }

  private class Frame {

    final int id;
    final List<Statement> statements;
    boolean suspend = true;
    ExpressionTree out;
    Frame next;

    Frame() {
      this.id = nextId++;
      this.statements = new ArrayList<>();
    }
    Frame append(String statement) {
      statements.add((padding, buffer) -> buffer.append(padding).append(statement).append("\n"));
      return this;
    }

    void render(StringBuilder buffer) {
      for (Statement statement : statements) {
        statement.render("              ", buffer);
      }
    }
  }

  private Frame newFrame() {
    Frame frame = new Frame();
    frames.put(frame.id, frame);
    return frame;
  }

  @Override
  public Object visitImport(ImportTree node, Object o) {
    imports.append("import ").append(node.getQualifiedIdentifier()).append(";\n");
//    return super.visitImport(node, v);
    return null;
  }

  public String visitMethod(TreePath node) {
    variables.clear();
    currentFrame = newFrame();

    scan(node, null);

    StringBuilder source = new StringBuilder();
    source.append(imports);
    source.append("public class ").append("GeneratorImpl").append(" {\n");

    for (VariableTree variable : variables) {
      String s = "  " + variable.getType() + " " + variable.getName() + ";\n";
      source.append(s);
    }


    source.append("  public synctest.Generator ").append("create() {\n");
    source.append("    class Impl implements synctest.Generator {\n");
    source.append("      public Object next(synctest.Context context) {\n");

    source.append("        while(true) {\n");
    source.append("          switch(context.status) {\n");
    for (Frame frame : frames.values()) {
      source.append("            case ").append(frame.id).append(": {\n");
      frame.render(source);
      if (frame.next != null) {
        source.append("              context.status = ").append(frame.next.id).append(";\n");
      }
      if (frame.suspend) {
        String out = frame.out != null ? frame.out.toString() : "null";
        source.append("              return ").append(out).append(";\n");
      } else {
        source.append("              break;\n");
      }
      source.append("            }\n");
    }
    source.append("          }\n");
    source.append("        }\n");
    source.append("      }\n");
    source.append("    }\n");

    source.append("    return new Impl();\n");
    source.append("  }\n");
    source.append("}\n");

    return source.toString();
  }

  @Override
  public Object visitMethod(MethodTree node, Object o) {
    o = super.visitMethod(node, o);

    return o;
  }

  @Override
  public Object visitVariable(VariableTree node, Object o) {
    variables.add(node);
    if (node.getInitializer() != null) {
      currentFrame.append(node.getName() + " = " + node.getInitializer() + ";");
    }
    return super.visitVariable(node, o);
  }

  @Override
  public Object visitAssignment(AssignmentTree node, Object o) {
    currentFrame.append(node.toString() + ";");
    return super.visitAssignment(node, o);
  }

  @Override
  public Object visitIf(IfTree node, Object o) {

    Frame current = currentFrame;
    int index = current.statements.size();

    node.getThenStatement().accept(this, null);

    if (currentFrame != current) {

      Frame abc = currentFrame;

      Frame next = newFrame();

      abc.suspend = false;
      abc.next = next;

      int afterIf = next.id;
      if (node.getElseStatement() != null) {
        Frame elseFrame = newFrame();
        afterIf = elseFrame.id;
        currentFrame = elseFrame;
        node.getElseStatement().accept(this, o);
        if (elseFrame != currentFrame) {
          elseFrame = currentFrame;
        }
        elseFrame.suspend = false;
        elseFrame.next = next;
      }

      currentFrame = next;

      int a = afterIf;
      current.statements.add(index, (padding, buffer) -> {
        buffer.append(padding).append("if (!").append(node.getCondition()).append(") {\n");
        buffer.append(padding).append("  context.status = ").append(a).append(";\n");
        buffer.append(padding).append("  break;\n");
        buffer.append(padding).append("};\n");
      });


    } else {
      throw new UnsupportedOperationException();
    }

    return o;
  }

  @Override
  public Object visitForLoop(ForLoopTree node, Object o) {

    Frame initializerFrame = currentFrame;

    Frame current = newFrame();
    currentFrame = current;
    int index = current.statements.size();

    node.getStatement().accept(this, o);

    if (current != currentFrame) {

      initializerFrame.suspend = false;
      initializerFrame.next = current;

      node.getInitializer().forEach(initializer -> {
        if (initializer instanceof VariableTree) {
          VariableTree var = (VariableTree) initializer;
          initializerFrame.append(var.getName() + " = " + var.getInitializer() + ";");
          variables.add(var);
        } else {
          throw new UnsupportedOperationException();
        }
      });

      Frame afterFrame = newFrame();

      current.statements.add(0, (padding, buffer) -> {
        buffer.append(padding).append("if (!(").append(node.getCondition()).append(")) {\n");
        buffer.append(padding).append("  context.status = ").append(afterFrame.id).append(";\n");
        buffer.append(padding).append("  break;\n");
        buffer.append(padding).append("}\n");
      });

      node.getUpdate().forEach(update -> {
        currentFrame.append(update.toString() + ";");
      });

      currentFrame.suspend = false;
      currentFrame.next = current;

      currentFrame = afterFrame;

    } else {
      List<Statement> sub = current.statements.subList(index, currentFrame.statements.size());
      sub.clear();
      current.statements.add((padding, buffer) -> {
        Utils.splitBySep(node.toString()).forEach(line -> {
          buffer.append(padding).append(line).append("\n");
        });

      });
      initializerFrame.suspend = false;
      initializerFrame.next = current;
    }

    return o;
  }

  @Override
  public Object visitMethodInvocation(MethodInvocationTree node, Object o) {
    ExpressionTree select = node.getMethodSelect();
    if (select.getKind() == Tree.Kind.MEMBER_SELECT) {
      MemberSelectTree memberSelect = (MemberSelectTree) select;
      if (memberSelect.getExpression().getKind() == Tree.Kind.IDENTIFIER) {
        IdentifierTree ident = (IdentifierTree) memberSelect.getExpression();
        if (ident.getName().toString().equals("Helper")) {
          if (memberSelect.getIdentifier().toString().equals("yield")) {
            if (node.getArguments().size() == 1) {
              currentFrame.out = node.getArguments().get(0);
            }
            currentFrame.next = newFrame();
            currentFrame = currentFrame.next;
            return o;
          }
        }
      }
    }
    currentFrame.append(node.toString() + ";");
    return o;
  }
}
