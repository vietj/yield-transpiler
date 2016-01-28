package vertx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.Test;
import synctest.Generator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTest {

  @Test
  public void testHandlers() throws Exception {
    Vertx vertx = Vertx.vertx();
    Example1 ex = new Example1(vertx);
    Generator generator = new Example1_(ex).handlers();
    assertEquals("foobarjuu", run(generator));
  }

  @Test
  public void testAsyncResultSuccess() throws Exception {
    Vertx vertx = Vertx.vertx();
    Example1 ex = new Example1(vertx);
    Generator generator = new Example1_(ex).asyncResultSuccess();
    assertEquals("the_success", run(generator));
  }

  @Test
  public void testAsyncResultFailure() throws Exception {
    Vertx vertx = Vertx.vertx();
    Example1 ex = new Example1(vertx);
    Generator generator = new Example1_(ex).asyncResultFailure();
    try {
      run(generator);
    } catch (ExecutionException e) {
      assertEquals("the_failure", e.getCause().getMessage());
    }
  }

  private Object run(Generator generator) throws InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<Object> a = new CompletableFuture<>();
    VertxFlow flow = new VertxFlow();
    Future<Object> fut = flow.spawn(generator);
    fut.setHandler(ar -> {
      if (ar.succeeded()) {
        a.complete(ar.result());
      } else {
        a.completeExceptionally(ar.cause());
      }
    });
    return a.get(1, TimeUnit.SECONDS);
  }
}
