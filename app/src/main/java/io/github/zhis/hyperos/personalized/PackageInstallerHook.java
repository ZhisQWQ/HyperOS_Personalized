// PackageInstallerHook.java - 应用包管理器 Hook 集合

package io.github.zhis.hyperos.personalized;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public class PackageInstallerHook {

    private static final String T = "PackageInstallerHook: ";

    private final XposedModule module;
    private final ClassLoader cl;
    private final PackageReadyParam param;

    public PackageInstallerHook(XposedModule module, ClassLoader cl, PackageReadyParam param) {
        this.module = module;
        this.cl = cl;
        this.param = param;
    }

    public void hook() {
        DexKitBridge bridge = DexKitUtils.createBridge(param.getApplicationInfo().sourceDir);
        if (bridge == null) return;
        try {
            hookDisableAd(bridge);
            hookInstallRiskDisable(bridge);
            hookDisableCloudCheck(bridge);
            hookDisableCountChecking();
            hookAllAsSystemApp(bridge);
            hookDisableAppInfoUpload(bridge);
            XLog.i(T + "全部 Hook 结束");
        } finally {
            DexKitUtils.closeQuietly(bridge);
        }
    }

    // 通用模板
    @FunctionalInterface
    private interface MethodAction {
        void apply(Method m, String tag) throws Throwable;
    }

    private void forEachMethod(MethodDataList list, String tag, MethodAction action) {
        if (list == null || list.isEmpty()) {
            XLog.e(T + tag + " 未找到目标方法");
            return;
        }
        for (MethodData md : list) {
            try {
                Method m = md.getMethodInstance(cl);
                if (m == null) continue;
                action.apply(m, tag);
            } catch (Throwable t) {
                XLog.e(T + tag + " 处理失败 -> " + t);
            }
        }
    }

    private void hookAllReturn(MethodDataList list, String tag, Object constant) {
        forEachMethod(list, tag, (m, t) -> {
            m.setAccessible(true);
            module.hook(m).intercept(chain -> constant);
            XLog.i(T + t + " Hook " + m);
        });
    }

    private MethodDataList findBoolByString(DexKitBridge bridge, String key, boolean exact) {
        MethodMatcher mm = MethodMatcher.create();
        if (exact) mm.addUsingString(key, StringMatchType.Equals);
        else       mm.usingStrings(key);
        return bridge.findMethod(FindMethod.create()
                .matcher(mm.returnType(boolean.class)));
    }

    // 各功能
    private void hookDisableAd(DexKitBridge bridge) {
        String[] keys = {"ads_enable", "app_store_recommend", "virus_scan_install"};
        for (String key : keys) {
            try {
                hookAllReturn(findBoolByString(bridge, key, false),
                        "DisableAd(" + key + ")", false);
            } catch (Throwable t) {
                XLog.e(T + "DisableAd 查找失败 " + key + " -> " + t);
            }
        }
    }

    private void hookInstallRiskDisable(DexKitBridge bridge) {
        String[] keys = {
                "secure_verify_enable",
                "installerOpenSafetyModel",
                "android.provider.MiuiSettings$Ad"
        };
        for (String key : keys) {
            try {
                hookAllReturn(findBoolByString(bridge, key, true),
                        "InstallRiskDisable(" + key + ")", false);
            } catch (Throwable t) {
                XLog.e(T + "InstallRiskDisable 查找失败 " + key + " -> " + t);
            }
        }
    }

    private void hookDisableCloudCheck(DexKitBridge bridge) {
        Class<?> apkInfoCls = tryLoad("com.miui.packageInstaller.model.ApkInfo");
        Class<?> contCls    = tryLoad("kotlin.coroutines.Continuation");

        MethodDataList list;
        try {
            list = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(6)
                            .paramTypes(Context.class, null, int.class,
                                    apkInfoCls, HashMap.class, contCls)
                            .returnType(Object.class)));
        } catch (Throwable t) {
            XLog.e(T + "DisableCloudCheck 查找失败 -> " + t);
            return;
        }

        forEachMethod(list, "DisableCloudCheck", (m, tag) -> {
            m.setAccessible(true);
            module.hook(m).intercept(chain -> {
                try {
                    Class<?> cloudParamsCls = cl.loadClass(
                            "com.miui.packageInstaller.model.CloudParams");
                    Object cloudParams = cloudParamsCls.getDeclaredConstructor().newInstance();

                    Class<?> successCls = cl.loadClass(
                            "com.miui.packageInstaller.model.CloudResult$Success");
                    Constructor<?> ctor = successCls.getDeclaredConstructor(cloudParamsCls);
                    ctor.setAccessible(true);
                    return ctor.newInstance(cloudParams);
                } catch (Throwable t) {
                    XLog.e(T + "DisableCloudCheck 伪造失败，回退原方法 -> " + t);
                    return chain.proceed();
                }
            });
            XLog.i(T + tag + " Hook " + m);
        });
    }

    private void hookDisableCountChecking() {
        try {
            Class<?> clazz = cl.loadClass(
                    "com.miui.packageInstaller.model.RiskControlRules");
            Method m = ReflectUtils.requireMethod(clazz, "getCurrentLevel");
            HookUtils.hookReturn(module, m, 0);
        } catch (Throwable t) {
            XLog.e(T + "DisableCountChecking 失败 -> " + t);
        }
    }

    private void hookAllAsSystemApp(DexKitBridge bridge) {
        MethodDataList list;
        try {
            list = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(1)
                            .paramTypes(ApplicationInfo.class)
                            .returnType(boolean.class)));
        } catch (Throwable t) {
            XLog.e(T + "AllAsSystemApp 查找失败 -> " + t);
            return;
        }

        forEachMethod(list, "AllAsSystemApp", (m, tag) -> {
            if (!"com.android.packageinstaller.PackageUtil".equals(
                    m.getDeclaringClass().getName())) {
                return;
            }
            m.setAccessible(true);
            module.hook(m).intercept(chain -> {
                Object[] args = HookUtils.argsOf(chain.getArgs());
                if (args.length >= 1 && args[0] instanceof ApplicationInfo) {
                    ((ApplicationInfo) args[0]).flags |= ApplicationInfo.FLAG_SYSTEM;
                }
                return chain.proceed(args);
            });
            XLog.i(T + tag + " Hook " + m);
        });
    }

    private void hookDisableAppInfoUpload(DexKitBridge bridge) {
        // /avl/upload/
        try {
            MethodDataList list = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(4)
                            .usingStrings("appSourcepackageName", "packageName")
                            .returnType(void.class)));
            hookAllReturn(list, "AppInfoUpload(/avl/upload/)", null);
        } catch (Throwable t) {
            XLog.e(T + "AppInfoUpload(/avl/upload/) 查找失败 -> " + t);
        }

        // /info/layout
        try {
            MethodDataList list = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(7)
                            .paramTypes(String.class, String.class, String.class,
                                    Integer.class, String.class, String.class, null)
                            .returnType(Object.class)));
            hookAllReturn(list, "AppInfoUpload(/info/layout)", null);
        } catch (Throwable t) {
            XLog.e(T + "AppInfoUpload(/info/layout) 查找失败 -> " + t);
        }
    }

    private Class<?> tryLoad(String name) {
        try { return cl.loadClass(name); } catch (Throwable t) { return null; }
    }
}