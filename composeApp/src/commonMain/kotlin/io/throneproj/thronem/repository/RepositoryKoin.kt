package io.throneproj.thronem.repository

import org.koin.core.context.GlobalContext

fun resolveRepository(): Repository = GlobalContext.get().get()
