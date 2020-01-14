package realjenius.moongate.io

import okio.BufferedSource

enum class LibraryType(val hasSize: Boolean, val indexSize: Int) {
  SLib16(true, 2),
  SLib32(true, 4),
  Lib16(false, 2),
  Lib32(false, 4);

  fun readIndex(buffer: BufferedSource) : Long =
      if (indexSize == 2) buffer.readShortLe().toLong() else buffer.readIntLe().toLong()
}