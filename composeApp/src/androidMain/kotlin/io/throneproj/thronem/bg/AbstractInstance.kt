package io.throneproj.thronem.bg

import java.io.Closeable

interface AbstractInstance : Closeable {

    fun launch()

}