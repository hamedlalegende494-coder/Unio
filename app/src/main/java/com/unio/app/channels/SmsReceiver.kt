package com.unio.app.channels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.unio.app.UnioApp
import com.unio.app.data.Channel
import com.unio.app.data.MessageEntity
import kotlinx.coroutines.*

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val from = parts.firstOrNull()?.originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val pending = goAsync()
        val dao = (context.applicationContext as UnioApp).db.dao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.insert(
                    MessageEntity(
                        contactKey = from, contactName = from, channel = Channel.SMS.name,
                        body = body, fromMe = false, timestamp = System.currentTimeMillis(), read = false
                    )
                )
            } finally { pending.finish() }
        }
    }
}
