package realjenius.moongate.converse

import okio.BufferedSource
import realjenius.moongate.io.LZW
import realjenius.moongate.io.LibraryIO
import realjenius.moongate.io.LibraryType

object Conversations {
  private val conversations = hashMapOf<Int, Conversation>()

  fun load() {
    LibraryIO.read("CONVERSE.A", LibraryType.Lib32, ::readConversation)
    LibraryIO.read("CONVERSE.B", LibraryType.Lib32, ::readConversation)
  }

  private fun readConversation(buffer: BufferedSource, size: Long) {
    buffer.apply { require(size) }
        .let {  Conversation.parse(LZW.decompress(buffer)) }
        .apply { conversations[this.npcId] = this }
  }
}