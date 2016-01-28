package vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import synctest.Generator;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxFlow {

  public Future<Object> spawn(Generator generator) {
    return run(generator, null);
  }

  public Future<Object> run(Generator generator, Object arg) {
    Object next = generator.next(arg);
    if (next instanceof Future) {
      Future<Object> returnedFuture = Future.future();
      Future<?> nextFuture = (Future) next;
      nextFuture.setHandler(ar -> {
        if (ar.succeeded()) {
          Future<Object> tutu = run(generator, ar.result());
          tutu.setHandler(ar2 -> {
            if (ar2.succeeded()) {
              returnedFuture.complete(ar2.result());
            } else {
              returnedFuture.fail(ar2.cause());
            }
          });
        } else {
          returnedFuture.fail(ar.cause());
        }
      });
      return returnedFuture;
    } else {
      return Future.succeededFuture(next);
    }
  }

  public static <T> Future<T> future(Handler<Handler<T>> tutu) {
    Future<T> future = Future.future();
    tutu.handle(future::complete);
    return future;
  }

}
