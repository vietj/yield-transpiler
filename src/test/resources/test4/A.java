package test4;

import synctest.Helper;
import synctest.Generator;
import synctest.SyncTest;

import java.util.function.Consumer;

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
      Helper.yield();
      SyncTest.output.add("daa");
    }
    SyncTest.output.add("after");
  }
}