package test_yield_in_try_yield_in_catch;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before 1");
    try {
      SyncTest.fail();
      SyncTest.output.add("before 2");
      Flow.yield();
    } catch (Exception e) {
      SyncTest.output.add("failed 1");
      Flow.yield();
      SyncTest.output.add("failed 2");
    }
    SyncTest.output.add("after 1");
  }
}