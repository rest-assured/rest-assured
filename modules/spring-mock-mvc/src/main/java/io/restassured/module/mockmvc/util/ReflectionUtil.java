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
        final Class<?> targetClass = (instance instanceof Class<?> c) ? c : instance.getClass();

        Method method = ReflectionUtils.findMethod(targetClass, methodName, argumentTypes);
        if (method == null) {
            // Fallback: common mistake—asked for T but method takes T[]
            if (argumentTypes != null && argumentTypes.length == 1 && !argumentTypes[0].isArray()) {
                Class<?> arrayParam = java.lang.reflect.Array.newInstance(argumentTypes[0], 0).getClass();
                method = ReflectionUtils.findMethod(targetClass, methodName, arrayParam);
            }
        }
        if (method == null) {
            throw new IllegalArgumentException("Cannot find method '" + methodName + "' in "
                    + targetClass.getName() + " (arguments=" + Arrays.toString(arguments) + ")");
        }

        // Prefer the bridged (real) method over the synthetic bridge
        // Prefer the bridged (real) method over the synthetic bridge
        Method resolved = BridgeMethodResolver.findBridgedMethod(method);

        // If we still ended up on a bridge/synthetic, try to pick the concrete sibling
        if (resolved.isBridge() || resolved.isSynthetic()) {
            final String name = methodName;                // capture for lambda
            final Class<?>[] paramTypes = resolved.getParameterTypes(); // capture for lambda
            method = Arrays.stream(targetClass.getMethods())
                    .filter(m -> m.getName().equals(name))
                    .filter(m -> Arrays.equals(m.getParameterTypes(), paramTypes))
                    .filter(m -> !m.isBridge() && !m.isSynthetic())
                    .findFirst()
                    .orElse(resolved);
        } else {
            method = resolved;
        }
        // --- Unified array/varargs handling (works even if isVarArgs() is false due to bridge) ---
        Class<?>[] params = method.getParameterTypes();
        boolean lastIsArray = params.length > 0 && params[params.length - 1].isArray();

        if (lastIsArray) {
            int fixed = params.length - 1;
            Class<?> arrayType = params[params.length - 1];
            Class<?> componentType = arrayType.getComponentType();

            if (arguments.length == params.length) {
                Object last = arguments[arguments.length - 1];
                if (last == null) {
                    Object empty = java.lang.reflect.Array.newInstance(componentType, 0);
                    Object[] invocationArgs = Arrays.copyOf(arguments, arguments.length);
                    invocationArgs[invocationArgs.length - 1] = empty;
                    return (T) invoke(method, instance, invocationArgs);
                }
                if (arrayType.isInstance(last)) {
                    return (T) invoke(method, instance, arguments);
                }
                if (last.getClass().isArray()) {
                    throw new IllegalArgumentException("Array parameter type mismatch: expected "
                            + arrayType.getName() + " but got " + last.getClass().getName());
                }
                // else fall through to repack
            } else if (arguments.length < fixed) {
                throw new IllegalArgumentException("Too few arguments: expected at least " + fixed);
            }

            Object[] invocationArgs = new Object[params.length];
            if (fixed > 0) System.arraycopy(arguments, 0, invocationArgs, 0, fixed);

            int varCount = Math.max(0, arguments.length - fixed);
            Object varArray = java.lang.reflect.Array.newInstance(componentType, varCount);
            for (int i = 0; i < varCount; i++) {
                Object v = arguments[fixed + i];
                if (v != null && !componentType.isInstance(v)) {
                    throw new IllegalArgumentException("Vararg element not assignable: expected "
                            + componentType.getName() + " but got " + v.getClass().getName());
                }
                java.lang.reflect.Array.set(varArray, i, v);
            }
            invocationArgs[invocationArgs.length - 1] = varArray;
            return (T) invoke(method, instance, invocationArgs);
        }

        // Non-array signature
        return (T) invoke(method, instance, arguments);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Method method, Object instance, Object[] args) {
        if (!method.canAccess(instance instanceof Class ? null : instance)) method.setAccessible(true);
        try {
            return (T) method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (c instanceof RuntimeException re) throw re;
            if (c instanceof Error err) throw err;
            throw new RuntimeException(c);
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

    private static Object getVarArgsArguments(Class<?>[] argumentTypes, Object[] arguments) {
        Class<?> argumentType = argumentTypes[argumentTypes.length - 1];
        if (argumentType.isArray()) {
            argumentType = argumentType.getComponentType();
        }

        int numberOfVarArgParameters = arguments.length - argumentTypes.length + 1;
        Object varArgsArguments = Array.newInstance(argumentType, numberOfVarArgParameters);
        for (int j = 0, i = argumentTypes.length - 1; i < arguments.length; i++, j++) {
            Array.set(varArgsArguments, j, arguments[i]);
        }
        return varArgsArguments;
    }
}
