package synctest;

import org.junit.Before;
import org.junit.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SyncTest {

  public static String value;
  public static List<String> output;

  public static void fail() {
    throw new RuntimeException();
  }

  @Before
  public void before() {
    value = null;
    output = new ArrayList<>();
  }


  private Supplier<Generator> compile(String fqn) throws Exception {

    File classes = new File(new File("target"), fqn);
    assertTrue(classes.exists() ? classes.isDirectory() : classes.mkdirs());

    URL res = SyncTest.class.getResource("/" + fqn.replace('.', '/') + ".java");
    assertNotNull(res);
    File root = new File(res.toURI()).getParentFile().getParentFile().getAbsoluteFile();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.forName("UTF-8"));
    fileManager.setLocation(StandardLocation.SOURCE_PATH, Collections.singletonList(root));
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(classes));
    fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singletonList(classes));
    Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjects(new File(res.toURI()));
    JavaCompiler.CompilationTask task = compiler.getTask(new PrintWriter(System.out), fileManager, diagnostics, Collections.<String>emptyList(), Collections.emptyList(), sources);
    task.setProcessors(Collections.singletonList(new Processor()));
    if (!task.call()) {
      diagnostics.getDiagnostics().forEach(diagnostic -> {
        System.out.println(diagnostic.getMessage(Locale.ENGLISH));
      });
      fail();
    }
    URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
    Class<?> genClass = loader.loadClass("GeneratorImpl");
    Object instance = genClass.newInstance();
    return () -> {
      try {
        return (Generator) genClass.getMethod("create").invoke(instance);
      } catch (Exception e) {
        throw new UndeclaredThrowableException(e);
      }
    };
  }

  @Test
  public void testSuspendResume() throws Exception {
    Supplier<Generator> test = compile("test_suspend_resume.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("foo"), output);
    it.next();
    assertEquals(Arrays.asList("foo", "bar"), output);
    try {
      it.next();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testYieldInIf() throws Exception {
    Supplier<Generator> test = compile("test_yield_in_if.A");
    Generator it = test.get();
    value = "one";
    it.next();
    assertEquals(Arrays.asList("before", "foo"), output);
    it.next();
    assertEquals(Arrays.asList("before", "foo", "bar", "after"), output);
    output.clear();
    it = test.get();
    value = null;
    it.next();
    assertEquals(Arrays.asList("before", "after"), output);
  }

  @Test
  public void testYieldInIfElse() throws Exception {
    Supplier<Generator> test = compile("test_yield_in_if_else.A");
    Generator it = test.get();
    value = "one";
    it.next();
    assertEquals(Arrays.asList("before", "foo"), output);
    it.next();
    assertEquals(Arrays.asList("before", "foo", "bar", "after"), output);
    it = test.get();
    output.clear();
    value = null;
    it.next();
    assertEquals(Arrays.asList("before", "juu", "after"), output);
  }

  @Test
  public void testYieldInIfYieldInElse() throws Exception {
    Supplier<Generator> test = compile("test_yield_in_if_yield_in_else.A");
    Generator it = test.get();
    value = "one";
    it.next();
    assertEquals(Arrays.asList("before", "foo"), output);
    it.next();
    assertEquals(Arrays.asList("before", "foo", "bar", "after"), output);
    it = test.get();
    output.clear();
    value = null;
    it.next();
    assertEquals(Arrays.asList("before", "juu"), output);
    it.next();
    assertEquals(Arrays.asList("before", "juu", "daa", "after"), output);
  }

  @Test
  public void testYieldInFor() throws Exception {
    Supplier<Generator> test = compile("test_yield_in_for.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before", "<-0"), output);
    it.next();
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1"), output);
    it.next();
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2"), output);
    it.next();
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2", "->2", "after"), output);
  }

  @Test
  public void testFor() throws Exception {
    Supplier<Generator> test = compile("test_for.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2", "->2", "after"), output);
  }

  @Test
  public void testYieldInIfInFor() throws Exception {
    Supplier<Generator> test = compile("test_yield_if_in_for.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1"), output);
    it.next();
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2", "->2", "after"), output);
  }

  @Test
  public void testDeclareVariable() throws Exception {
    Supplier<Generator> test = compile("test_declare_variable.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("0"), output);
  }

  @Test
  public void testYieldReturn() throws Exception {
    Supplier<Generator> test = compile("test_yield_return.A");
    Generator it = test.get();
    assertEquals("the_return_value", it.next());
  }

  @Test
  public void testYieldArgumentInVariable() throws Exception {
    Supplier<Generator> test = compile("test_yield_argument_in_variable.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList(), output);
    it.next("the_argument_value");
    assertEquals(Arrays.asList("the_argument_value"), output);
  }

  @Test
  public void testYieldArgumentInAssign() throws Exception {
    Supplier<Generator> test = compile("test_yield_argument_in_assign.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList(), output);
    it.next("the_argument_value");
    assertEquals(Arrays.asList("the_argument_value"), output);
  }

  @Test
  public void testThrowRuntimeException() throws Exception {
    Supplier<Generator> test = compile("test_throw_runtime_exception.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
    } catch (RuntimeException e) {
      assertEquals("the runtime exception", e.getMessage());
    }
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testThrowRuntimeExceptionInIf() throws Exception {
    Supplier<Generator> test = compile("test_throw_runtime_exception_in_if.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
    } catch (RuntimeException e) {
      assertEquals("the runtime exception", e.getMessage());
    }
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testReturn() throws Exception {
    Supplier<Generator> test = compile("test_return.A");
    Generator it = test.get();
    assertEquals(null, it.next());
    assertEquals(Arrays.asList("before"), output);
    assertEquals("the_returned_string", it.next());
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testReturnInIf() throws Exception {
    Supplier<Generator> test = compile("test_return_in_if.A");
    Generator it = test.get();
    assertEquals(null, it.next());
    assertEquals(Arrays.asList("before"), output);
    assertEquals("the_yielded_string", it.next("the_yielded_string"));
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testReturnInFor() throws Exception {
    Supplier<Generator> test = compile("test_return_in_for.A");
    Generator it = test.get();
    assertEquals(null, it.next());
    assertEquals(Arrays.asList("before"), output);
    assertEquals("the_yielded_string", it.next("the_yielded_string"));
    assertEquals(Arrays.asList("before"), output);
    try {
      it.next();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testFail() throws Exception {
    Supplier<Generator> test = compile("test_fail.A");
    Generator it = test.get();
    assertEquals(null, it.next());
    assertEquals(Arrays.asList("before"), output);
    RuntimeException cause = new RuntimeException();
    try {
      assertEquals(null, it.fail(cause));
      fail();
    } catch (RuntimeException e) {
      assertSame(cause, e);
    }
    assertEquals(Arrays.asList("before"), output);
  }

  @Test
  public void testTry() throws Exception {
    Supplier<Generator> test = compile("test_try.A");
  }

  @Test
  public void testTryFailBeforeYield() throws Exception {
    Supplier<Generator> test = compile("test_try_fail_before_yield.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before", "failed", "after"), output);
  }

  @Test
  public void testTryFailAfterYield() throws Exception {
    Supplier<Generator> test = compile("test_try_fail_after_yield.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before"), output);
    it.next();
    assertEquals(Arrays.asList("before", "failed", "after"), output);
  }

  @Test
  public void testTryFailBeforeYieldInTry() throws Exception {
    Supplier<Generator> test = compile("test_try_fail_before_yield_in_try.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before 1", "before 2"), output);
    it.next();
    assertEquals(Arrays.asList("before 1", "before 2", "failed 2", "after 2", "after 1"), output);
  }

  @Test
  public void testYieldInTryYieldInCatch() throws Exception {
    Supplier<Generator> test = compile("test_yield_in_try_yield_in_catch.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before 1", "failed 1"), output);
    it.next();
    assertEquals(Arrays.asList("before 1", "failed 1", "failed 2", "after 1"), output);
  }

  @Test
  public void testYieldInCatch() throws Exception {
    Supplier<Generator> test = compile("test_yield_in_catch.A");
    Generator it = test.get();
    it.next();
    assertEquals(Arrays.asList("before", "try 1", "catch 1"), output);
    it.next();
    assertEquals(Arrays.asList("before", "try 1", "catch 1", "catch 2", "after"), output);
  }

  @Test
  public void testAsyncCallback() throws Exception {
    Supplier<Generator> test = compile("test_async_callback.A");
    Generator it = test.get();
    Consumer<Generator> consumer = (Consumer<Generator>) it.next();
    consumer.accept(it);
    assertNotNull(theCallback);
    assertEquals(Arrays.asList(), output);
    theCallback.accept("the_callback_value");
    assertEquals(Arrays.asList("the_callback_value"), output);
  }

  public static Consumer<Generator> wrap(Consumer<Consumer<String>> tutu) {
    return generator -> {
      tutu.accept(event -> {
        generator.next(event);
      });
    };
  }

  private static Consumer<String> theCallback;

  public static void async(Consumer<String> callback) {
    theCallback = callback;
  }
}
