// MainActivity.java - 主界面

package io.github.zhis.hyperos.personalized;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private View pageHome, pageSettings;
    private View pageHomeDefault, pageHomeFeatures;
    private TextView tabHome, tabSettings;
    private TextView tvGithub;
    private View textCard;
    private View textContent;
    private ImageView bgImage;
    private ImageView blurCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XLog.i("onCreate 开始");

        // 全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        setContentView(R.layout.activity_main);
        XLog.d("setContentView 完成");

        // 隐藏系统栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController ctrl = getWindow().getInsetsController();
            if (ctrl != null) {
                ctrl.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
                ctrl.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                XLog.d("系统栏已隐藏");
            }
        }

        // 绑定视图
        pageHome         = findViewById(R.id.page_home);
        pageSettings     = findViewById(R.id.page_settings);
        pageHomeDefault  = findViewById(R.id.page_home_default);
        pageHomeFeatures = findViewById(R.id.page_home_features);
        tabHome          = findViewById(R.id.tab_home);
        tabSettings      = findViewById(R.id.tab_settings);
        tvGithub         = findViewById(R.id.tv_github);
        textCard         = findViewById(R.id.text_card);
        textContent      = findViewById(R.id.text_content);

        bgImage   = findViewById(R.id.bg_image);
        blurCard  = findViewById(R.id.blur_layer_card);
        View bottomBar    = findViewById(R.id.bottom_bar);
        ImageView blurBar = findViewById(R.id.blur_layer_bar);

        // 圆角裁剪(与 drawable 中的 radius 保持一致)
        float density = getResources().getDisplayMetrics().density;
        clipRound(textCard,  20f * density);
        clipRound(bottomBar, 30f * density);
        clipRound(blurCard,  20f * density);
        clipRound(blurBar,   30f * density);
        XLog.d("圆角裁剪已应用");

        // 初始化 Tab 状态
        switchTab(true);

        // 毛玻璃
        bgImage.post(() -> bgImage.post(() -> {
            XLog.d("开始生成毛玻璃效果");
            fixCardSize();
            textCard.post(() -> {
                applyFrostedGlass(bgImage, textCard, blurCard);
                applyFrostedGlass(bgImage, bottomBar, blurBar);
                XLog.i("毛玻璃效果生成完成");
            });
        }));

        // 底部 Tab 点击
        tabHome.setOnClickListener(v -> switchTab(true));
        tabSettings.setOnClickListener(v -> switchTab(false));

        // 中间卡片点击切换内容
        textCard.setOnClickListener(v -> {
            if (pageHome.getVisibility() == View.VISIBLE) {
                toggleHomeContent();
            }
        });

        // Github 链接点击
        tvGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/ZhisQWQ/HyperOS_Personalized"));
            startActivity(intent);
        });

        XLog.i("onCreate 完成");
    }

    @Override
    protected void onResume() {
        super.onResume();
        XLog.d("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        XLog.d("onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XLog.i("onDestroy");
    }

    private void clipRound(View view, float radius) {
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        view.setClipToOutline(true);
    }

     // 固定中间卡片尺寸
     // 分别测量“默认信息”和“功能列表”，取宽、高的最大值，防止重绘错误显示
    private void fixCardSize() {
        if (textCard == null || textContent == null
                || pageHomeDefault == null || pageHomeFeatures == null) {
            return;
        }

        int padLeft = textContent.getPaddingLeft();
        int padRight = textContent.getPaddingRight();
        int padTop = textContent.getPaddingTop();
        int padBottom = textContent.getPaddingBottom();

        int defaultVisibility = pageHomeDefault.getVisibility();
        int featuresVisibility = pageHomeFeatures.getVisibility();

        // 临时改为 INVISIBLE，保证 GONE 的页面也能被测量，但不会显示出来
        pageHomeDefault.setVisibility(View.INVISIBLE);
        pageHomeFeatures.setVisibility(View.INVISIBLE);

        int maxWidth = 0;
        int maxHeight = 0;
        View[] pages = new View[]{pageHomeDefault, pageHomeFeatures};

        for (View page : pages) {
            page.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            maxWidth = Math.max(maxWidth, page.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, page.getMeasuredHeight());
        }

        // 恢复原来的可见性
        pageHomeDefault.setVisibility(defaultVisibility);
        pageHomeFeatures.setVisibility(featuresVisibility);

        int targetWidth = maxWidth + padLeft + padRight;
        int targetHeight = maxHeight + padTop + padBottom;

        ViewGroup.LayoutParams lp = textCard.getLayoutParams();
        if (lp.width != targetWidth || lp.height != targetHeight) {
            lp.width = targetWidth;
            lp.height = targetHeight;
            textCard.setLayoutParams(lp);
            XLog.d("固定卡片尺寸 = " + targetWidth + "x" + targetHeight);
        }
    }

    private void applyFrostedGlass(ImageView bgImage, View container, ImageView blurLayer) {
        int bgW = bgImage.getWidth(), bgH = bgImage.getHeight();
        int cW  = container.getWidth(), cH = container.getHeight();
        if (bgW <= 0 || bgH <= 0 || cW <= 0 || cH <= 0) {
            XLog.w("applyFrostedGlass 尺寸无效，跳过");
            return;
        }

        int[] loc = new int[2];
        container.getLocationOnScreen(loc);
        int x = Math.max(0, Math.min(loc[0], bgW - 1));
        int y = Math.max(0, Math.min(loc[1], bgH - 1));
        int w = Math.min(cW, bgW - x);
        int h = Math.min(cH, bgH - y);
        if (w <= 0 || h <= 0) {
            XLog.w("applyFrostedGlass 裁剪区域为空，跳过");
            return;
        }

        try {
            Bitmap full = Bitmap.createBitmap(bgW, bgH, Bitmap.Config.ARGB_8888);
            bgImage.draw(new Canvas(full));
            Bitmap cropped = Bitmap.createBitmap(full, x, y, w, h);
            full.recycle();
            blurLayer.setImageBitmap(cropped);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 模糊半径 10，级联模糊 3 次
                float r = 10f;
                RenderEffect b1 = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
                RenderEffect b2 = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
                RenderEffect b3 = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
                blurLayer.setRenderEffect(
                        RenderEffect.createChainEffect(
                                RenderEffect.createChainEffect(b1, b2), b3));
                XLog.d("RenderEffect 三级模糊(r=10) 已应用");
            } else {
                blurLayer.setAlpha(0.55f);
                XLog.d("SDK < 31，降级为半透明");
            }
        } catch (Exception e) {
            XLog.e("毛玻璃生成异常 - " + e.getMessage());
        }
    }

    // 切换主页内的两种内容
    private void toggleHomeContent() {
        if (pageHomeDefault.getVisibility() == View.VISIBLE) {
            // 切换到功能列表
            pageHomeDefault.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                pageHomeDefault.setVisibility(View.GONE);
                pageHomeDefault.setAlpha(1f);
                pageHomeFeatures.setVisibility(View.VISIBLE);
                pageHomeFeatures.setAlpha(0f);
                pageHomeFeatures.animate().alpha(1f).setDuration(200).withEndAction(() -> {
                    textCard.post(() -> applyFrostedGlass(bgImage, textCard, blurCard));
                }).start();
            }).start();
        } else {
            // 切换到默认信息
            pageHomeFeatures.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                pageHomeFeatures.setVisibility(View.GONE);
                pageHomeFeatures.setAlpha(1f);
                pageHomeDefault.setVisibility(View.VISIBLE);
                pageHomeDefault.setAlpha(0f);
                pageHomeDefault.animate().alpha(1f).setDuration(200).withEndAction(() -> {
                    textCard.post(() -> applyFrostedGlass(bgImage, textCard, blurCard));
                }).start();
            }).start();
        }
    }

    private void switchTab(boolean home) {
        XLog.d("switchTab -> " + (home ? "主页" : "设置"));
        pageHome.setVisibility(home ? View.VISIBLE : View.GONE);
        pageSettings.setVisibility(home ? View.GONE : View.VISIBLE);
        tabHome.setSelected(home);
        tabSettings.setSelected(!home);
        tabHome.setTypeface(null, home ? Typeface.BOLD : Typeface.NORMAL);
        tabHome.setTextColor(home ? 0xFF000000 : 0x66000000);
        tabSettings.setTypeface(null, home ? Typeface.NORMAL : Typeface.BOLD);
        tabSettings.setTextColor(home ? 0x66000000 : 0xFF000000);
    }
}