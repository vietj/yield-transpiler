package test1;

import synctest.Helper;
import synctest.Generator;
import synctest.SyncTest;

import java.util.function.Consumer;

public class A {
  @Generator
  public void sync() {
    SyncTest.value = "foo";
    Helper.yield();
    SyncTest.value = "bar";
  }
}