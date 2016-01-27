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
import java.util.HashMap;
import java.util.Map;
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
    return Collections.singleton(GeneratorFunction.class.getName());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Map<TypeElement, Transpiler> transpilers = new HashMap<>();
    for (Element elt : roundEnv.getElementsAnnotatedWith(GeneratorFunction.class)) {
      ExecutableElement methodElt = (ExecutableElement) elt;
      TypeElement typeElt = (TypeElement) methodElt.getEnclosingElement();
      Transpiler transpiler = transpilers.computeIfAbsent(typeElt, t -> {
        attributeClass(typeElt);
        Transpiler tr = new Transpiler();
        TreePath path = trees.getPath(typeElt);
        tr.scan(path.getCompilationUnit(), null);
        return tr;
      });
      TreePath path = trees.getPath(methodElt);
      transpiler.scan(path, null);
    }
    transpilers.forEach((typeElt, transpiler) -> {
      try {
        JavaFileObject generator = processingEnv.getFiler().createSourceFile(typeElt.getQualifiedName() + "_", typeElt);
        try (Writer writer = generator.openWriter()) {
          writer.write(transpiler.getSource());
        }
      } catch (IOException e) {
        e.printStackTrace();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Coult not compile");
      }
    });
    return false;
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
