package test_suspend_resume;

import synctest.Helper;
import synctest.Generator;
import synctest.SyncTest;

public class A {
  @Generator
  public void sync() {
    SyncTest.output.add("foo");
    Helper.yield();
    SyncTest.output.add("bar");
  }
}