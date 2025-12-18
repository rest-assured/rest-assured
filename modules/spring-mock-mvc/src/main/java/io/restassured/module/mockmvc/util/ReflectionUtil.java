package io.restassured.module.mockmvc.util;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;

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
        java.lang.reflect.Method method = ReflectionUtils.findMethod(instance instanceof Class ? (Class<?>) instance : instance.getClass(), methodName, argumentTypes);
        if (method == null) {
            throw new IllegalArgumentException("Cannot find method '" + methodName + "' in " + instance.getClass() + " (arguments=" + Arrays.toString(arguments) + ")");
        }
		// Line below is needed to access e.g. methods of anonymous objects (check AcceptTest)
        ReflectionUtils.makeAccessible(method);
        if (!method.isVarArgs() || argumentTypes.length == 0
                || (argumentTypes.length == arguments.length
                && Objects.equals(argumentTypes[argumentTypes.length - 1], arguments[arguments.length - 1].getClass()))) {
            return (T) ReflectionUtils.invokeMethod(method, instance, arguments);
        }
        //Try to pack arguments to vararg

        Object[] objectArrayNeededForInvocation = new Object[argumentTypes.length];
        Object varArgsArguments = getVarArgsArguments(argumentTypes, arguments);

        System.arraycopy(arguments, 0, objectArrayNeededForInvocation, 0, argumentTypes.length - 1);
        objectArrayNeededForInvocation[objectArrayNeededForInvocation.length - 1] = varArgsArguments;
        return (T) ReflectionUtils.invokeMethod(method, instance, objectArrayNeededForInvocation);
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
