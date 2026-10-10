package com.unio.app

import android.app.Application
import com.unio.app.data.UnioDb

class UnioApp : Application() {
    val db: UnioDb by lazy { UnioDb.create(this) }
}
