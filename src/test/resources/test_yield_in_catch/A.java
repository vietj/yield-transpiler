package test_yield_in_catch;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    try {
      SyncTest.output.add("try 1");
      SyncTest.fail();
      SyncTest.output.add("try 2");
    } catch (Exception e) {
      SyncTest.output.add("catch 1");
      Flow.yield();
      SyncTest.output.add("catch 2");
    }
    SyncTest.output.add("after");
  }
}