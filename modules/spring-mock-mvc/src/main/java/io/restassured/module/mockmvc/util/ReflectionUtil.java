package io.restassured.module.mockmvc.util;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtil {
    private ReflectionUtil() {
        //no-op
    }

    public static <T> T invokeMethod(Object instance, String methodName, Object... arguments) {
        Class<?>[] argumentTypes = getArgumentTypes(arguments);
        return invokeMethod(instance, methodName, argumentTypes, arguments);
    }


    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object instance, String methodName, Class<?>[] argumentTypes, Object... arguments) {
        final Class<?> targetClass = (instance instanceof Class) ? (Class<?>) instance : instance.getClass();

        // Find method (try exact; if not found and caller likely meant varargs, retry with array)
        Method method = ReflectionUtils.findMethod(targetClass, methodName, argumentTypes);
        if (method == null && argumentTypes != null && argumentTypes.length == 1 && !argumentTypes[0].isArray()) {
            Class<?> arrayParam = Array.newInstance(argumentTypes[0], 0).getClass();
            method = ReflectionUtils.findMethod(targetClass, methodName, arrayParam);
        }
        if (method == null) {
            throw new IllegalArgumentException(
                    "Cannot find method '" + methodName + "' in " + targetClass.getName()
                            + " (arguments=" + Arrays.toString(arguments) + ")"
            );
        }

        //  Resolve bridge -> real method
        Method resolved = BridgeMethodResolver.findBridgedMethod(method);
        if (resolved.isBridge() || resolved.isSynthetic()) {
            final String name = methodName;                      // capture for lambda
            final Class<?>[] paramTypes = resolved.getParameterTypes(); // capture for lambda

            Method nonBridge = null;
            for (Method m : targetClass.getMethods()) {
                if (m.getName().equals(name)
                        && Arrays.equals(m.getParameterTypes(), paramTypes)
                        && !m.isBridge()
                        && !m.isSynthetic()) {
                    nonBridge = m;
                    break;
                }
            }
            method = (nonBridge != null) ? nonBridge : resolved;
        } else {
            method = resolved;
        }

        // Uniform handling for methods whose last parameter is an array
        final Class<?>[] params = method.getParameterTypes();
        final boolean lastIsArray = params.length > 0 && params[params.length - 1].isArray();

        if (lastIsArray) {
            final int fixed = params.length - 1;
            final Class<?> arrayType = params[params.length - 1];
            final Class<?> componentType = arrayType.getComponentType();

            if (arguments.length == params.length) {
                final Object last = arguments[arguments.length - 1];

                if (last == null) {
                    Object empty = Array.newInstance(componentType, 0);
                    Object[] invocationArgs = Arrays.copyOf(arguments, arguments.length);
                    invocationArgs[invocationArgs.length - 1] = empty;
                    return (T) invoke(method, instance, invocationArgs);
                }
                if (arrayType.isInstance(last)) {
                    return (T) invoke(method, instance, arguments);
                }
                if (last.getClass().isArray()) {
                    throw new IllegalArgumentException(
                            "Array parameter type mismatch: expected " + arrayType.getName()
                                    + " but got " + last.getClass().getName()
                    );
                }
            } else if (arguments.length < fixed) {
                throw new IllegalArgumentException(
                        "Too few arguments: expected at least " + fixed + " for " + method);
            }

            Object[] invocationArgs = new Object[params.length];
            if (fixed > 0) {
                System.arraycopy(arguments, 0, invocationArgs, 0, fixed);
            }

            int varCount = Math.max(0, arguments.length - fixed);
            Object varArray = Array.newInstance(componentType, varCount);
            for (int i = 0; i < varCount; i++) {
                Object v = arguments[fixed + i];
                if (v != null && !componentType.isInstance(v)) {
                    throw new IllegalArgumentException(
                            "Vararg element not assignable: expected " + componentType.getName()
                                    + " but got " + v.getClass().getName()
                    );
                }
                Array.set(varArray, i, v);
            }
            invocationArgs[invocationArgs.length - 1] = varArray;
            return (T) invoke(method, instance, invocationArgs);
        }

        return (T) invoke(method, instance, arguments);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Method method, Object instance, Object[] args) {
        if (!method.isAccessible()) {
            method.setAccessible(true); // Java 8 way
        }
        try {
            return (T) method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeConstructor(String className, Object... arguments) {
        Class<?>[] argumentTypes = getArgumentTypes(arguments);
        try {
            Class<T> cls = (Class<T>) Class.forName(className);
            Constructor<T> constructor = cls.getConstructor(argumentTypes);
            return constructor.newInstance(arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?>[] getArgumentTypes(Object[] arguments) {
        Class<?>[] argumentTypes = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentTypes[i] = arguments[i].getClass();
        }
        return argumentTypes;
    }
}
