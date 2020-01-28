package realjenius.moongate.io

import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source

object LibraryIO {
  fun <T> read(path: String, libType: LibraryType, blockHandler: (buffer: Buffer, size: Long) -> T) : List<T> {
    val (size,buffer) = loadLibrary(path)
    var dataStart = 0L
    val indexes = arrayListOf<BlockIndex>()
    var processed = 0L
    while (dataStart == 0L || processed < dataStart) {
      val index = libType.readIndex(buffer)
      processed += libType.indexSize
      if (index <= 0) continue
      if (dataStart == 0L) dataStart = index
      val blockIndex = BlockIndex(index)
      if (indexes.size > 0) indexes.last().markEnd(index)
      indexes += blockIndex
    }
    indexes.last().markEnd(size)
    return indexes.map {
      val toSkip = (it.offset - processed)
      if (toSkip > 0) {
        buffer.skip(toSkip)
        processed += toSkip
      }
      buffer.require(it.length)
      processed += it.length
      // TODO - eliminate buffer copy.
      blockHandler(Buffer().write(buffer.readByteArray(it.length)), it.length)
    }
  }

  private fun loadLibrary(path: String) = GameFiles.loadExternal(path).let { it.length() to it.source().buffer() }
}

private data class BlockIndex(val offset: Long, var length: Long = -1) {
  fun markEnd(end: Long) { length = (end - offset) }
}