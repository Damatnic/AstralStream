package com.astralplayer.nextplayer.utils

import android.content.Context
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.di.DatabaseModule

val Context.database: AstralVuDatabase
    get() = DatabaseModule.getDatabase(this)