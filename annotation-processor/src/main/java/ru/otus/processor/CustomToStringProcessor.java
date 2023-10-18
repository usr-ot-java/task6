package ru.otus.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedAnnotationTypes("ru.otus.annotation.CustomToString")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class CustomToStringProcessor extends AbstractProcessor {

    private static final Set<ElementKind> FIELD_KINDS = Set.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT);
    private static final Set<ElementKind> METHOD_KINDS = Set.of(ElementKind.METHOD);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element annotatedElement : annotatedElements) {
                boolean toStringOverridden = Stream.of(annotatedElement)
                        .flatMap(e -> e.getEnclosedElements().stream())
                        .filter(e -> METHOD_KINDS.contains(e.getKind()))
                        .map(Element::getSimpleName)
                        .anyMatch(e -> e.contentEquals("toString"));

                if (!toStringOverridden) {
                    Set<? extends Element> fields =
                            Stream.of(annotatedElement)
                                    .flatMap(e -> e.getEnclosedElements().stream())
                                    .filter(e -> FIELD_KINDS.contains(e.getKind()))
                                    .collect(Collectors.toSet());
                    List<VariableElement> variableElements = extractNonStaticAndNonPrivateFields(fields);

                    if (!variableElements.isEmpty()) {
                        createClassFileWithToString(variableElements);
                    }
                }
                else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            String.format("The method toString() is already overridden in the %s. Ignoring it.", annotatedElement)
                    );
                }
            }
        }

        return true;
    }

    private List<VariableElement> extractNonStaticAndNonPrivateFields(Set<? extends Element> fields) {
        List<VariableElement> result = new ArrayList<>();
        for (Element field : fields) {
            Set<Modifier> modifiers = field.getModifiers();
            if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                // Skip static and private fields field
                continue;
            }
            result.add((VariableElement) field);
        }
        return result;
    }

    private void createClassFileWithToString(List<VariableElement> variableElements) {
        String className = ((TypeElement) variableElements.get(0).getEnclosingElement()).getQualifiedName().toString();
        String toStringClassName = className + "WithToString";
        try {
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(toStringClassName);
            writeBodyToStringClassFile(className, toStringClassName, builderFile, variableElements);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    String.format("Failed to create the file %s: %s", toStringClassName, e));
        }
    }

    private void writeBodyToStringClassFile(String className,
                                            String toStringClassName,
                                            JavaFileObject builderFile,
                                            List<VariableElement> variableElements) {
        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String simpleToStringClassName = toStringClassName.substring(toStringClassName.lastIndexOf('.') + 1);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            out.print("public class ");
            out.print(simpleToStringClassName);
            out.print(" extends ");
            out.print(className);
            out.println(" {");

            out.println("\tpublic String toString() {");
            out.print("\t\treturn \"");

            out.print(simpleToStringClassName);
            out.print("{\"");
            for (int i = 0; i < variableElements.size(); i++) {
                VariableElement e = variableElements.get(i);
                out.print(" + \"");
                out.print(e.getSimpleName());
                out.print("=\" + this.");
                out.print(e.getSimpleName());
                if (i + 1 != variableElements.size()) {
                    out.print(" + \",\"");
                }
            }
            out.println(" + \"}\";");

            out.println("\t}");
            out.println("}");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    String.format("Failed to write to the file %s: %s", builderFile.getName(), e));
        }
    }

}
