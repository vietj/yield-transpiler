package synctest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class Generator {

  private final GeneratorContext context;

  public Generator() {
    this.context = new GeneratorContext();
  }

  public Object next() {
    return next((Object) null);
  }

  public Object next(Object o) {
    if (context.status == -1) {
      throw new IllegalStateException("Done");
    }
    context.argument = o;
    return next(context);
  }

  protected abstract Object next(GeneratorContext context);
}
