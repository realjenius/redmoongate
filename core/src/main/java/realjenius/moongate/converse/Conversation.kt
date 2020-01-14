package realjenius.moongate.converse

import okio.Buffer
import okio.ByteString
import realjenius.moongate.CodedError
import realjenius.moongate.cursor
import realjenius.moongate.toChar

data class Conversation(val npcId: Int, val npcName: String, val conversation: CharSequence) {

  fun start() = ActiveConversation(conversation.cursor())

  companion object {
    private const val IDENTITY = 0xff
    private const val SLOOK = 0xf1
    const val SPREFIX = 0xf3

    fun parse(bytes: ByteArray) : Conversation {
      // TODO - move conversations and cursor onto bytestring...
      // That would eliminate a lot of copying and weird coersion.
      val raw = bytes.fold(StringBuilder()) { builder, byte -> builder.append(byte.toUByte().toChar()) }
      val cursor = raw.cursor()
      cursor.nextInt().apply {
        if (this != IDENTITY)
          CodedError.InvalidConversationFormat.fail("Conversation does not start with identity: $this")
      }
      val npcId = cursor.nextInt()
      val name = StringBuilder()
      while (cursor.peekInt() != SLOOK && cursor.peekInt() != SPREFIX) name.append(cursor.nextChar())
      while (cursor.peekInt() == SLOOK || cursor.peekInt() == SPREFIX) cursor.nextInt()
      return Conversation(npcId, name.toString(), raw.subSequence(cursor.index()+1, raw.length))
    }
  }
}