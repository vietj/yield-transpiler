## Yield transpiler

A proof of concept of a `yield` transpiler based on annotation processor integrated in the Java compiler providing
a transparent compile-time transpilation.

Benefits:

- 100% compile time
- no external dependencies
- lighweight
- transpiled source is available and can be debugged

### Example

```
@Transpile
public void myMethod() {
  SyncTest.output.add("before");
  for (int i = 0;i < 3;i++) {
    SyncTest.output.add("<-" + i);
    if (i == 1) {
      Helper.yield();
    }
    SyncTest.output.add("->" + i);
  }
  SyncTest.output.add("after");
}
```

transpiles to a new class :

```
public class MyClass {
  int i;
  public synctest.Generator myMethod() {
    class GeneratorImpl extends synctest.Generator {
      public Object next(synctest.Context context) {
        while(true) {
          switch(context.status) {
            case 0: {
              SyncTest.output.add("before");
              i = 0;
              context.status = 1;
              break;
            }
            case 1: {
              if (!(i < 3)) {
                context.status = 4;
                break;
              }
              SyncTest.output.add("<-" + i);
              if (!(i == 1)) {
                context.status = 3;
                break;
              };
              context.status = 2;
              return null;
            }
            case 2: {
              context.status = 3;
              break;
            }
            case 3: {
              SyncTest.output.add("->" + i);
              i++;;
              context.status = 1;
              break;
            }
            case 4: {
              SyncTest.output.add("after");
              return null;
            }
          }
        }
      }
    }
    return new GeneratorImpl();
  }
}
```

### Todo

- function parameters
- while