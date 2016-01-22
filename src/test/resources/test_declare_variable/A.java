package test_declare_variable;

import synctest.Transpile;
import synctest.SyncTest;

public class A {
  @Transpile
  public void sync() {
    int i;
    i = 0;
    SyncTest.output.add("" + i);
  }
}