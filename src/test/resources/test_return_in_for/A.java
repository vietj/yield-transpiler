package test_return_in_for;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public String sync() {
    SyncTest.output.add("before");
    for (int i = 0;i < 10;i++) {
      String a = Flow.yield();
      return a;
    }
    SyncTest.output.add("after");
    return "the_other_string";
  }
}