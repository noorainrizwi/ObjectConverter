package com.converter.generator;

import com.converter.Constants;
import com.converter.MethodUtil;
import com.converter.ValidationException;
import com.converter.Validator;
import com.converter.annotations.Attribute;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConverterGenerator {

    private final String className;

    private TypeSpec.Builder classBuilder;

    private static final String PARAMETER_NAME = "object";
    private static final String VARIABLE_NAME = "variable";

    public ConverterGenerator(final String className) {
        if (!Validator.isValidClassName(className)) {
            throw new ValidationException("Invalid className");
        }
        this.className = className;
    }

    public void generateConverterMethod(final Element class1, final Element class2) {
        generateConverterMethod(class1, class2, true);
    }

    public void generateConverterMethod(final Element class1, final Element class2, boolean bidirectional) {

//        if (!Validator.hasDefaultConstructor(class1) || !Validator.hasDefaultConstructor(class2))
//            throw new ValidationException(String.format("Default constructor is required for both class %s and %s",
//                    class1.getSimpleName(), class2.getSimpleName()));

        if (null == classBuilder)
            generateClass();

        // Get the list of method annotated with @Attribute in class
        Map<String, Element> idFieldMappingOfClass1 = getAnnotatedFileds(class1)
                .stream().collect(Collectors.toMap(element -> element.getAnnotation(Attribute.class).id(), element -> element));
        Map<String, Element> idFieldMappingOfClass2 = getAnnotatedFileds(class2)
                .stream().collect(Collectors.toMap(element -> element.getAnnotation(Attribute.class).id(), element -> element));

        // List of Attributes required to convert from class1 to class2
        List<String> class1toClass2FieldList = validateAndGetConverterFields(idFieldMappingOfClass1, idFieldMappingOfClass2);
        // List of Attributes required to convert from class2 to class1
        List<String> class2toClass1FieldList = validateAndGetConverterFields(idFieldMappingOfClass2, idFieldMappingOfClass1);

        if (class1toClass2FieldList.isEmpty() && class2toClass1FieldList.isEmpty()) {
            throw new ValidationException(String.format("There is no common methods between class %s and %s",
                    class1.getSimpleName(), class2.getSimpleName()));
        }

        MethodSpec.Builder methodBuilder = getMethodBuilder(class1, class2);
        MethodSpec converter12 = getMethod(methodBuilder, class1toClass2FieldList, idFieldMappingOfClass1, idFieldMappingOfClass2);
        classBuilder.addMethod(converter12);

        if (bidirectional) {
            methodBuilder = getMethodBuilder(class2, class1);
            MethodSpec converter21 = getMethod(methodBuilder, class1toClass2FieldList, idFieldMappingOfClass2, idFieldMappingOfClass1);
            classBuilder.addMethod(converter21);
        }
    }

    private MethodSpec.Builder getMethodBuilder(Element fromClass, Element toClass) {
        final String methodName = Constants.CONVERTER_PREFIX + fromClass.getSimpleName().toString() +
                Constants.CONVERTER_JOINER + toClass.getSimpleName().toString();
        final ParameterSpec parameter = ParameterSpec.builder(TypeName.get(fromClass.asType()), PARAMETER_NAME, Modifier.FINAL).build();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addParameter(parameter)
                .returns(TypeName.get(toClass.asType()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("$L $L = new $L()", toClass.asType(), VARIABLE_NAME, toClass.asType());
        return methodBuilder;
    }

    private MethodSpec getMethod(final MethodSpec.Builder methodBuilder,
                                 final List<String> class1toClass2FieldList,
                                 final Map<String, Element> idFieldMappingOfClass1,
                                 final Map<String, Element> idFieldMappingOfClass2) {

        for (String id : class1toClass2FieldList) {
            String setterMethod = MethodUtil.getSetterMethodName(idFieldMappingOfClass2.get(id));
            String getterMethod = MethodUtil.getGetterMethodName(idFieldMappingOfClass1.get(id));
            methodBuilder.addStatement("$L.$L($L.$L())", VARIABLE_NAME, setterMethod, PARAMETER_NAME, getterMethod);
        }
        methodBuilder.addStatement("return $L", VARIABLE_NAME);
        return methodBuilder.build();
    }

    private List<String> validateAndGetConverterFields(Map<String, Element> fromFields, Map<String, Element> toFields) {
        List<String> converterFields = new ArrayList<>();
        for (Map.Entry<String, Element> tofield : toFields.entrySet()) {

            Element fromField = fromFields.get(tofield.getKey());
            boolean required = validateFieldComaptiblity(fromField, tofield.getValue());

            if (required) {
                converterFields.add(tofield.getKey());
            }
        }
        return converterFields;
    }

    // 1. Validate for Data type;
    // 2. Validate for Required flag;
    // 3. Validate for final modifier; --Not now
    // 4. Validate getter for from and setter for to filed;
    private boolean validateFieldComaptiblity(Element fromField, Element toField) {
        //Verifying for the mandatory flag
        if (fromField == null) {
            if (toField.getAnnotation(Attribute.class).required()) {
                throw new ValidationException(String.format("Mandatory field:%s is not present in source class", toField.getSimpleName()));
            } else {
                return false;
            }
        }

        // Verifying for the Data type
        if (!Validator.isDataTyeSame(fromField, toField)) {
            throw new ValidationException(String.format("Incompitable type for %s and  %s", toField.getSimpleName(), fromField.getSimpleName()));
        }


        //Verifying proper method is available
        boolean hasGetter = fromField.getEnclosingElement().getEnclosedElements().stream().anyMatch(ele -> {
            return ele.getKind() == ElementKind.METHOD && ele.getSimpleName().contentEquals(MethodUtil.getGetterMethodName(fromField));
        });

        if (!hasGetter) {
            throw new ValidationException(String.format("Getter is not defined for the field: %s %s", fromField.getSimpleName(), MethodUtil.getGetterMethodName(fromField)));
        }

        boolean hasSetter = toField.getEnclosingElement().getEnclosedElements().stream().anyMatch(ele -> {
            return ele.getKind() == ElementKind.METHOD && ele.getSimpleName().contentEquals(MethodUtil.getSetterMethodName(toField));
        });
        if (!hasSetter) {
            throw new ValidationException(String.format("Setter is not defined for the field: %s", toField.getSimpleName()));
        }
        return true;
    }

    // Returns all the fields annotated with @Attribute annotation within the given class Element
    private List<Element> getAnnotatedFileds(Element classElement) {
        Map<Boolean, List<Element>> enclosingElements = classElement.getEnclosedElements().stream().collect(
                Collectors.partitioningBy(element -> null != element.getAnnotation(Attribute.class)));
        return enclosingElements.get(true);
    }

    private void generateClass() {
        classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);
    }

    public TypeSpec getTypeClass() {
        return classBuilder == null ? null : classBuilder.build();
    }
}