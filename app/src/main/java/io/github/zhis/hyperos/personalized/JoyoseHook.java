// JoyoseHook.java - 禁用 Joyose 的云控

package io.github.zhis.hyperos.personalized;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public class JoyoseHook {

    private static final String TARGET_STRING = "job exist, sync local...";

    private final XposedModule module;
    private final ClassLoader cl;
    private final PackageReadyParam param;

    public JoyoseHook(XposedModule module, ClassLoader cl, PackageReadyParam param) {
        this.module = module;
        this.cl = cl;
        this.param = param;
    }

    public void hook() {
        DexKitBridge bridge = DexKitUtils.createBridge(param.getApplicationInfo().sourceDir);
        if (bridge == null) return;

        try {
            MethodDataList result = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .usingStrings(TARGET_STRING)
                            .returnType(void.class)));

            if (result == null || result.isEmpty()) {
                XLog.e("未找到云控同步方法");
                return;
            }
            XLog.d("找到 " + result.size() + " 个候选方法");

            Method target = result.get(0).getMethodInstance(cl);
            if (target == null) {
                XLog.e("getMethodInstance 返回 null");
                return;
            }
            HookUtils.hookVoid(module, target);
        } catch (Throwable t) {
            XLog.e("JoyoseHook 失败 -> " + t);
        } finally {
            DexKitUtils.closeQuietly(bridge);
        }
    }
}