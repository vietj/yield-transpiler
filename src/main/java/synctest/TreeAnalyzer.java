package synctest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TreeAnalyzer extends TreePathScanner<Object, Object> {

  private final List<VariableTree> variables = new ArrayList<>();
  private final StringBuilder imports = new StringBuilder();
  private List<String> statements = new ArrayList<>();
  private List<Frame> frames = new ArrayList<>();

  private static class Frame {
    final int id;
    final List<String> statements;
    Frame(int id, List<String> statements) {
      this.id = id;
      this.statements = statements;
    }
  }

  private void beginFrame() {

  }

  private void endFrame() {
    frames.add(new Frame(frames.size(), new ArrayList<>(statements)));
    statements.clear();
  }

  @Override
  public Object visitImport(ImportTree node, Object o) {
    imports.append("import ").append(node.getQualifiedIdentifier()).append(";\n");
//    return super.visitImport(node, v);
    return null;
  }

  @Override
  public String visitMethod(MethodTree node, Object o) {
    variables.clear();
    beginFrame();
    o = super.visitMethod(node, o);
    endFrame();

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
    source.append("        switch(context.status) {\n");
    for (Frame frame : frames) {
      source.append("          case ").append(frame.id).append(": {\n");
      for (String statement : frame.statements) {
        source.append("            ").append(statement).append(";\n");
      }
      source.append("            context.status = ").append(frame.id + 1).append(";\n");
      source.append("            break;\n");
      source.append("          }\n");
    }
    source.append("        }\n");
    source.append("      }\n");
    source.append("    }\n");

    source.append("    return new IteratorImpl();\n");
    source.append("  }\n");
    source.append("}\n");

    return source.toString();
  }

  @Override
  public Object visitVariable(VariableTree node, Object o) {
    variables.add(node);
    statements.add(node.getName() + " = " + node.getInitializer());
    return super.visitVariable(node, o);
  }

  @Override
  public Object visitAssignment(AssignmentTree node, Object o) {
    statements.add(node.toString());
    return super.visitAssignment(node, o);
  }

  @Override
  public Object visitMethodInvocation(MethodInvocationTree node, Object o) {
    if (isYield(node)) {
      beginFrame();
      endFrame();
    } else {
      statements.add(node.toString());
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
