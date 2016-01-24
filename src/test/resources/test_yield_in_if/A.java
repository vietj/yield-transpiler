package test_yield_in_if;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    if ("one".equals(SyncTest.value)) {
      SyncTest.output.add("foo");
      Flow.yield();
      SyncTest.output.add("bar");
    }
    SyncTest.output.add("after");
  }
}