package test_yield_in_if_else;

import synctest.Helper;
import synctest.Generator;
import synctest.SyncTest;

public class A {
  @Generator
  public void sync() {
    SyncTest.output.add("before");
    if ("one".equals(SyncTest.value)) {
      SyncTest.output.add("foo");
      Helper.yield();
      SyncTest.output.add("bar");
    } else {
      SyncTest.output.add("juu");
    }
    SyncTest.output.add("after");
  }
}