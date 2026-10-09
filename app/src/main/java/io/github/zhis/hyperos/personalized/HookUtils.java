// HookUtils.java - Hook 常用封装

package io.github.zhis.hyperos.personalized;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;

public final class HookUtils {

    private HookUtils() {}

    // Hook 方法并返回固定值(null 表示返回 void/null)
    public static void hookReturn(XposedModule module, Method m, Object value) {
        if (m == null) return;
        try {
            m.setAccessible(true);
            module.hook(m).intercept(chain -> value);
            XLog.i("Hook 成功 - " + m);
        } catch (Throwable t) {
            XLog.e("Hook 失败 - " + m + " -> " + t);
        }
    }

    public static void hookVoid(XposedModule module, Method m) {
        hookReturn(module, m, null);
    }

    public static Object[] argsOf(List<Object> list) {
        return list.toArray(new Object[0]);
    }
}