package vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import synctest.Flow;
import synctest.GeneratorFunction;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Example1 {

  private final Vertx vertx;

  public Example1(Vertx vertx) {
    this.vertx = vertx;
  }

  @GeneratorFunction
  public String handlers() {
    String value1 = Flow.yield(VertxFlow.<String>future(c -> this.delay("foo", c)));
    String value2 = Flow.yield(VertxFlow.<String>future(c -> this.delay("bar", c)));
    String value3 = Flow.yield(VertxFlow.<String>future(c -> this.delay("juu", c)));
    return value1 + value2 + value3;
  }

  @GeneratorFunction
  public String asyncResultSuccess() {
    String result = Flow.yield(Future.succeededFuture("the_success"));
    return result;
  }

  @GeneratorFunction
  public String asyncResultFailure() {
    String result = Flow.yield(Future.failedFuture("the_failure"));
    return result;
  }

  public void delay(String s, Handler<String> resultHandler) {
    vertx.setTimer(10, id -> {
      resultHandler.handle(s);
    });
  }
}
