package com.autoever.chajava

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import android.Manifest
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var splashView: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private var currentCameraIndex = 0
    private val CAMERA_PERMISSION_CODE = 100
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        splashView = findViewById(R.id.splashView)
        webView = findViewById(R.id.webView)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // WebView 설정
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true  // 추가
        webSettings.allowFileAccess = true  // 추가


        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebView", "Page loaded: $url")

                // 페이지 로드 완료 후 함수 존재 여부 확인
                view?.evaluateJavascript(
                    "(function() { return !!window.receiveImage; })()",
                    { result -> Log.d("WebView", "receiveImage exists: $result") }
                )
            }
        }
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("https://chajava.store/")

        // Splash 애니메이션
        Handler().postDelayed({ fadeOut(splashView) }, 3000)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack() // 이전 페이지로 이동
        } else {
            super.onBackPressed() // 기본 동작 수행
        }
    }

    private fun fadeOut(view: View) {
        val fadeOutAnimation = AlphaAnimation(1f, 0f).apply {
            duration = 500
            fillAfter = true
        }
        fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) { view.visibility = VISIBLE }
            override fun onAnimationEnd(animation: Animation?) { view.visibility = GONE }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view.startAnimation(fadeOutAnimation)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            val outputStream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

            val sanitizedBase64Image = base64Image
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "")
                .replace("\r", "")

            val javascriptString = """
                javascript:window.receiveImage("data:image/jpeg;base64,$sanitizedBase64Image");
            """.trimIndent()

            Log.d("원본 길이",base64Image.length.toString())
            Log.d("수정 후 길이",javascriptString.length.toString())
            webView.post {
                webView.evaluateJavascript(javascriptString) { result ->
                    Log.d("WebView", "Image transfer result: $result")
                }
            }
        }
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun openCameraAndGallery() {
            Log.d("나 지금 눌리고 있니?", "ㅇㅇㅇㅇ")
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

                startCamera()
            } else {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
        }

    }

    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    // 권한 허용 요청에 대한 결과를 반환합니다.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            }
        }
    }

}
