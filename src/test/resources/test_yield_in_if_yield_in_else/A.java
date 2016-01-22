package test_yield_in_if_yield_in_else;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    SyncTest.output.add("before");
    if ("one".equals(SyncTest.value)) {
      SyncTest.output.add("foo");
      Helper.yield();
      SyncTest.output.add("bar");
    } else {
      SyncTest.output.add("juu");
      Helper.yield();
      SyncTest.output.add("daa");
    }
    SyncTest.output.add("after");
  }
}