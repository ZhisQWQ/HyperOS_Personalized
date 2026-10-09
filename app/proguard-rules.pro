-keep class io.github.zhis.hyperos.personalized.HookInit { *; }
-keep class io.github.zhis.hyperos.personalized.hooks.** { *; }
-keepattributes *Annotation*
# DexKit
-keep class org.luckypray.dexkit.** { *; }
-dontwarn org.luckypray.dexkit.**