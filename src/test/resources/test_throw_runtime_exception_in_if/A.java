package test_throw_runtime_exception_in_if;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    if (true) {
      Flow.yield();
      throw new RuntimeException("the runtime exception");
    }
    SyncTest.output.add("after");
  }
}