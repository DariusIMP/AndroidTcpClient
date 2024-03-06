package com.example.tcpclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean


class TcpSubService : Service() {

    companion object {
        private val TAG = TcpSubService::javaClass.name
        private const val PORT = 9876
        private const val NANOS_TO_SEC = 1_000_000_000L
    }

    private val n = 50000L
    private var batchCount = 0
    private var count = 0
    private var startTimestampNs: Long = 0
    private var globalStartTimestampNs: Long = 0

    private fun listener() {
        if (count == 0) {
            startTimestampNs = System.nanoTime()
            if (globalStartTimestampNs == 0L) {
                globalStartTimestampNs = startTimestampNs
            }
            count++
            return
        }
        if (count < n) {
            count++
            return
        }
        val stop = System.nanoTime()
        val msgs = n * NANOS_TO_SEC / (stop - startTimestampNs)
        Log.i(TAG, "$msgs msgs/sec")
        batchCount++
        count = 0
    }

    private val runnable = Runnable {
        val socket = Socket("localhost", PORT)
        val dataInputStream = DataInputStream(socket.getInputStream())
        val buff = ByteArray(8)
        try {
            Log.i(TAG, "Established connection. Will receive stuff now...")
            while (dataInputStream.read(buff) > 0) {
                listener()
            }
        } catch (e: SocketException) {
            Log.i(TAG, "Connection closed...")
        }
    }

    override fun onCreate() {
        startMeForeground()
        Thread(runnable).start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startMeForeground() {
        val NOTIFICATION_CHANNEL_ID = packageName
        val channelName = "Tcp Server Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tcp Client is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }
}