package realjenius.moongate.converse

import realjenius.moongate.io.LZW
import realjenius.moongate.io.LibraryIO
import realjenius.moongate.io.LibraryType

object Conversations {
  private val conversations = hashMapOf<Int, Conversation>()

  fun load() {
    LibraryIO.read("CONVERSE.A", LibraryType.Lib32) { buffer, size ->
      buffer.require(size)
      Conversation.parse(LZW.decompress(buffer)).apply { conversations[this.npcId] = this }
    }
    LibraryIO.read("CONVERSE.B", LibraryType.Lib32) { buffer, size ->
      buffer.require(size)
      Conversation.parse(LZW.decompress(buffer)).apply { conversations[this.npcId] = this }
    }
  }
}