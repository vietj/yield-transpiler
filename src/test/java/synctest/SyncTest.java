package synctest;

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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SyncTest {

  @Test
  public void testName() throws Exception {
    URL res = SyncTest.class.getResource("/test1/A.java");
    assertNotNull(res);
    File root = new File(res.toURI()).getParentFile().getParentFile().getAbsoluteFile();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.forName("UTF-8"));
    fileManager.setLocation(StandardLocation.SOURCE_PATH, Collections.singletonList(root));
    List<String> classes = Collections.singletonList("test1.A");
    JavaCompiler.CompilationTask task = compiler.getTask(new PrintWriter(System.out), fileManager, diagnostics, Collections.<String>emptyList(), classes, Collections.<JavaFileObject>emptyList());
    task.setProcessors(Arrays.asList(new Processor()));
    assertTrue(task.call());
  }
}
