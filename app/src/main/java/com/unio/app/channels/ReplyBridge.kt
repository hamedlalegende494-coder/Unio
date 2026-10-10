package com.unio.app.channels

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import com.unio.app.UnioApp
import com.unio.app.data.Channel
import com.unio.app.data.MessageEntity
import java.util.concurrent.ConcurrentHashMap

/** Envoie un message sur le bon canal. */
object ReplyBridge {
    private val actions = ConcurrentHashMap<String, Notification.Action>()

    private fun k(channel: Channel, key: String) = "${channel.name}:$key"

    fun store(channel: Channel, key: String, action: Notification.Action) {
        actions[k(channel, key)] = action
    }

    /** Retourne true si le message a été transmis. */
    suspend fun send(ctx: Context, channel: Channel, key: String, name: String, text: String): Boolean {
        val ok = if (channel == Channel.SMS) sendSms(ctx, key, text) else replyToNotification(ctx, channel, key, text)
        if (ok) {
            (ctx.applicationContext as UnioApp).db.dao().insert(
                MessageEntity(
                    contactKey = key, contactName = name, channel = channel.name,
                    body = text, fromMe = true, timestamp = System.currentTimeMillis(), read = true
                )
            )
        }
        return ok
    }

    private fun sendSms(ctx: Context, to: String, text: String): Boolean = try {
        val sms = ctx.getSystemService(SmsManager::class.java)
        sms.sendMultipartTextMessage(to, null, sms.divideMessage(text), null, null)
        true
    } catch (e: Exception) { false }

    private fun replyToNotification(ctx: Context, channel: Channel, key: String, text: String): Boolean {
        val action = actions[k(channel, key)] ?: return false
        val input = action.remoteInputs?.firstOrNull() ?: return false
        val intent = Intent()
        val bundle = Bundle().apply { putCharSequence(input.resultKey, text) }
        RemoteInput.addResultsToIntent(arrayOf(input), intent, bundle)
        return try {
            action.actionIntent.send(ctx, 0, intent); true
        } catch (e: PendingIntent.CanceledException) { false }
    }
}
