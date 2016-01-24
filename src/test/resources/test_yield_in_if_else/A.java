package test_yield_in_if_else;

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
    } else {
      SyncTest.output.add("juu");
    }
    SyncTest.output.add("after");
  }
}