// ReflectUtils.java - 反射工具

package io.github.zhis.hyperos.personalized;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectUtils {

    private ReflectUtils() {}

    // 按名称 + 精确参数类型查找方法，找不到返回 null
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!name.equals(m.getName())) continue;
            Class<?>[] pt = m.getParameterTypes();
            if (pt.length != paramTypes.length) continue;
            boolean match = true;
            for (int i = 0; i < pt.length; i++) {
                if (pt[i] != paramTypes[i]) { match = false; break; }
            }
            if (match) return m;
        }
        return null;
    }

    // 仅按名称查找，命中第一个重载
    public static Method findAnyMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (name.equals(m.getName())) return m;
        }
        return null;
    }

    // 精确匹配，找不到抛异常
    public static Method requireMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Method m = findMethod(clazz, name, paramTypes);
        if (m == null) throw new RuntimeException("方法不存在: " + clazz.getName() + "." + name);
        return m;
    }

    // 沿继承链向上查找字段
    public static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        return null;
    }
}