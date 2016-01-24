package synctest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GeneratorContext {

  private static final Object EMPTY = new Object();

  public int status;
  boolean failed;
  Object argument;

  public <T> T resume() {
    if (argument == EMPTY) {
      throw new AssertionError("Should not be called");
    }
    if (failed) {
      failed = false;
      Throwable cause = (Throwable) argument;
      argument = EMPTY;
      Utils.uncheckedThrow(cause);
      return null;
    } else {
      @SuppressWarnings("unchecked")
      T ret = (T) argument;
      argument = EMPTY;
      return ret;
    }
  }
}
