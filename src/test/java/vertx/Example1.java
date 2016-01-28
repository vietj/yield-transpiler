package vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import synctest.Flow;
import synctest.GeneratorFunction;
import vertx.VertxFlow;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Example1 {

  private final Vertx vertx;

  public Example1(Vertx vertx) {
    this.vertx = vertx;
  }

  @GeneratorFunction
  public String businessMethod() {
    String value1 = Flow.yield(VertxFlow.<String>wrap(c -> this.delay("foo", c)));
    String value2 = Flow.yield(VertxFlow.<String>wrap(c -> this.delay("bar", c)));
    String value3 = Flow.yield(VertxFlow.<String>wrap(c -> this.delay("juu", c)));
    return value1 + value2 + value3;
  }

  public void delay(String s, Handler<String> resultHandler) {
    vertx.setTimer(10, id -> {
      resultHandler.handle(s);
    });
  }
}
