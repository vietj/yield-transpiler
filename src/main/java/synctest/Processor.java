package synctest;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Processor extends AbstractProcessor {

  private Trees trees;
  private Attr attr;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    Context context = ((JavacProcessingEnvironment)processingEnv).getContext();
    this.trees = Trees.instance(processingEnv);
    this.attr = Attr.instance(context);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Generator.class.getName());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (Element elt : roundEnv.getElementsAnnotatedWith(Generator.class)) {
      process((ExecutableElement) elt);
    }


    return false;
  }

  private void process(ExecutableElement methodElt) {
    TypeElement typeElt = (TypeElement) methodElt.getEnclosingElement();
    attributeClass(typeElt);
    TreePath path = trees.getPath(methodElt);
    TreeAnalyzer analyzer = new TreeAnalyzer();
    path.getCompilationUnit().getImports().forEach(import_ -> analyzer.visitImport(import_, null));
    String source = analyzer.visitMethod(path);
    try {
      JavaFileObject generator = processingEnv.getFiler().createSourceFile("GeneratorImpl", methodElt);
      try (Writer writer = generator.openWriter()) {
        writer.write(source);
      }
    } catch (IOException e) {
      e.printStackTrace();
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Coult not compile");
    }
  }

  private void attributeClass(Element classElement) {
    assert classElement.getKind() == ElementKind.CLASS;
    JCTree.JCClassDecl ct = (JCTree.JCClassDecl) trees.getTree(classElement);
    if (ct.sym != null) {
      if ((ct.sym.flags_field & Flags.UNATTRIBUTED) != 0) {
        attr.attribClass(ct.pos(), ct.sym);
      }
    }
  }
}
