package test_throw_runtime_exception_in_if;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    SyncTest.output.add("before");
    if (true) {
      Helper.yield();
      throw new RuntimeException("the runtime exception");
    }
    SyncTest.output.add("after");
  }
}