package test_return_in_if;

import synctest.Helper;
import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public String sync() {
    SyncTest.output.add("before");
    if (true) {
      String a = Helper.yield();
      return a;
    }
    SyncTest.output.add("after");
    return "the_other_string";
  }
}