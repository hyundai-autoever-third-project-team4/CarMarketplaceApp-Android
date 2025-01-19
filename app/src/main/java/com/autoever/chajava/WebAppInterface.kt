package com.autoever.chajava

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

// 웹뷰에서 호출하는 함수
class WebAppInterface(private val context: Context) {
    // 로그인 했을 때 & 앱 시작 시 자동 로그인 된 때 호출
    @JavascriptInterface
    fun getToken(userId: Long){
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Firebase", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result
            Log.d("Firebase", "get token: $token")
            sendTokenToServer(userId, token)
        })
    }

    // 로그아웃 할 때 & 앱 시작 시 자동 로그아웃 된 때 호출
    @JavascriptInterface
    fun deleteToken(){
        // 기존 토큰 검색 후 서버에서 토큰 삭제
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Firebase", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result
            Log.d("Firebase", "get token to delete: $token")
            deleteTokenAtServer(token)
        })

        // 토큰 삭제
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful){
                Log.w("Firebase", "Deleting FCM token failed", task.exception)
                return@OnCompleteListener
            }

            Log.d("Firebase", "token deleted")
        })
    }
}


// 서버에 토큰 저장 요청
fun sendTokenToServer(userId: Long, token: String) {
    val currentTime = LocalDateTime.now()
    val serverUrl = URL("https://chajava.store/api/fcm/save")
    //val serverUrl = URL("http://10.0.2.2:8081/fcm/save")
    val conn = serverUrl.openConnection() as HttpURLConnection

    val saveThread = Thread {
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = """
                    {
                        "userId": "$userId",
                        "token": "$token",
                        "currentTime": "$currentTime"
                    }
                """.trimIndent()

            conn.outputStream.use { os: OutputStream ->
                os.write(payload.toByteArray())
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("Firebase", "FCM Token successfully sent to the server.")
            } else {
                Log.e("Firebase", "Failed to send FCM Token. Response Code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error sending FCM Token to server", e)
        } finally {
            conn.disconnect()
        }
    }
    saveThread.start()
    saveThread.join()
}

fun deleteTokenAtServer(token: String){
    val serverUrl = URL("https://chajava.store/api/fcm/delete/$token")
    //val serverUrl = URL("http://10.0.2.2:8081/fcm/delete/$token")
    val conn = serverUrl.openConnection() as HttpURLConnection

    Thread{
        try{
            conn.requestMethod = "DELETE"
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("Firebase", "FCM Token successfully deleted at server.")
            } else {
                Log.e("Firebase", "Failed to delete FCM Token. Response Code: $responseCode")
            }
            conn.disconnect()
        }catch (e: Exception){
            Log.e("Firebase", "Error deleting FCM Token at server", e)
        }
    }.start()
}