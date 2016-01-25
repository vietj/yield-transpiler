package synctest;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class Utils {

  static void uncheckedThrow(Throwable throwable) {
    Utils.<RuntimeException>throwIt(throwable);
  }

  private static <T extends Throwable> void throwIt(Throwable throwable) throws T {
    throw (T)throwable;
  }

  static List<String> splitBySep(String s) {
    String sep = System.getProperty("line.separator");
    List<String> list = new ArrayList<>();
    int prev = 0;
    while (true) {
      int next = s.indexOf(sep, prev);
      if (next == -1) {
        break;
      }
      list.add(s.substring(prev, next));
      prev = next + 1;
    }
    if (prev < s.length()) {
      list.add(s.substring(prev));
    }
    return list;
  }

  /**
   * @return true when the given block contains a yield instruction
   */
  static boolean hasYield(BlockTree block) {
    return new TreeScanner<Boolean, Object>() {
      @Override
      public Boolean reduce(Boolean r1, Boolean r2) {
        if (r1 == null) {
          r1 = false;
        }
        if (r2 == null) {
          r2 = false;
        }
        return r1 || r2;
      }
      @Override
      public Boolean visitMethodInvocation(MethodInvocationTree node, Object o) {
        return isYield(node);
      }
    }.visitBlock(block, null) == Boolean.TRUE;
  }

  /**
   * @return true when it is a yieled
   */
  static boolean isYield(MethodInvocationTree methodInvocation) {
    ExpressionTree select = methodInvocation.getMethodSelect();
    if (select.getKind() == Tree.Kind.MEMBER_SELECT) {
      MemberSelectTree memberSelect = (MemberSelectTree) select;
      if (memberSelect.getExpression().getKind() == Tree.Kind.IDENTIFIER) {
        IdentifierTree ident = (IdentifierTree) memberSelect.getExpression();
        if (ident.getName().toString().equals("Flow")) {
          if (memberSelect.getIdentifier().toString().equals("yield")) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
