package test_method_params;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public String sync(String s, int i) {
    return s + i;
  }
}