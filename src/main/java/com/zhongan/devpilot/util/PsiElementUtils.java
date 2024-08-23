package com.zhongan.devpilot.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class PsiElementUtils {
    public static String getFullClassName(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            var psiClass = ((PsiMethod) element).getContainingClass();
            if (psiClass == null) {
                return null;
            }

            return psiClass.getQualifiedName();
        }

        return null;
    }

    public static String getRelatedClass(@NotNull PsiElement element) {
        Set<PsiClass> classSet = new HashSet<>();

        if (element instanceof PsiMethod) {
            classSet = getMethodRelatedClass(element);
        } else if (element instanceof PsiClass) {
            classSet = getClassRelatedClass(element);
        }

        return transformElementToString(classSet);
    }

    public static <T extends PsiElement> String transformElementToString(Collection<T> elements) {
        var result = new StringBuilder();

        for (T element : elements) {
            if (element instanceof PsiClass) {
                if (ignoreClass((PsiClass) element)) {
                    continue;
                }
            }

            if (element instanceof PsiMethod) {
                var method = (PsiMethod) element;
                var psiClass = method.getContainingClass();

                if (ignoreClass(psiClass)) {
                    continue;
                }
            }

            result.append(element.getText()).append("\n");
        }

        return result.toString();
    }

    private static Set<PsiClass> getClassRelatedClass(@NotNull PsiElement element) {
        Set<PsiClass> result = new HashSet<>();

        if (element instanceof PsiClass) {
            var psiClass = (PsiClass) element;
            var methods = psiClass.getMethods();
            var fields = psiClass.getFields();

            for (PsiMethod psiMethod : methods) {
                result.addAll(getMethodParameterTypeClass(psiMethod));
            }

            for (PsiField psiField : fields) {
                result.addAll(getFieldTypeClass(psiField));
            }
        }

        return result;
    }

    private static Set<PsiClass> getMethodRelatedClass(@NotNull PsiElement element) {
        var parameterClass = getMethodParameterTypeClass(element);
        var returnClass = getMethodReturnTypeClass(element);

        var result = new HashSet<>(parameterClass);
        result.addAll(returnClass);

        return result;
    }

    private static List<PsiClass> getMethodReturnTypeClass(@NotNull PsiElement element) {
        var result = new ArrayList<PsiClass>();

        if (element instanceof PsiMethod) {
            var returnType = ((PsiMethod) element).getReturnType();

            if (returnType instanceof PsiClassReferenceType) {
                var referenceType = (PsiClassReferenceType) returnType;
                result.addAll(getTypeClassAndGenericType(referenceType));
                return result;
            }
        }

        return result;
    }

    private static List<PsiClass> getMethodParameterTypeClass(@NotNull PsiElement element) {
        var result = new ArrayList<PsiClass>();

        if (element instanceof PsiMethod) {
            var params = ((PsiMethod) element).getParameterList().getParameters();

            for (JvmParameter parameter : params) {
                if (parameter.getType() instanceof PsiClassReferenceType) {
                    var referenceType = (PsiClassReferenceType) parameter.getType();
                    result.addAll(getTypeClassAndGenericType(referenceType));
                }
            }
        }

        return result;
    }

    private static List<PsiClass> getFieldTypeClass(@NotNull PsiElement element) {
        var result = new ArrayList<PsiClass>();

        if (element instanceof PsiField) {
            var field = ((PsiField) element);

            if (field.getType() instanceof PsiClassReferenceType) {
                var referenceType = (PsiClassReferenceType) field.getType();
                result.addAll(getTypeClassAndGenericType(referenceType));
            }
        }

        return result;
    }

    private static List<PsiClass> getGenericType(PsiClassReferenceType referenceType) {
        var result = new ArrayList<PsiClass>();

        var genericType = referenceType.resolveGenerics();
        var typeClass = genericType.getElement();

        if (typeClass == null) {
            return result;
        }

        var psiSubstitutor = genericType.getSubstitutor();

        for (PsiTypeParameter typeParameter : typeClass.getTypeParameters()) {
            var psiType = psiSubstitutor.substitute(typeParameter);

            if (psiType instanceof PsiClassReferenceType) {
                var psiClass = ((PsiClassReferenceType) psiType).resolve();
                if (psiClass != null) {
                    result.add(psiClass);
                }
            }
        }

        return result;
    }

    public static String getCompletionRelatedClass(PsiFile psiFile, int offset) {
        var element = psiFile.findElementAt(offset);
        var psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        if (psiMethod != null) {
            var methodList = findMethodCall(psiMethod);
            var methodRelatedType = findMethodRelatedType(psiMethod);

            return transformElementToString(methodList) + transformElementToString(methodRelatedType);
        }

        var psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        if (psiClass != null) {
            var fieldList = PsiTreeUtil.findChildrenOfType(psiClass, PsiField.class);

            var fieldClassSet = new HashSet<PsiClass>();
            for (PsiField field : fieldList) {
                fieldClassSet.addAll(getFieldTypeClass(field));
            }

            return transformElementToString(fieldClassSet);
        }

        return null;
    }

    private static Set<PsiMethod> findMethodCall(PsiMethod psiMethod) {
        var callMethodList = PsiTreeUtil.findChildrenOfType(psiMethod, PsiMethodCallExpression.class);

        var methodSet = new HashSet<PsiMethod>();
        for (PsiMethodCallExpression callMethod : callMethodList) {
            var method = callMethod.resolveMethod();
            methodSet.add(method);
        }

        return methodSet;
    }

    private static Set<PsiClass> findMethodRelatedType(PsiMethod psiMethod) {
        var typeList = PsiTreeUtil.findChildrenOfType(psiMethod, PsiTypeElement.class);

        var classSet = new HashSet<PsiClass>();
        for (PsiTypeElement typeElement : typeList) {
            var type = typeElement.getType();

            if (type instanceof PsiClassReferenceType) {
                var referenceType = (PsiClassReferenceType) type;
                classSet.addAll(getTypeClassAndGenericType(referenceType));
            }
        }

        return classSet;
    }

    private static List<PsiClass> getTypeClassAndGenericType(PsiClassReferenceType referenceType) {
        var result = new ArrayList<PsiClass>();

        var psiClass = referenceType.resolve();
        if (psiClass != null) {
            result.add(psiClass);
        }
        result.addAll(getGenericType(referenceType));

        return result;
    }

    private static boolean ignoreClass(PsiClass psiClass) {
        if (psiClass == null) {
            return true;
        }

        var fullClassName = psiClass.getQualifiedName();

        if (fullClassName == null) {
            return true;
        }

        // ignore jdk class
        if (fullClassName.startsWith("java")) {
            return true;
        }

        // ignore some log package
        if (fullClassName.startsWith("org.slf4j")
                || fullClassName.startsWith("org.jboss.logmanager")
                || fullClassName.startsWith("org.apache.log4j")
                || fullClassName.startsWith("ch.qos.logback")) {
            return true;
        }

        // todo should ignore some famous opensource dependency

        return false;
    }
}
