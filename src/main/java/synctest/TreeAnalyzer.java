package synctest;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TreeAnalyzer extends TreePathScanner<Void, Void> {


  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void v) {

    ExpressionTree select = node.getMethodSelect();
    if (select.getKind() == Tree.Kind.MEMBER_SELECT) {
      MemberSelectTree memberSelect = (MemberSelectTree) select;
      if (memberSelect.getExpression().getKind() == Tree.Kind.IDENTIFIER) {
        IdentifierTree ident = (IdentifierTree) memberSelect.getExpression();
        if (ident.getName().toString().equals("Helper")) {
          if (memberSelect.getIdentifier().toString().equals("awaitResult")) {
            // Should rewrite this !!!!
          }
        }
      }
    }
    return super.visitMethodInvocation(node, v);
  }
}
