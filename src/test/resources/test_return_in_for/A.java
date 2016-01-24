package test_return_in_for;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public String sync() {
    SyncTest.output.add("before");
    for (int i = 0;i < 10;i++) {
      String a = Helper.yield();
      return a;
    }
    SyncTest.output.add("after");
    return "the_other_string";
  }
}