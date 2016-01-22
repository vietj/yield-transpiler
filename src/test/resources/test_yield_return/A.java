package test_return;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    Helper.yield("the_return_value");
  }
}