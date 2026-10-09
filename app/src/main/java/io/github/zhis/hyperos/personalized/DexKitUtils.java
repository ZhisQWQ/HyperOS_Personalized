// DexKitUtils.java - DexKit 加载与 Bridge 工具

package io.github.zhis.hyperos.personalized;

import org.luckypray.dexkit.DexKitBridge;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DexKitUtils {

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private DexKitUtils() {}

    public static boolean ensureLoaded() {
        if (LOADED.get()) return true;
        synchronized (DexKitUtils.class) {
            if (LOADED.get()) return true;
            String path = locateNativeLib();
            if (path == null) {
                XLog.e("无法定位 libdexkit.so");
                return false;
            }
            try {
                System.load(path);
                LOADED.set(true);
                XLog.d("libdexkit.so 加载成功: " + path);
                return true;
            } catch (Throwable t) {
                XLog.e("libdexkit.so 加载失败: " + t);
                return false;
            }
        }
    }

    public static DexKitBridge createBridge(String apkPath) {
        if (!ensureLoaded()) return null;
        try {
            return DexKitBridge.create(apkPath);
        } catch (Throwable t) {
            XLog.e("创建 DexKitBridge 失败 -> " + t);
            return null;
        }
    }

    public static void closeQuietly(DexKitBridge bridge) {
        if (bridge == null) return;
        try { bridge.close(); } catch (Throwable ignored) {}
    }

    private static String locateNativeLib() {
        String modulePath = HookInit.getModulePath();
        if (modulePath == null) return null;

        String candidate = modulePath.replace("/base.apk", "/lib/arm64/libdexkit.so");
        if (new File(candidate).exists()) return candidate;

        File parent = new File(modulePath).getParentFile();
        if (parent != null) {
            File fallback = new File(parent, "lib/arm64/libdexkit.so");
            if (fallback.exists()) return fallback.getAbsolutePath();
        }
        return null;
    }
}