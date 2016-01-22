package test_async_callback;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    String value = Helper.yield(SyncTest.wrap(c -> SyncTest.async(c)));
    SyncTest.output.add(value);
  }
}