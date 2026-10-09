// XLog.java - 日志工具

package io.github.zhis.hyperos.personalized;

import io.github.libxposed.api.XposedModule;

public final class XLog {

    public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int INFO = 3;
    public static final int DEBUG = 4;

    // 全局日志过滤
    private static volatile int level = WARNING;
    private static volatile XposedModule module;
    private static final String TAG = "HyperOS_Personalized";

    private XLog() {}

    public static void init(XposedModule m) { module = m; }

    public static void setLevel(int l) { level = l; }

    public static boolean isDebug() { return level >= DEBUG; }

    public static boolean isInfo()  { return level >= INFO; }

    public static void e(String msg) { if (level >= ERROR)   output(ERROR, msg); }
    public static void w(String msg) { if (level >= WARNING) output(WARNING, msg); }
    public static void i(String msg) { if (level >= INFO)    output(INFO, msg); }
    public static void d(String msg) { if (level >= DEBUG)   output(DEBUG, msg); }

    private static void output(int lv, String msg) {
        String formatted = shortLevel(lv) + "| " + msg;
        int androidLv = toAndroidLevel(lv);
        XposedModule m = module;
        if (m != null) {
            m.log(androidLv, TAG, formatted);
        } else {
            android.util.Log.println(androidLv, TAG, formatted);
        }
    }

    private static int toAndroidLevel(int lv) {
        switch (lv) {
            case ERROR:   return android.util.Log.ERROR;
            case WARNING: return android.util.Log.WARN;
            case DEBUG:   return android.util.Log.DEBUG;
            default:      return android.util.Log.INFO;
        }
    }

    private static String shortLevel(int lv) {
        switch (lv) {
            case ERROR:   return "E";
            case WARNING: return "W";
            case INFO:    return "I";
            default:      return "D";
        }
    }
}