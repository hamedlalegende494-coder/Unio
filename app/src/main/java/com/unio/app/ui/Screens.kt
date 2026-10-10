package com.unio.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unio.app.UnioApp
import com.unio.app.channels.ReplyBridge
import com.unio.app.data.Channel
import kotlinx.coroutines.launch

@Composable
fun UnioRoot(app: UnioApp, askSms: () -> Unit, openNotifAccess: () -> Unit) {
    var open by remember { mutableStateOf<String?>(null) }
    BackHandler(open != null) { open = null }
    val key = open
    if (key == null) InboxScreen(app, askSms, openNotifAccess) { open = it }
    else ChatScreen(app, key)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(app: UnioApp, askSms: () -> Unit, openNotifAccess: () -> Unit, onOpen: (String) -> Unit) {
    val dao = app.db.dao()
    val threads by remember(dao) { dao.threads() }.collectAsState(emptyList())
    val unread by remember(dao) { dao.unread() }.collectAsState(emptyList())
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Channel?>(null) }

    val shown = threads.filter {
        (filter == null || it.channel == filter!!.name) &&
            (it.contactName.contains(query, true) || it.body.contains(query, true))
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Messages", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp, 12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Rechercher un contact ou un message") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Row(Modifier.padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter == null, { filter = null }, { Text("Tous") })
            Channel.values().forEach { c -> FilterChip(filter == c, { filter = c }, { Text(c.label) }) }
        }
        LazyColumn(Modifier.weight(1f)) {
            if (threads.isEmpty()) item {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Aucun message pour le moment. Autorisez les SMS et l'accès aux notifications pour commencer.")
                    Button(onClick = askSms) { Text("Autoriser les SMS") }
                    OutlinedButton(onClick = openNotifAccess) { Text("Activer l'accès aux notifications") }
                }
            }
            items(shown, key = { it.contactKey }) { m ->
                val n = unread.firstOrNull { it.contactKey == m.contactKey }?.n ?: 0
                ListItem(
                    headlineContent = { Text(m.contactName) },
                    supportingContent = {
                        Text((if (m.fromMe) "Vous : " else "") + m.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    overlineContent = { Text(Channel.valueOf(m.channel).label) },
                    trailingContent = { if (n > 0) Badge { Text("$n") } },
                    modifier = Modifier.clickable { onOpen(m.contactKey) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(app: UnioApp, key: String) {
    val dao = app.db.dao()
    val msgs by remember(key) { dao.thread(key) }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Channel?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key, msgs.size) {
        dao.markRead(key)
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    val channels = msgs.map { Channel.valueOf(it.channel) }.distinct()
    val channel = chosen ?: channels.lastOrNull() ?: Channel.SMS
    val name = msgs.lastOrNull()?.contactName ?: key

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { c -> FilterChip(channel == c, { chosen = c }, { Text(c.label) }) }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 12.dp), state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(msgs, key = { it.id }) { m ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (m.fromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) { Text(m.body, Modifier.padding(12.dp, 8.dp)) }
                }
            }
        }
        info?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        Row(
            Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Votre message") })
            Button(onClick = {
                val t = text.trim(); if (t.isEmpty()) return@Button
                scope.launch {
                    val ok = ReplyBridge.send(app, channel, key, name, t)
                    if (ok) { text = ""; info = null }
                    else info = if (channel == Channel.SMS) "Envoi impossible. Vérifiez l'autorisation SMS."
                    else "Réponse impossible : attendez un nouveau message de ${channel.label} pour répondre d'ici."
                }
            }) { Text("Envoyer") }
        }
    }
}
