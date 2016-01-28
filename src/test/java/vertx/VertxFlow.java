package vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import synctest.Generator;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxFlow {

  public Future<Object> spawn(Generator generator) {
    Future<Object> first = Future.future();
    Future<Object> last = Future.future();
    run(generator, first, last);
    first.complete();
    return last;
  }

  public void run(Generator generator, Future<Object> prev, Future<Object> next) {
    prev.setHandler(ar -> {
      try {
        Object result;
        if (ar.succeeded()) {
          result = generator.next(ar.result());
        } else {
          result = generator.fail(ar.cause());
        }
        if (result instanceof Future<?>) {
          run(generator, (Future<Object>) result, next);
        } else {
          next.complete(result);
        }
      } catch (Throwable err) {
        next.fail(err);
      }
    });
  }

  public static <T> Future<T> future(Handler<Handler<T>> tutu) {
    Future<T> future = Future.future();
    tutu.handle(future::complete);
    return future;
  }

}
