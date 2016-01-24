package test_suspend_resume;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    SyncTest.output.add("foo");
    Flow.yield();
    SyncTest.output.add("bar");
  }
}