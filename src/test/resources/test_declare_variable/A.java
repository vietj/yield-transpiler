package test_declare_variable;

import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    int i;
    i = 0;
    SyncTest.output.add("" + i);
  }
}