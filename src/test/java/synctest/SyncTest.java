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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SyncTest {

  public static String value;
  public static List<String> output;

  @Before
  public void before() {
    value = null;
    output = new ArrayList<>();
  }

  private synctest.Iterator compile(String fqn) throws Exception {

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
    assertTrue(task.call());
    URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
    Class<?> genClass = loader.loadClass("GeneratorImpl");
    Object instance = genClass.newInstance();
    return (Iterator) genClass.getMethod("create").invoke(instance);
  }

  @Test
  public void testSuspendResume() throws Exception {
    synctest.Iterator it = compile("test_suspend_resume.A");
    Context context = new Context();
    it.next(context);
    assertEquals(Arrays.asList("foo"), output);
    it.next(context);
    assertEquals(Arrays.asList("foo", "bar"), output);
  }

  @Test
  public void testYieldInIf() throws Exception {
    synctest.Iterator it = compile("test_yield_in_if.A");
    Context context = new Context();
    value = "one";
    it.next(context);
    assertEquals(Arrays.asList("before", "foo"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "foo", "bar", "after"), output);
    output.clear();
    context = new Context();
    value = null;
    it.next(context);
    assertEquals(Arrays.asList("before", "after"), output);
  }

  @Test
  public void testYieldInIfElse() throws Exception {
    synctest.Iterator it = compile("test_yield_in_if_else.A");
    Context context = new Context();
    value = "one";
    it.next(context);
    assertEquals(Arrays.asList("before", "foo"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "foo", "bar", "after"), output);
    context = new Context();
    output.clear();
    value = null;
    it.next(context);
    assertEquals(Arrays.asList("before", "juu", "after"), output);
  }

  @Test
  public void testYieldInIfYieldInElse() throws Exception {
    synctest.Iterator it = compile("test_yield_in_if_yield_in_else.A");
    Context context = new Context();
    value = "one";
    it.next(context);
    assertEquals(Arrays.asList("before", "foo"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "foo", "bar", "after"), output);
    context = new Context();
    output.clear();
    value = null;
    it.next(context);
    assertEquals(Arrays.asList("before", "juu"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "juu", "daa", "after"), output);
  }

  @Test
  public void testYieldInFor() throws Exception {
    synctest.Iterator it = compile("test_yield_in_for.A");
    Context context = new Context();
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2", "->2", "after"), output);
  }

  @Test
  public void testFor() throws Exception {
    synctest.Iterator it = compile("test_for.A");
    Context context = new Context();
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2", "->2", "after"), output);
  }

  @Test
  public void testYieldInIfInFor() throws Exception {
    synctest.Iterator it = compile("test_yield_if_in_for.A");
    Context context = new Context();
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1"), output);
    it.next(context);
    assertEquals(Arrays.asList("before", "<-0", "->0", "<-1", "->1", "<-2", "->2", "after"), output);
  }
}
