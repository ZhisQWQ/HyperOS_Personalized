// HookInit.java - LSPosed 模块入口

package io.github.zhis.hyperos.personalized;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public class HookInit extends XposedModule {

    private static volatile String MODULE_PATH;

    public static String getModulePath() { return MODULE_PATH; }

    @FunctionalInterface
    interface HookAction {
        void hook(HookContext ctx) throws Throwable;
    }

    static final class HookContext {
        final XposedModule module;
        final ClassLoader cl;
        final PackageReadyParam param;
        final String pkg;
        final String proc;

        HookContext(XposedModule module, PackageReadyParam param, String proc) {
            this.module = module;
            this.cl = param.getClassLoader();
            this.param = param;
            this.pkg = param.getPackageName();
            this.proc = proc;
        }
    }

    private static final Map<String, HookAction> REGISTRY = new HashMap<>();
    static {
        REGISTRY.put("com.miui.home",
                c -> new MiuiHomeHook(c.module, c.cl).hook());
        REGISTRY.put("com.miui.securitycenter",
                c -> new SkipCountDownLimit(c.module, c.cl).hook());
        REGISTRY.put("com.android.htmlviewer",
                c -> new HtmlViewerHook(c.module, c.cl).hook());
        REGISTRY.put("com.xiaomi.joyose",
                c -> new JoyoseHook(c.module, c.cl, c.param).hook());
        REGISTRY.put("com.miui.analytics",
                c -> new AnalyticsHook(c.module, c.cl).hook());
        REGISTRY.put("com.miui.packageinstaller",
                c -> new PackageInstallerHook(c.module, c.cl, c.param).hook());
    }

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        MODULE_PATH = getModuleApplicationInfo().sourceDir;
        XLog.init(this);
        XLog.d("模块已加载, path=" + MODULE_PATH);
        XLog.d("进程名=" + param.getProcessName()
                + ", isSystemServer=" + param.isSystemServer());
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!param.isFirstPackage()) return;

        String pkg = param.getPackageName();
        HookAction action = REGISTRY.get(pkg);
        if (action == null) return;

        String proc = getCurrentProcessName();
        if (!matchProcess(pkg, proc)) {
            XLog.d("不在目标进程，跳过: " + pkg + "/" + proc);
            return;
        }

        XLog.i("开始 Hook: " + pkg + "/" + proc);
        try {
            action.hook(new HookContext(this, param, proc));
        } catch (Throwable t) {
            XLog.e("Hook 失败: " + pkg + "/" + proc + " -> " + t);
        }
    }

    private static boolean matchProcess(String pkg, String proc) {
        if ("com.android.htmlviewer".equals(pkg)) {
            return "com.android.htmlviewer:remote".equals(proc);
        }
        return pkg.equals(proc);
    }

    private String getCurrentProcessName() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            return (String) at.getMethod("currentProcessName").invoke(null);
        } catch (Throwable t) {
            XLog.e("获取进程名失败: " + t);
            return null;
        }
    }
}