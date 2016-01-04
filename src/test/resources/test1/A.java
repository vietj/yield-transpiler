package test1;

import synctest.Helper;
import synctest.Sync;

import java.util.function.Consumer;

public class A {

  @Sync
  public void sync() {
    String result = Helper.awaitResult(c -> blocking(c));
    System.out.println(result);
  }

  public void blocking(Consumer<String> callback) {
    callback.accept("something");
  }
}