package synctest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Helper {

  public static <T> T yield() {
    throw new UnsupportedOperationException("Should never be invoked directly");
  }

  public static void yield(Object value) {
  }

}
