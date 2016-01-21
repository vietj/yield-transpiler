package test2;

import synctest.Helper;
import synctest.Generator;
import synctest.SyncTest;

import java.util.function.Consumer;

public class A {
  @Generator
  public void sync() {
    if ("one".equals(SyncTest.value)) {
      SyncTest.value = "foo";
      Helper.yield();
      SyncTest.value = "bar";
    }
    SyncTest.value2 = "done";
  }
}