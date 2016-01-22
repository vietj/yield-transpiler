package test_suspend_resume;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    SyncTest.output.add("foo");
    Helper.yield();
    SyncTest.output.add("bar");
  }
}