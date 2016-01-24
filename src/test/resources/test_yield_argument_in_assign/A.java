package test_yield_argument_in_assign;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    String value;
    value = Flow.yield();
    SyncTest.output.add(value);
  }
}