package synctest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
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
    int jump = -1;

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
    source.append("  public synctest.Iterator ").append("create() {\n");
    source.append("    class IteratorImpl implements synctest.Iterator {\n");
    source.append("      public void next(synctest.Context context) {\n");

    for (VariableTree variable : variables) {
      String s = "    " + variable.getType() + " " + variable.getName() + ";\n";
      source.insert(0, s);
    }
    source.append("        while(true) {\n");
    source.append("          switch(context.status) {\n");
    for (int idx = 0;idx < frames.size();idx++) {
      Frame frame = frames.get(idx);
      source.append("            case ").append(frame.id).append(": {\n");
      for (String statement : frame.statements) {
        source.append("              ").append(statement).append("\n");
      }
      if (frame.jump >= 0) {
        source.append("              context.status = ").append(frame.jump).append(";\n");
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
      abc.jump = next.id;

      int afterIf = next.id;
      if (node.getElseStatement() != null) {
        Frame elseFrame = new Frame();
        frames.add(elseFrame);
        node.getElseStatement().accept(this, o);
        elseFrame.suspend = false;
        elseFrame.jump = next.id;
        afterIf = elseFrame.id;
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

    return null;
  }

  @Override
  public Object visitMethodInvocation(MethodInvocationTree node, Object o) {
    if (isYield(node)) {
      frames.peekLast().jump = frames.size();
      beginFrame();
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
