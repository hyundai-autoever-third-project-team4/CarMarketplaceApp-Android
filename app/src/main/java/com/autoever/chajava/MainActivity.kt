package com.autoever.chajava

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val splashView = findViewById<ImageView>(R.id.splashView)
        webView = findViewById(R.id.webView)


        // WindowInsets 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // WebView 설정
        webView.webViewClient = WebViewClient()
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // JavaScript 사용 허용

        // 웹 페이지 로드
        webView.loadUrl("https://theaterup.site/")

        // 3초 후에 fade out 애니메이션 실행
        Handler().postDelayed({
            fadeOut(splashView)
        }, 3000) // 3000ms = 3초
    }

    private fun fadeOut(view: View) {
        val fadeOutAnimation = AlphaAnimation(1f, 0f).apply {
            duration = 500 // 애니메이션 지속 시간 (1초)
            fillAfter = true // 애니메이션이 끝난 후 상태 유지
        }

        fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                view.visibility = VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = GONE // 애니메이션 끝난 후 숨김
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        view.startAnimation(fadeOutAnimation)
    }
}
