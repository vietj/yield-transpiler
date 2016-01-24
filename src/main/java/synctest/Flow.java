package synctest;

/**
 * Function that modifies the control flow of a program.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Flow {

  public static <T> T yield() {
    throw new UnsupportedOperationException("Should never be invoked directly");
  }

  public static <T> T yield(Object value) {
    throw new UnsupportedOperationException("Should never be invoked directly");
  }
}
