// SkipCountDownLimit.java - 跳过危险操作拦截页面的倒计时

package io.github.zhis.hyperos.personalized;

import android.os.Bundle;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class SkipCountDownLimit {

    private final XposedModule module;
    private final ClassLoader cl;

    public SkipCountDownLimit(XposedModule module, ClassLoader cl) {
        this.module = module;
        this.cl = cl;
    }

    public void hook() throws Throwable {
        Class<?> clazz = Class.forName(
                "com.miui.permcenter.privacymanager.model.InterceptBaseActivity", false, cl);
        XLog.d("找到类 " + clazz.getName());

        Method target = ReflectUtils.requireMethod(clazz, "onCreate", Bundle.class);
        target.setAccessible(true);

        module.hook(target).intercept(chain -> {
            try {
                Object[] args = HookUtils.argsOf(chain.getArgs());
                Bundle bundle = (Bundle) args[0];
                if (bundle == null) {
                    bundle = new Bundle();
                    args[0] = bundle;
                }
                // 实测可跳过倒计时，并非拼写错误
                bundle.putInt("KET_STEP_COUNT", 0);
                bundle.putBoolean("KEY_ALLOW_ENABLE", true);
                return chain.proceed(args);
            } catch (Throwable t) {
                XLog.e("intercept 异常，回退原方法 -> " + t);
                return chain.proceed();
            }
        });

        XLog.i("Hook 成功 - SkipCountDownLimit");
    }
}