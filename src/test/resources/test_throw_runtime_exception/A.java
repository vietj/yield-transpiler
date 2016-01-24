package test_throw_runtime_exception;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    SyncTest.output.add("before");
    Helper.yield();
    throw new RuntimeException("the runtime exception");
  }
}