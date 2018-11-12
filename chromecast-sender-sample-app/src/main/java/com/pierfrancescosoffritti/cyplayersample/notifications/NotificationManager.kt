package com.pierfrancescosoffritti.cyplayersample.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v7.graphics.Palette
import android.util.Log
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.cyplayersample.R
import com.pierfrancescosoffritti.cyplayersample.utils.YouTubeDataEndpoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class NotificationManager(private val context: Context, private val notificationHostActivity: Class<*>) : AbstractYouTubePlayerListener() {
    private val notificationId = 1
    private val channelId = "CHANNEL_ID"

    private val notificationBuilder: NotificationCompat.Builder

    init {
        initNotificationChannel()
        notificationBuilder = initNotificationBuilder()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "chromecast-youtube-player", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "sample-app"
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initNotificationBuilder() : NotificationCompat.Builder {
        val openActivityExplicitIntent = Intent(context.applicationContext, notificationHostActivity)
        val openActivityPendingIntent = PendingIntent.getActivity(context.applicationContext, 0, openActivityExplicitIntent, 0)

        val togglePlaybackImplicitIntent = Intent(PlaybackControllerBroadcastReceiver.TOGGLE_PLAYBACK)
        val togglePlaybackPendingIntent = PendingIntent.getBroadcast(context, 0, togglePlaybackImplicitIntent, 0)

        val stopCastSessionImplicitIntent = Intent(PlaybackControllerBroadcastReceiver.STOP_CAST_SESSION)
        val stopCastSessionPendingIntent = PendingIntent.getBroadcast(context, 0, stopCastSessionImplicitIntent, 0)

        return NotificationCompat.Builder(context, channelId)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_cast_connected_24dp)
                .setContentIntent(openActivityPendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_play_arrow_24dp, "Toggle Playback", togglePlaybackPendingIntent)
                .addAction(R.drawable.ic_cast_connected_24dp, "Disconnect from chromecast", stopCastSessionPendingIntent)
                .setStyle(MediaStyle().setShowActionsInCompactView(0, 1))
    }

    fun showNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun dismissNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    @SuppressLint("SwitchIntDef")
    override fun onStateChange(state: PlayerConstants.PlayerState) {
        when(state) {
            PlayerConstants.PlayerState.PLAYING -> notificationBuilder.mActions[0].icon = R.drawable.ic_pause_24dp
            else -> notificationBuilder.mActions[0].icon = R.drawable.ic_play_arrow_24dp
        }

        showNotification()
    }

    override fun onVideoId(videoId: String) {
        val observable = YouTubeDataEndpoint.getVideoInfoFromYouTubeDataAPIs(videoId)

        observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            notificationBuilder.setContentTitle(it.videoTitle)
                            notificationBuilder.setContentText(it.channelTitle)
                            notificationBuilder.setLargeIcon(it?.thumbnail)

                            val color = Palette.from(it?.thumbnail!!).generate().getDominantColor(ContextCompat.getColor(context, android.R.color.black))
                            notificationBuilder.color = color

                            showNotification()
                        },
                        { Log.e(javaClass.simpleName, "Can't retrieve video title, are you connected to the internet?") }
                )
    }
}