package test_return;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public String sync() {
    SyncTest.output.add("before");
    Flow.yield();
    return "the_returned_string";
  }
}