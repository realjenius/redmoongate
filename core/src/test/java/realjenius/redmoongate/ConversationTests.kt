package realjenius.redmoongate

import okio.Buffer
import org.junit.Before
import org.junit.Test
import realjenius.moongate.Env
import realjenius.moongate.converse.Conversation
import realjenius.moongate.io.LZW
import realjenius.moongate.io.LibraryIO
import realjenius.moongate.io.LibraryType

class ConversationTests {
  private var convos: MutableMap<Int, Conversation> = hashMapOf()

  @Before
  fun setup() {
    Env.testMode = true
    Env["STATICDATA"] = "/home/realjenius/Games/u6installer"
    LibraryIO.read("CONVERSE.A", LibraryType.Lib32, ::loadConvo)
    LibraryIO.read("CONVERSE.B", LibraryType.Lib32, ::loadConvo)
  }

  private fun loadConvo(buffer: Buffer, size: Long) {
    buffer.require(size)
    val conversation = Conversation.parse(LZW.decompress(buffer))
    convos[conversation.npcId] = conversation
  }

  @Test
  fun shaminoTest() {
    convos[2]?.start()
  }
}