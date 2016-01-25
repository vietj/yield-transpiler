package test_try_fail_before_yield;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    try {
      SyncTest.fail();
      Flow.yield();
    } catch (Exception e) {
      SyncTest.output.add("failed");
    }
    SyncTest.output.add("after");
  }
}