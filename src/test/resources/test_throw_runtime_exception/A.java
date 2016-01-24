package test_throw_runtime_exception;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    Flow.yield();
    throw new RuntimeException("the runtime exception");
  }
}