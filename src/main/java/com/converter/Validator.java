package com.converter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;

public class Validator {

    public static boolean isValidClassName(final String className) {
        return SourceVersion.isName(className);
    }

    public static boolean hasDefaultConstructor(Element classElement) {
        return classElement.getEnclosedElements().stream().anyMatch(ele -> {
            return ele.getKind() == ElementKind.CONSTRUCTOR
                    && ((ExecutableType) ele.asType()).getParameterTypes().size() == 1
                    && ele.getModifiers().contains(Modifier.PUBLIC);
        });
    }

    public static boolean isDataTyeSame(final Element field1, final Element field2) {
        if (field1.asType().getKind().isPrimitive() && field2.asType().getKind().isPrimitive()) {
            PrimitiveType field1Type = ((PrimitiveType) field1.asType());
            PrimitiveType field2Type = ((PrimitiveType) field2.asType());
            return field1Type.equals(field2Type);
        }
        if (!field1.asType().getKind().isPrimitive() && !field2.asType().getKind().isPrimitive()) {
            ReferenceType field1Type = ((ReferenceType) field1.asType());
            ReferenceType field2Type = ((ReferenceType) field2.asType());
            return field1Type.equals(field2Type);
        }
        return false;
    }

    public static boolean hasGetter(final Element field) {
        return hasMethod(field, MethodUtil.getGetterMethodName(field), 1);
    }

    public static boolean hasSetter(final Element field) {
        return hasMethod(field, MethodUtil.getSetterMethodName(field), 1);
    }

    public static boolean hasMethod(final Element field, final String methodName, final int paramCount) {
        return field.getEnclosingElement().getEnclosedElements().stream().anyMatch(ele -> {
            return ele.getKind() == ElementKind.METHOD
                    && ele.getSimpleName().contentEquals(methodName)
                    && ((ExecutableType) ele.asType()).getParameterTypes().size() == 1
                    && ele.getModifiers().contains(Modifier.PUBLIC);
        });
    }
}
