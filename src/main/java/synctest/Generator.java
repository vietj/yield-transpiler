package synctest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Generator {

  Object next(Context ctx);

}
