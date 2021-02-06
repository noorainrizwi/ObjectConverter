package com.converter.processor;

import com.converter.Constants;
import com.converter.annotations.Model;
import com.converter.generator.ConverterGenerator;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.converter.annotations.Model")
@AutoService(Processor.class)
public class ModelProcessor extends AbstractProcessor {

    String message = "";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {

            // Get all class Element annotated @Model
            Set<? extends Element> annotatedClass = roundEnv.getElementsAnnotatedWith(annotation);

            // Separate classes based on the Model identifier
            Map<String, List<Element>> annotatedclassMap = annotatedClass.stream().collect(
                    Collectors.groupingBy(element -> element.getAnnotation(Model.class).converterName()));

            for (Map.Entry<String, List<Element>> classEntry : annotatedclassMap.entrySet()) {
                if (2 != classEntry.getValue().size()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            String.format(Constants.NOT_TWO_CLASS, classEntry.getKey()), classEntry.getValue().get(0));
                } else {
//                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, classEntry.getValue().get(0).getEnclosedElements().toString());
                    final String className = classEntry.getValue().get(0).getAnnotation(Model.class).converterName();
                    try {
                        ConverterGenerator generator = new ConverterGenerator(className);
                        generator.generateConverterMethod(classEntry.getValue().get(0), classEntry.getValue().get(1));

                        TypeSpec classType = generator.getTypeClass();
                        JavaFile javaFile = JavaFile.builder(Constants.CONVERTER_PACKAGE, classType).build();
                        javaFile.writeTo(processingEnv.getFiler());
                    } catch (IOException ioe) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                String.format(Constants.CLASS_WRITE_ERROR, className));
                    } catch (Exception e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    }
                }
            }
        }
        return false;
    }
}
