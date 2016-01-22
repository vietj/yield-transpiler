package test_yield_in_for;

import synctest.Helper;
import synctest.Generator;
import synctest.SyncTest;

public class A {
  @Generator
  public void sync() {
    SyncTest.output.add("before");
    for (int i = 0;i < 3;i++) {
      SyncTest.output.add("<-" + i);
      Helper.yield();
      SyncTest.output.add("->" + i);
    }
    SyncTest.output.add("after");
  }
}