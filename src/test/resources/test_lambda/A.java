package test_lambda;

import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {

  String s = "the_value";

  @GeneratorFunction
  public void sync() {
    SyncTest.wrap(c -> SyncTest.output.add(this.s));
  }


}