package test_yield_argument_in_variable;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    String value = Flow.yield();
    SyncTest.output.add(value);
  }
}