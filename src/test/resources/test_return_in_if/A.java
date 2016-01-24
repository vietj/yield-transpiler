package test_return_in_if;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public String sync() {
    SyncTest.output.add("before");
    if (true) {
      String a = Flow.yield();
      return a;
    }
    SyncTest.output.add("after");
    return "the_other_string";
  }
}