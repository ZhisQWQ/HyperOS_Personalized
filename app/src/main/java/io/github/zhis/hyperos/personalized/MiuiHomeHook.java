// MiuiHomeHook.java - 隐藏最近任务界面的清理按钮

package io.github.zhis.hyperos.personalized;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class MiuiHomeHook {

    private static final String TARGET_CLASS = "com.miui.home.recents.views.RecentsContainer";
    private static final String TARGET_METHOD = "setupVisible";
    private static final String FIELD_NAME = "mMemoryAndClearContainer";

    private final XposedModule module;
    private final ClassLoader cl;

    public MiuiHomeHook(XposedModule module, ClassLoader cl) {
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
        method.setAccessible(true);

        module.hook(method).intercept(chain -> {
            Object result = chain.proceed();
            try {
                Object thisObj = chain.getThisObject();
                Field field = ReflectUtils.findField(thisObj.getClass(), FIELD_NAME);
                if (field == null) {
                    XLog.e("未找到字段 " + FIELD_NAME);
                    return result;
                }
                field.setAccessible(true);
                Object container = field.get(thisObj);
                if (container instanceof ViewGroup) {
                    ((ViewGroup) container).setVisibility(View.GONE);
                    XLog.d("已隐藏清理按钮容器");
                }
            } catch (Throwable t) {
                XLog.e("隐藏清理按钮失败 -> " + t);
            }
            return result;
        });

        XLog.i("Hook 成功 - " + TARGET_CLASS + "." + TARGET_METHOD);
    }
}