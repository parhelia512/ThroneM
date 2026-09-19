package io.throneproj.thronem.group

import androidx.core.net.toUri
import io.throneproj.thronem.repository.resolveAndroidRepository

actual fun readContentUri(uri: String): String? {
    return resolveAndroidRepository().context.contentResolver.openInputStream(uri.toUri())
        ?.bufferedReader()
        ?.readText()
}
