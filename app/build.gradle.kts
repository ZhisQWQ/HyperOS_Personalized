plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.zhis.hyperos.personalized"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.github.zhis.hyperos.personalized"
        minSdk    = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.0"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // 统一打包配置
    packaging {
        jniLibs {
            // .so 不压缩
            useLegacyPackaging = true
        }
        resources {
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
}

dependencies {
    // LSPosed API 102，运行时由框架提供
    compileOnly("io.github.libxposed:api:102.0.0")
    // AndroidX 注解
    compileOnly("androidx.annotation:annotation:1.9.1")
    // DexKit
    implementation("org.luckypray:dexkit:2.0.2")
}