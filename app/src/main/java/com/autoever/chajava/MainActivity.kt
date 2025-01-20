package com.autoever.chajava

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
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
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.JsResult
import android.webkit.WebChromeClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var splashView: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private var currentCameraIndex = 0
    private val CAMERA_PERMISSION_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 2
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1

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
        webView.addJavascriptInterface(WebAppInterface(this), "Android") //웹뷰에서 WebAppInterface 함수 호출 허용

        // 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한이 없으면 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult
            ): Boolean {
                // AlertDialog 생성
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Alert")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        result.confirm() // JS alert에 대한 확인 응답
                    }
                    .setCancelable(false) // 다이얼로그가 취소 불가능하게 설정
                    .show()
                return true // true를 반환하여 JS alert을 처리했음을 알림
            }
        }


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

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // intent:// 스킴 처리
                if (url != null && url.startsWith("intent://")) {
                    try {
                        // 인텐트 URL을 처리하기 위해 인텐트를 생성
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        startActivity(intent)
                        return true // 웹뷰에서 처리하지 않음
                    } catch (e: Exception) {
                        Log.e("WebView", "Error parsing intent URL: $e")
                        return false // 웹뷰에서 처리하도록 함
                    }
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // 알림을 눌러 들어오는 경우
        val notificationUrl = intent.getStringExtra("url")
        val urlToLoad = notificationUrl ?: "https://chajava.store/"

        // 웹 페이지 로드 전에 인터넷 연결 확인
        if (hasInternetConnection()) {
            webView.loadUrl(urlToLoad)
        } else {
            Toast.makeText(this, "인터넷 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

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

    // 인터넷 연결 확인
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
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

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    processAndSendImage(imageBitmap)
                }
                REQUEST_PICK_IMAGE -> {
                    data?.data?.let { uri ->
                        val inputStream: InputStream? = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        processAndSendImage(bitmap)
                        inputStream?.close()
                    }
                }
            }
        }
    }

    private fun processAndSendImage(bitmap: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
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

        webView.post {
            webView.evaluateJavascript(javascriptString) { result ->
                Log.d("WebView", "Image transfer result: $result")
            }
        }
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun openCameraAndGallery() {
            runOnUiThread {
                val items = arrayOf("카메라로 촬영", "갤러리에서 선택")
                AlertDialog.Builder(context)
                    .setTitle("이미지 선택")
                    .setItems(items) { dialog, which ->
                        when (which) {
                            0 -> checkCameraPermission()
                            1 -> checkStoragePermission()
                        }
                    }
                    .show()
            }
        }
    }

    private fun checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상에서는 READ_MEDIA_IMAGES 권한 필요
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                // ActivityCompat.requestPermissions 사용
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    STORAGE_PERMISSION_CODE
                )
            }
        } else {
            // Android 13 미만에서는 READ_EXTERNAL_STORAGE 권한 필요
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
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
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // url intent 체크
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val notificationUrl = intent.getStringExtra("url")
        if (!notificationUrl.isNullOrEmpty()){
            webView.loadUrl(notificationUrl)
        }
    }

}