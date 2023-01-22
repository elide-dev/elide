package elide.util.uuid

internal expect fun getRandomUuidBytes(): ByteArray

internal expect fun <T> T.freeze(): T
