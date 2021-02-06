package com.converter;

import javax.lang.model.element.Element;

public class MethodUtil {
    public static String getSetterMethodName(final Element field) {
        return getMethodName(field, Constants.SETTER_PREFIX);
    }

    public static String getGetterMethodName(final Element field) {
        return getMethodName(field, Constants.GETTER_PREFIX);
    }

    public static String getMethodName(final Element field, final String prefix) {
        if (null == field || null == prefix) {
            throw new NullPointerException("field and prefix cannot be null");
        }
        final String fieldName = field.getSimpleName().toString();
        return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
}
