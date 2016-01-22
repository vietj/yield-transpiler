package test_yield_argument_in_variable;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    String value = Helper.yield();
    SyncTest.output.add(value);
  }
}