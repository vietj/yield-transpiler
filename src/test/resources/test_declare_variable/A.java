package test_declare_variable;

import synctest.Generator;
import synctest.SyncTest;

public class A {
  @Generator
  public void sync() {
    int i;
    i = 0;
    SyncTest.output.add("" + i);
  }
}