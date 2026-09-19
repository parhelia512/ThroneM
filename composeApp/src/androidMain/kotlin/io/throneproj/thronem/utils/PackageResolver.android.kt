package io.throneproj.thronem.utils

actual object PackageResolver {
    actual suspend fun awaitLoad() = PackageCache.awaitLoad()
    actual fun awaitLoadSync() = PackageCache.awaitLoadSync()
    actual fun findUidForPackage(packageName: String) = PackageCache[packageName]
    actual fun findPackagesForUid(uid: Int): Set<String>? = PackageCache[uid]
    actual fun isAppInstalled(packageName: String) = packageName in PackageCache.installedApps
    actual fun loadAppLabel(packageName: String): String? {
        val info = PackageCache.installedApps[packageName] ?: return null
        return info.loadLabel(PackageCache.packageManager).toString()
    }

    actual fun loadAppIcon(packageName: String): Any? {
        val info = PackageCache.installedApps[packageName] ?: return null
        return info.loadIcon(PackageCache.packageManager)
    }
}
