package com.autoever.chajava

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService


class MyFirebaseMessagingService : FirebaseMessagingService() {
    //새 토큰 가져오기
    override fun onNewToken(token: String) {
        Log.d("Firebase", "Refreshed token: $token")

        sendRegistrationToServer(token)
    }
}