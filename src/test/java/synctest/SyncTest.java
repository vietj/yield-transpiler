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

  @Before
  public void before() {
    value = null;
  }

  private synctest.Iterator compile(String fqn) throws Exception {
    URL res = SyncTest.class.getResource("/" + fqn.replace('.', '/') + ".java");
    assertNotNull(res);
    File root = new File(res.toURI()).getParentFile().getParentFile().getAbsoluteFile();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.forName("UTF-8"));
    fileManager.setLocation(StandardLocation.SOURCE_PATH, Collections.singletonList(root));
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(new File("target/classes")));
    fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singletonList(new File("target/classes")));
    Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjects(new File(res.toURI()));
    JavaCompiler.CompilationTask task = compiler.getTask(new PrintWriter(System.out), fileManager, diagnostics, Collections.<String>emptyList(), Collections.emptyList(), sources);
    task.setProcessors(Collections.singletonList(new Processor()));
    assertTrue(task.call());
    URLClassLoader loader = new URLClassLoader(new URL[]{new File(".").getAbsoluteFile().toURI().toURL()}, Thread.currentThread().getContextClassLoader());
    Class<?> genClass = loader.loadClass("GeneratorImpl");
    Object instance = genClass.newInstance();
    return (Iterator) genClass.getMethod("create").invoke(instance);
  }

  @Test
  public void testSuspendResume() throws Exception {
    synctest.Iterator it = compile("test1.A");
    Context context = new Context();
    it.next(context);
    assertEquals("foo", value);
    it.next(context);
    assertEquals("bar", value);
  }
}
