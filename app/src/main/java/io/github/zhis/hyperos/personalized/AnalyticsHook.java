// AnalyticsHook.java - 禁用 Analytics 的遥测数据收集

package io.github.zhis.hyperos.personalized;

import android.content.Context;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class AnalyticsHook {

    private final XposedModule module;
    private final ClassLoader cl;

    public AnalyticsHook(XposedModule module, ClassLoader cl) {
        this.module = module;
        this.cl = cl;
    }

    public void hook() {
        XLog.d("开始 Hook Analytics");

        try {
            Class<?> c1 = Class.forName("a.b.a.a.a.c.a", false, cl);
            XLog.d("找到类 " + c1.getName());
            hookReturn(c1, "K", new Class<?>[]{Context.class}, 0, "拦截方法3");
        } catch (Throwable t) {
            XLog.e("c1 类加载失败 -> " + t);
        }

        try {
            Class<?> c2 = Class.forName("a.b.a.f.b", false, cl);
            XLog.d("找到类 " + c2.getName());
            hookReturn(c2, "r", new Class<?>[]{long.class, long.class}, null, "拦截方法1");
        } catch (Throwable t) {
            XLog.e("c2 类加载失败 -> " + t);
        }
    }

    private void hookReturn(Class<?> clazz, String name, Class<?>[] paramTypes,
                            Object value, String tag) {
        try {
            Method m = ReflectUtils.requireMethod(clazz, name, paramTypes);
            m.setAccessible(true);
            module.hook(m).intercept(chain -> {
                if (XLog.isDebug()) XLog.d(tag + " -> " + value);
                return value;
            });
            XLog.i("Hook 成功 - " + clazz.getName() + "." + name);
        } catch (Throwable t) {
            XLog.e("Hook 失败 - " + clazz.getName() + "." + name + " -> " + t);
        }
    }
}