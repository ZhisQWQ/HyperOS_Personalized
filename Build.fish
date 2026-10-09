#!/usr/bin/env fish
# Build.fish - 编译、对齐、签名并重命名 APK 的自动化程序

# 全局配置
set GRADLE_EXEC "/storage/Storage[1]/APP/gradle/bin/gradle"
set KEYSTORE_FILE "keystore.jks"
set KEY_VALIDITY 10000
set KEY_DNAME "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown"

# 日志
function log
    set -l level $argv[1]
    set -l msg (string join ' ' $argv[2..-1])

    switch $level
        case Info I
            set_color green
            echo "I| $msg"
        case Warning W
            set_color yellow
            echo "W| $msg"
        case Error E
            set_color red
            echo "E| $msg"
        case '*'
            set_color normal
            echo "$level| $msg"
    end

    set_color normal
end

# 定位项目目录
set PROJECT_DIR (cd (dirname (status filename)); and pwd)
set OUTPUT_APK_NAME (basename $PROJECT_DIR).apk
cd "$PROJECT_DIR"; or exit 1

log Info "项目目录: $PROJECT_DIR"
log Info "输出名称: $OUTPUT_APK_NAME"

# 检查 gradle 可执行
if not test -x "$GRADLE_EXEC"
    log Error "gradle 不可执行: $GRADLE_EXEC"
    exit 1
end

# 读取密钥变量
set KEY_FILE "$PROJECT_DIR/Build_Key.txt"
if not test -f "$KEY_FILE"
    log Error "找不到密钥配置文件 $KEY_FILE"
    exit 1
end

set KEY_ALIAS ""
set STORE_PASS ""
set KEY_PASS ""

while read -l line
    set line (string trim -- $line)
    test -z "$line"; and continue
    string match -q '#*' -- $line; and continue

    set -l kv (string split -m 1 '=' -- $line)
    test (count $kv) -eq 2; or continue

    set -l k (string trim -- $kv[1])
    set -l v (string trim -- $kv[2])
    # 去掉成对的外层引号
    set v (string replace -r '^"(.*)"$' '$1' -- "$v")
    set v (string replace -r "^'(.*)'\$" '$1' -- "$v")

    switch $k
        case KEY_ALIAS
            set KEY_ALIAS $v
        case STORE_PASS
            set STORE_PASS $v
        case KEY_PASS
            set KEY_PASS $v
    end
end < "$KEY_FILE"

if test -z "$KEY_ALIAS"; or test -z "$STORE_PASS"; or test -z "$KEY_PASS"
    log Error "$KEY_FILE 中缺少 KEY_ALIAS / STORE_PASS / KEY_PASS"
    exit 1
end
log Info "密钥配置已加载: alias=$KEY_ALIAS"

# 查找工具
function find_tool --argument-names name
    if command -q $name
        command -v $name
        return 0
    end

    if set -q ANDROID_HOME
        set -l p (find "$ANDROID_HOME/build-tools" -maxdepth 2 -type f -name $name 2>/dev/null | sort -V | tail -n 1)
        if test -n "$p"
            echo "$p"
            return 0
        end
    end

    return 1
end

set ZIPALIGN (find_tool zipalign)
set APKSIGNER (find_tool apksigner)
set JAVA_KEYTOOL (command -v keytool 2>/dev/null)

# 检查必要工具
set -l missing
test -z "$ZIPALIGN";    and set -a missing zipalign
test -z "$APKSIGNER";   and set -a missing apksigner
test -z "$JAVA_KEYTOOL"; and set -a missing keytool

if test (count $missing) -ne 0
    log Error "缺少工具: $missing（请配置 ANDROID_HOME 或 PATH）"
    exit 1
end
log Info "工具就绪: zipalign=$ZIPALIGN | apksigner=$APKSIGNER | keytool=$JAVA_KEYTOOL"

# 显式删除旧的 APK
log Info "[1|6] 清理旧 APK"
set -l old_apks (find "$PROJECT_DIR" -maxdepth 1 -type f -name '*.apk' 2>/dev/null)
if test (count $old_apks) -gt 0
    rm -f -- $old_apks
end

# 清理构建缓存
log Info "[2|6] 清理构建缓存"
if not "$GRADLE_EXEC" clean
    log Error "清理构建缓存失败"
    exit 1
end

# 编译
log Info "[3|6] 编译"
if not "$GRADLE_EXEC" assembleRelease
    log Error "编译失败"
    exit 1
end

set -l RELEASE_DIR "app/build/outputs/apk/release"
set UNSIGNED_APK (find "$RELEASE_DIR" -maxdepth 1 -type f -name '*-unsigned.apk' 2>/dev/null | head -n 1)

if test -z "$UNSIGNED_APK"
    set UNSIGNED_APK (find "$RELEASE_DIR" -maxdepth 1 -type f -name '*.apk' \
        -not -name '*-aligned.apk' -not -name '*-signed.apk' 2>/dev/null | head -n 1)
end

if test -z "$UNSIGNED_APK"
    log Error "未找到编译产物"
    exit 1
end

# 生成密钥库
log Info "[4|6] 生成密钥库"
if not test -f "$KEYSTORE_FILE"
    if not "$JAVA_KEYTOOL" -genkey -v -keystore "$KEYSTORE_FILE" \
            -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 \
            -validity "$KEY_VALIDITY" \
            -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
            -dname "$KEY_DNAME"
        log Error "密钥库生成失败"
        exit 1
    end
else
    # 验证现有 keystore 的别名和密码
    if not "$JAVA_KEYTOOL" -list -keystore "$KEYSTORE_FILE" \
            -alias "$KEY_ALIAS" -storepass "$STORE_PASS" >/dev/null 2>&1
        log Error "现有 keystore 的别名或密码不匹配"
        exit 1
    end
end

# 对齐和签名
log Info "[5|6] 对齐并签名"
set ALIGNED_APK "app/build/outputs/apk/release/app-release-aligned.apk"
set SIGNED_APK  "app/build/outputs/apk/release/app-release-signed.apk"

if not "$ZIPALIGN" -v -p 4 "$UNSIGNED_APK" "$ALIGNED_APK"
    log Error "对齐失败"
    exit 1
end

if not "$APKSIGNER" sign --ks "$KEYSTORE_FILE" \
        --ks-key-alias "$KEY_ALIAS" \
        --ks-pass "pass:$STORE_PASS" --key-pass "pass:$KEY_PASS" \
        --out "$SIGNED_APK" "$ALIGNED_APK"
    log Error "签名失败"
    exit 1
end

if not "$APKSIGNER" verify --verbose "$SIGNED_APK"
    log Error "验签失败"
    exit 1
end

# 输出
log Info "[6|6] 输出到目录"
if not cp "$SIGNED_APK" "$PROJECT_DIR/$OUTPUT_APK_NAME"
    log Error "复制失败"
    exit 1
end
rm -f "$ALIGNED_APK" "$SIGNED_APK"

log Info "完成: $PROJECT_DIR/$OUTPUT_APK_NAME"