package test_field_read;

import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {

  String s = "the_value";

  @GeneratorFunction
  public String sync() {
    return this.s;
  }
}