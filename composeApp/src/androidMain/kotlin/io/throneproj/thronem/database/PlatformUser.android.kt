package io.throneproj.thronem.database

import android.os.Binder

actual fun callingUserIndex(): Int = Binder.getCallingUserHandle().hashCode()
