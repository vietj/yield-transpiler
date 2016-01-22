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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TreeAnalyzer extends TreePathScanner<Object, Object> {

  private final List<VariableTree> variables = new ArrayList<>();
  private final StringBuilder imports = new StringBuilder();
  private LinkedList<Frame> frames = new LinkedList<>();
  private int nextId = 0;

  private class Frame {

    final int id;
    final List<String> statements;
    boolean suspend = true;
    Frame next;

    Frame() {
      this.id = nextId++;
      this.statements = new ArrayList<>();
    }
    Frame append(String statement) {
      statements.add(statement);
      return this;
    }
  }

  private void beginFrame() {
    frames.add(new Frame());
  }

  @Override
  public Object visitImport(ImportTree node, Object o) {
    imports.append("import ").append(node.getQualifiedIdentifier()).append(";\n");
//    return super.visitImport(node, v);
    return null;
  }

  public String visitMethod(TreePath node) {
    variables.clear();
    beginFrame();

    scan(node, null);

    StringBuilder source = new StringBuilder();
    source.append(imports);
    source.append("public class ").append("GeneratorImpl").append(" {\n");

    for (VariableTree variable : variables) {
      String s = "  " + variable.getType() + " " + variable.getName() + ";\n";
      source.append(s);
    }


    source.append("  public synctest.Iterator ").append("create() {\n");
    source.append("    class IteratorImpl implements synctest.Iterator {\n");
    source.append("      public void next(synctest.Context context) {\n");

    source.append("        while(true) {\n");
    source.append("          switch(context.status) {\n");
    for (int idx = 0;idx < frames.size();idx++) {
      Frame frame = frames.get(idx);
      source.append("            case ").append(frame.id).append(": {\n");
      for (String statement : frame.statements) {
        source.append("              ").append(statement).append("\n");
      }
      if (frame.next != null) {
        source.append("              context.status = ").append(frame.next.id).append(";\n");
      }
      source.append("              ").append(frame.suspend ? "return" : "break").append(";\n");
      source.append("            }\n");
    }
    source.append("          }\n");
    source.append("        }\n");
    source.append("      }\n");
    source.append("    }\n");

    source.append("    return new IteratorImpl();\n");
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
    frames.getLast().append(node.getName() + " = " + node.getInitializer() + ";");
    return super.visitVariable(node, o);
  }

  @Override
  public Object visitAssignment(AssignmentTree node, Object o) {
    frames.peekLast().append(node.toString() + ";");
    return super.visitAssignment(node, o);
  }

  @Override
  public Object visitIf(IfTree node, Object o) {

    Frame current = frames.peekLast();
    int index = current.statements.size();

    node.getThenStatement().accept(this, null);

    if (frames.peekLast() != current) {

      Frame abc = frames.peekLast();

      Frame next = new Frame();

      abc.suspend = false;
      abc.next = next;

      int afterIf = next.id;
      if (node.getElseStatement() != null) {
        Frame elseFrame = new Frame();
        afterIf = elseFrame.id;
        frames.add(elseFrame);
        node.getElseStatement().accept(this, o);
        if (elseFrame != frames.peekLast()) {
          elseFrame = frames.peekLast();
        }
        elseFrame.suspend = false;
        elseFrame.next = next;
      }

      frames.add(next);

      current.statements.addAll(index, Arrays.asList(
          "if (!" + node.getCondition() + ") {",
          "  context.status = " + afterIf + ";",
          "  break;",
          "}"
      ));


    } else {
      throw new UnsupportedOperationException();
    }

    return o;
  }

  @Override
  public Object visitForLoop(ForLoopTree node, Object o) {

    Frame initializerFrame = frames.peekLast();

    Frame current = new Frame();
    frames.add(current);
    int index = current.statements.size();

    node.getStatement().accept(this, o);

    if (current != frames.peekLast()) {

      initializerFrame.suspend = false;
      initializerFrame.next = current;

      node.getInitializer().forEach(initializer -> {
        if (initializer instanceof VariableTree) {
          VariableTree var = (VariableTree) initializer;
          initializerFrame.statements.add(var.getName() + " = " + var.getInitializer() + ";");
          variables.add(var);
        } else {
          throw new UnsupportedOperationException();
        }
      });

      Frame afterFrame = new Frame();

      current.statements.addAll(0, Arrays.asList(
          "if (!(" + node.getCondition() + ")) {",
          "  context.status = " + afterFrame.id + ";",
          "  break;",
          "}"
      ));

      node.getUpdate().forEach(update -> {
        frames.peekLast().append(update.toString() + ";");
      });

      frames.peekLast().suspend = false;
      frames.peekLast().next = current;

      frames.add(afterFrame);

    } else {
      List<String> before = new ArrayList<>();
      node.getInitializer().forEach(initializer -> {
        before.add(initializer.toString() + ";");
      });
      before.add("for (;" + node.getCondition() + ";" + node.getUpdate().get(0).getExpression() + ") {");
      current.statements.addAll(index, before);
      current.statements.add("}");
      initializerFrame.suspend = false;
      initializerFrame.next = current;
    }

    return o;
  }

  @Override
  public Object visitMethodInvocation(MethodInvocationTree node, Object o) {
    if (isYield(node)) {
      Frame last = frames.peekLast();
      beginFrame();
      last.next = frames.peekLast();
    } else {
      frames.peekLast().append(node.toString() + ";");
    }
    return o;
  }

  private boolean isYield(MethodInvocationTree node) {
    ExpressionTree select = node.getMethodSelect();
    if (select.getKind() == Tree.Kind.MEMBER_SELECT) {
      MemberSelectTree memberSelect = (MemberSelectTree) select;
      if (memberSelect.getExpression().getKind() == Tree.Kind.IDENTIFIER) {
        IdentifierTree ident = (IdentifierTree) memberSelect.getExpression();
        if (ident.getName().toString().equals("Helper")) {
          if (memberSelect.getIdentifier().toString().equals("yield")) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
