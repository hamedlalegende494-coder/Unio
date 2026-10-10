package com.unio.app.channels

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.unio.app.UnioApp
import com.unio.app.data.Channel
import com.unio.app.data.MessageEntity
import kotlinx.coroutines.*

/** Lit les notifications WhatsApp / Messenger / Facebook et les range dans la boîte unifiée. */
class NotificationCaptureService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sources = mapOf(
        "com.whatsapp" to Channel.WHATSAPP,
        "com.whatsapp.w4b" to Channel.WHATSAPP,
        "com.facebook.orca" to Channel.MESSENGER,
        "com.facebook.katana" to Channel.FACEBOOK
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val channel = sources[sbn.packageName] ?: return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val key = title.lowercase()

        n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
            ?.let { ReplyBridge.store(channel, key, it) }

        val dao = (applicationContext as UnioApp).db.dao()
        scope.launch {
            val now = System.currentTimeMillis()
            if (dao.recentDuplicates(key, text, now - 60_000) > 0) return@launch
            dao.insert(
                MessageEntity(
                    contactKey = key, contactName = title, channel = channel.name,
                    body = text, fromMe = false, timestamp = now, read = false
                )
            )
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
