package vertx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.Test;
import synctest.Generator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTest {

  @Test
  public void testExample1() throws Exception {
    Vertx vertx = Vertx.vertx();
    Example1 ex = new Example1(vertx);
    CompletableFuture<Object> a = new CompletableFuture<>();
    VertxFlow flow = new VertxFlow(vertx);
    Generator generator = new Example1_(ex).businessMethod();
    Future<Object> fut = flow.spawn(generator);
    fut.setHandler(ar -> {
      if (ar.succeeded()) {
        a.complete(ar.result());
      } else {
        a.completeExceptionally(ar.cause());
      }
    });
    assertEquals("foobarjuu", a.get(1, TimeUnit.SECONDS));
  }
}
