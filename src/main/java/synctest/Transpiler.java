package synctest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Transpiler extends TreePathScanner<Object, Object> {

  private StringBuilder source = new StringBuilder();
  private final List<VariableTree> variables = new ArrayList<>();
  private Map<Integer, Frame> frames = new LinkedHashMap<>();
  private int frameCounter = 0;
  private Frame currentFrame;
  private int tryCounter = 0;
  private int currentTry;
  private Map<Integer, Map<String, Frame>> catchMapping = new LinkedHashMap<>();

  public String getSource() {
    return source.toString() + "}\n";
  }

  private interface Statement {
    void render(String padding, StringBuilder buffer);
  }

  enum Exit {
    SUSPEND, THROW, CONTINUE, RETURN
  }

  private class Frame {

    final int id;
    final int tryId = currentTry;
    final List<Statement> statements;
    Exit exit = Exit.CONTINUE;
    ExpressionTree exitExpr;
    ExpressionTree out;
    Frame next;

    Frame() {
      this.id = frameCounter++;
      this.statements = new ArrayList<>();
    }

    Frame append(String statement) {
      statements.add((padding, buffer) -> buffer.append(padding).append(statement).append("\n"));
      return this;
    }

    void append(StatementTree statement) {
      statements.add((padding, buffer) -> {
        Utils.splitBySep(statement.toString()).forEach(line -> {
          buffer.append(padding).append(line).append("\n");
        });
      });
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
  public Object visitCompilationUnit(CompilationUnitTree node, Object o) {
    source.append("package ").append(node.getPackageName()).append(";\n");
    node.getImports().forEach(import_ -> visitImport(import_, null));
    ClassTree decl = (ClassTree) node.getTypeDecls().get(0);
    source.append("public class ").append(decl.getSimpleName()).append("_ {\n");
    source.append("  private final ").append(decl.getSimpleName()).append(" this_;\n");
    source.append("  public ").append(decl.getSimpleName()).append("_(").append(decl.getSimpleName()).append(" this_) {\n");
    source.append("    this.this_ = this_;\n");
    source.append("}\n");
    return null;
  }

  @Override
  public Object visitImport(ImportTree node, Object o) {
    source.append("import ").append(node.getQualifiedIdentifier()).append(";\n");
    return null;
  }

  @Override
  public Object visitMethod(MethodTree node, Object o) {
    variables.clear();
    currentFrame = newFrame();

    visitBlock(node.getBody(), o);

    source.append("  public synctest.Generator ").append(node.getName()).append("(");
    for (Iterator<? extends VariableTree> i = node.getParameters().iterator();i.hasNext();) {
      VariableTree param = i.next();
      source.append(param.getType()).append(" ").append(param.getName());
      if (i.hasNext()) {
        source.append(", ");
      }
    }
    source.append(") {\n");

    source.append("    class GeneratorImpl extends synctest.Generator {\n");

    for (VariableTree variable : variables) {
      String s = "      private " + variable.getType() + " " + variable.getName() + ";\n";
      source.append(s);
    }

    source.append("      public Object next(synctest.GeneratorContext context) {\n");

    source.append("        while(true) {\n");
    source.append("          try {\n");
    source.append("            switch(context.status) {\n");
    for (Frame frame : frames.values()) {
      source.append("              case ").append(frame.id).append(": {\n");
      frame.render(source);
      switch (frame.exit) {
        case SUSPEND:
          source.append("                context.status = ").append(frame.next.id).append(";\n");
          String out = frame.out != null ? render(frame.out) : "null";
          source.append("                return ").append(out).append(";\n");
          break;
        case CONTINUE:
          if (frame.next != null) {
            source.append("                context.status = ").append(frame.next.id).append(";\n");
            source.append("                break;\n");
          } else {
            source.append("                context.status = -1;\n");
            source.append("                return null;\n");
          }
          break;
        case RETURN:
          source.append("                context.status = -1;\n");
          source.append("                return ").append(render(frame.exitExpr)).append(";\n");
          break;
        case THROW:
          source.append("                context.status = -1;\n");
          source.append("                throw ").append(render(frame.exitExpr)).append(";\n");
          break;
      }
      source.append("              }\n");
    }
    source.append("            }\n");
    source.append("          } catch(Throwable t) {\n");


    source.append("            switch (context.status) {\n");
    for (Frame frame : frames.values()) {
      if (frame.tryId > 0) {
        source.append("              case ").append(frame.id).append(": {\n");
        Map<String, Frame> abc = catchMapping.get(frame.tryId);
        abc.forEach((exception, def) -> {
          source.append("                if (t instanceof ").append(exception).append(") {\n");
          source.append("                  context.status = ").append(def.id).append(";\n");
          source.append("                  continue;\n");
          source.append("                }\n");
        });
        source.append("                break;\n");
        source.append("              }\n");
      }
    }
    source.append("            }\n");

    catchMapping.forEach((tryId, entry) -> {
    });

    source.append("            throw t;\n");
    source.append("          }\n");
    source.append("        }\n");
    source.append("      }\n");
    source.append("    }\n");

    source.append("    return new GeneratorImpl();\n");
    source.append("  }\n");

    //
    return source.toString();
  }

  @Override
  public Object visitIf(IfTree node, Object o) {

    Frame current = currentFrame;
    int index = current.statements.size();

    node.getThenStatement().accept(this, null);

    if (currentFrame != current) {

      Frame abc = currentFrame;

      Frame next = newFrame();

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

      currentFrame.next = current;

      currentFrame = afterFrame;

    } else {
      List<Statement> sub = current.statements.subList(index, currentFrame.statements.size());
      sub.clear();
      current.append(node);
      initializerFrame.next = current;
    }

    return o;
  }

  @Override
  public Object visitTry(TryTree node, Object o) {

    int prevTry = currentTry;
    int thisTry = ++tryCounter;
    currentTry = thisTry;
    Frame nextFrame = newFrame();
    currentFrame.next = nextFrame;
    currentFrame = nextFrame;

    //
    o = node.getBlock().accept(this, o);

    //
    currentTry = prevTry;
    Frame afterFrame = newFrame();
    currentFrame.next = afterFrame;

    //
    LinkedHashMap<String, Frame> blah = new LinkedHashMap<>();
    catchMapping.put(thisTry, blah);
    node.getCatches().forEach(catchTree -> {
      Frame catchFrame = newFrame();
      currentFrame = catchFrame;
      blah.put("" + catchTree.getParameter().getType(), catchFrame);
      catchTree.getBlock().accept(this, null);
      currentFrame.next = afterFrame;
    });

    currentFrame = afterFrame;

    return o;
  }

  @Override
  public Object visitThrow(ThrowTree node, Object o) {
    currentFrame.exit = Exit.THROW;
    currentFrame.exitExpr = node.getExpression();
    return o;
  }

  @Override
  public Object visitReturn(ReturnTree node, Object o) {
    currentFrame.exit = Exit.RETURN;
    currentFrame.exitExpr = node.getExpression();
    return o;
  }

  @Override
  public Object visitVariable(VariableTree variable, Object o) {
    variables.add(variable);
    if (variable.getInitializer() instanceof MethodInvocationTree) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) variable.getInitializer();
      if (Utils.isYield(methodInvocation)) {
        visitYieldInvocation(methodInvocation);
        currentFrame.statements.add((padding, buffer) -> {
          buffer.append(padding).append(variable.getName()).append(" = context.resume();\n");
        });
        return null;
      }
    }
    if (variable.getInitializer() != null) {
      currentFrame.append(variable.getName() + " = " + variable.getInitializer() + ";");
    }
    return o;
  }

  @Override
  public Object visitExpressionStatement(ExpressionStatementTree node, Object o) {
    ExpressionTree expr = node.getExpression();
    if (expr instanceof MethodInvocationTree) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) expr;
      if (Utils.isYield(methodInvocation)) {
        visitYieldInvocation(methodInvocation);
        currentFrame.append("context.resume();\n");
        return o;
      }
    } else if (expr instanceof AssignmentTree) {
      AssignmentTree assignment = (AssignmentTree) expr;
      if (assignment.getExpression() instanceof MethodInvocationTree) {
        MethodInvocationTree methodInvocation = (MethodInvocationTree) assignment.getExpression();
        if (Utils.isYield(methodInvocation)) {
          visitYieldInvocation(methodInvocation);
          currentFrame.statements.add((padding, buffer) -> {
            buffer.append(padding).append(assignment.getVariable()).append(" = context.resume();\n");
          });
          return o;
        }
      }
    }
    o = expr.accept(this, o);
    if (o instanceof String) {
      currentFrame.append(o + ";");
    } else {
      throw new UnsupportedOperationException("todo");
    }
    return o;
  }

  private void visitYieldInvocation(MethodInvocationTree yieldInvocation) {
    if (yieldInvocation.getArguments().size() == 1) {
      currentFrame.out = yieldInvocation.getArguments().get(0);
    }
    currentFrame.exit = Exit.SUSPEND;
    currentFrame.next = newFrame();
    currentFrame = currentFrame.next;
  }

  // Expression visits just render the expression and do some rewriting

  private String render(ExpressionTree expr) {
    return (String) expr.accept(this, null);
  }

  @Override
  public String visitIdentifier(IdentifierTree node, Object o) {
    String identifier = "" + node.getName();
    if (identifier.equals("this")) {
      identifier = "this_";
    }
    return identifier;
  }

  @Override
  public Object visitParenthesized(ParenthesizedTree node, Object o) {
    return "(" + node.accept(this, o) + ")";
  }

  @Override
  public Object visitConditionalExpression(ConditionalExpressionTree node, Object o) {
    return node.getCondition().accept(this, o) + " ? " + node.getTrueExpression().accept(this, o) + " : " + node.getFalseExpression().accept(this, o);
  }

  @Override
  public Object visitNewClass(NewClassTree node, Object o) {
    // no support for anonymous inner classes
    // nor enclosing expression
    // nor type arguments
    StringBuilder tmp = new StringBuilder("new ").append(node.getIdentifier()).append("(");
    for (Iterator<? extends ExpressionTree> i = node.getArguments().iterator();i.hasNext();) {
      tmp.append(i.next().accept(this, o));
      if (i.hasNext()) {
        tmp.append(", ");
      }
    }
    tmp.append(")");
    return tmp.toString();
  }

  @Override
  public Object visitMethodInvocation(MethodInvocationTree node, Object o) {
    // no support for type arguments
    StringBuilder tmp = new StringBuilder();
    ExpressionTree methodSelect = node.getMethodSelect();
    if (methodSelect instanceof MemberSelectTree) {
      MemberSelectTree memberSelect = (MemberSelectTree) methodSelect;
      tmp.append(memberSelect.getExpression().accept(this, o));
      tmp.append(".");
      if (node.getTypeArguments().size() > 0) {
        tmp.append("<");
        for (Iterator<? extends Tree> i = node.getTypeArguments().iterator(); i.hasNext();) {
          tmp.append(i.next().accept(this, o));
          if (i.hasNext()) {
            tmp.append(", ");
          }
        }
        tmp.append(">");
      }
      tmp.append(memberSelect.getIdentifier());
    } else {
      throw new UnsupportedOperationException();
    }
    tmp.append("(");
    for (Iterator<? extends ExpressionTree> i = node.getArguments().iterator();i.hasNext();) {
      tmp.append(i.next().accept(this, o));
      if (i.hasNext()) {
        tmp.append(", ");
      }
    }
    tmp.append(")");
    return tmp.toString();
  }

  @Override
  public Object visitMemberSelect(MemberSelectTree node, Object o) {
    return node.getExpression().accept(this, o) + "." + node.getIdentifier();
  }

  @Override
  public Object visitLiteral(LiteralTree node, Object o) {
    return node.toString();
  }

  @Override
  public Object visitNewArray(NewArrayTree node, Object o) {
    throw new UnsupportedOperationException("todo");
  }

  @Override
  public Object visitLambdaExpression(LambdaExpressionTree node, Object o) {
    StringBuilder tmp = new StringBuilder("(");
    for (Iterator<? extends VariableTree> i = node.getParameters().iterator();i.hasNext();) {
      VariableTree variable = i.next();
      tmp.append(variable.getName());
      if (i.hasNext()) {
        tmp.append(", ");
      }
    }
    tmp.append(") -> ");
    if (node.getBodyKind() == LambdaExpressionTree.BodyKind.STATEMENT) {
      throw new UnsupportedOperationException("todo");
    } else {
      tmp.append(node.getBody().accept(this, o));
    }
    return tmp.toString();
  }

  @Override
  public Object visitAssignment(AssignmentTree node, Object o) {
    return node.getVariable().accept(this, o) + " = " + node.getExpression().accept(this, o);
  }

  @Override
  public Object visitBinary(BinaryTree node, Object o) {
    String op;
    switch (node.getKind()) {
      case PLUS:
        op = "+";
        break;
      case MINUS:
        op = "-";
        break;
      case MULTIPLY:
        op = "*";
        break;
      case DIVIDE:
        op = "/";
        break;
      default:
        throw new IllegalStateException();
    }
    return node.getLeftOperand().accept(this, o) + " " + op + "" + node.getRightOperand().accept(this, o);
  }
}
