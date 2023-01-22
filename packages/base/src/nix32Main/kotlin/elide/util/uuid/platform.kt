package elide.util.uuid

import platform.posix.read

internal actual fun getRandomUuidBytes(): ByteArray {
  return bytesWithURandomFd { fd, bytePtr ->
    read(fd, bytePtr, UUID_BYTES.toUInt())
  }
}
