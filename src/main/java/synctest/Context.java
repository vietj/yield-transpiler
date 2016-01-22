package synctest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Context {

  public int status;
  Object argument;

  public <T> T getArgument() {
    @SuppressWarnings("unchecked")
    T ret = (T) argument;
    argument = null;
    return ret;
  }

}
