package test_return;

import synctest.Flow;
import synctest.GeneratorFunction;

public class A {
  @GeneratorFunction
  public void sync() {
    Flow.yield("the_return_value");
  }
}