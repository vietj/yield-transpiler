package test_try;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("before");
    try {
      SyncTest.fail();
    } catch (Exception e) {
      SyncTest.output.add("failed");
    }
    SyncTest.output.add("after");
  }
}