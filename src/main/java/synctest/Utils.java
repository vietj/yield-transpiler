package synctest;

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

}
