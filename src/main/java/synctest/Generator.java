package synctest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class Generator {

  private final Context context;

  public Generator() {
    this.context = new Context();
  }

  public Object next() {
    return next(context);
  }

  public Object next(Object o) {
    context.argument = o;
    return next(context);
  }

  protected abstract Object next(Context context);
}
