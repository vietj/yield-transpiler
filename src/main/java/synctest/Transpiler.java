package synctest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
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

  private final List<VariableTree> variables = new ArrayList<>();
  private final StringBuilder imports = new StringBuilder();
  private Map<Integer, Frame> frames = new LinkedHashMap<>();
  private int frameCounter = 0;
  private Frame currentFrame;
  private int tryCounter = 0;
  private int currentTry;
  private Map<Integer, Map<String, Frame>> catchMapping = new LinkedHashMap<>();
  private int catchCounter = 0;

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
  public Object visitImport(ImportTree node, Object o) {
    imports.append("import ").append(node.getQualifiedIdentifier()).append(";\n");
//    return super.visitImport(node, v);
    return null;
  }

  public String visitMethod(TreePath node) {
    return (String) scan(node, null);
  }

  @Override
  public Object visitMethod(MethodTree node, Object o) {
    variables.clear();
    currentFrame = newFrame();

    visitBlock(node.getBody(), o);

    StringBuilder source = new StringBuilder();
    source.append(imports);
    source.append("public class ").append("GeneratorImpl").append(" {\n");

    source.append("  public synctest.Generator ").append(node.getName()).append("(");
    for (Iterator<? extends VariableTree> i = node.getParameters().iterator();i.hasNext();) {
      VariableTree param = i.next();
      source.append(param.getType()).append(" ").append(param.getName());
      if (i.hasNext()) {
        source.append(", ");
      }
    }
    source.append(") {\n");

    source.append("    class TheGenerator extends synctest.Generator {\n");

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
          String out = frame.out != null ? frame.out.toString() : "null";
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
          source.append("                return ").append(frame.exitExpr).append(";\n");
          break;
        case THROW:
          source.append("                context.status = -1;\n");
          source.append("                throw ").append(frame.exitExpr).append(";\n");
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

    source.append("    return new TheGenerator();\n");
    source.append("  }\n");
    source.append("}\n");

    //
    return source.toString();
  }

  @Override
  public Object visitVariable(VariableTree node, Object o) {
    variables.add(node);
    if (node.getInitializer() != null) {
      Frame frame = currentFrame;
      o = super.visitVariable(node, node);
      if (frame == currentFrame) {
        frame.append(node.getName() + " = " + node.getInitializer() + ";");
      }
    }
    return o;
  }

  @Override
  public Object visitAssignment(AssignmentTree node, Object o) {
    Frame frame = this.currentFrame;
    o = super.visitAssignment(node, node);
    if (frame == currentFrame) {
      frame.append(node.toString() + ";");
    }
    return o;
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
  public Object visitMethodInvocation(MethodInvocationTree node, Object o) {
    if (Utils.isYield(node)) {
      if (node.getArguments().size() == 1) {
        currentFrame.out = node.getArguments().get(0);
      }
      currentFrame.exit = Exit.SUSPEND;
      currentFrame.next = newFrame();
      currentFrame = currentFrame.next;

      if (o instanceof VariableTree) {
        VariableTree variableTree = (VariableTree) o;
        currentFrame.statements.add((padding, buffer) -> {
          buffer.append(padding).append(variableTree.getName()).append(" = context.resume();\n");
        });
      } else if (o instanceof AssignmentTree) {
        AssignmentTree assignmentTree = (AssignmentTree) o;
        currentFrame.statements.add((padding, buffer) -> {
          buffer.append(padding).append(assignmentTree.getVariable()).append(" = context.resume();\n");
        });
      } else {
        currentFrame.append("context.resume();\n");
      }
      return null;
    }
    currentFrame.append(node.toString() + ";");
    return o;
  }
}
