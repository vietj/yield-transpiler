package test_return;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public String sync() {
    SyncTest.output.add("before");
    Helper.yield();
    return "the_returned_string";
  }
}