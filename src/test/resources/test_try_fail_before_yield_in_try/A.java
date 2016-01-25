package test_try_fail_before_yield_in_try;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before 1");
    try {
      SyncTest.output.add("before 2");
      try {
        Flow.yield();
        SyncTest.fail();
      } catch (Exception e) {
        SyncTest.output.add("failed 2");
      }
      SyncTest.output.add("after 2");
    } catch (Exception e) {
      SyncTest.output.add("failed 1");
    }
    SyncTest.output.add("after 1");
  }
}