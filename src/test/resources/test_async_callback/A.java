package test_async_callback;

import synctest.Flow;
import synctest.GeneratorFunction;
import synctest.SyncTest;

public class A {
  @GeneratorFunction
  public void sync() {
    String value = Flow.yield(SyncTest.wrap(c -> SyncTest.async(c)));
    SyncTest.output.add(value);
  }
}