package test_try_fail_after_yield;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    try {
      Flow.yield();
      SyncTest.fail();
    } catch (Exception e) {
      SyncTest.output.add("failed");
    }
    SyncTest.output.add("after");
  }
}