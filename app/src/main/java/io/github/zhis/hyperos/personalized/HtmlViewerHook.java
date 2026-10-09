// HtmlViewerHook.java - 禁用 HTML 查看器的云控

package io.github.zhis.hyperos.personalized;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class HtmlViewerHook {

    private static final String TARGET_CLASS = "com.android.settings.cloud.JobTask";
    private static final String TARGET_METHOD = "updateCloudAllData";

    private final XposedModule module;
    private final ClassLoader cl;

    public HtmlViewerHook(XposedModule module, ClassLoader cl) {
        this.module = module;
        this.cl = cl;
    }

    public void hook() throws Throwable {
        Class<?> clazz = Class.forName(TARGET_CLASS, false, cl);
        XLog.d("找到类 " + clazz.getName());

        Method method = ReflectUtils.findAnyMethod(clazz, TARGET_METHOD);
        if (method == null) {
            XLog.e("未找到方法 " + TARGET_CLASS + "." + TARGET_METHOD);
            return;
        }
        HookUtils.hookVoid(module, method);
    }
}