package realjenius.moongate.converse

import realjenius.moongate.CharCursor
import realjenius.moongate.toHex

class ActiveConversation(private val cursor: CharCursor) {
  private var waitingInput = false
  private var inputType: Int = 0

  fun step() {
    while (!waitingInput) {
      when {
        ConversationText.isPrintable(cursor.peekChar()) -> {
          val text = ConversationText.parseText(cursor)
          println("Text: $text")
        }
        ConvoStatementType.isStatement(cursor.peekInt()) -> {
          val stmt = ConvoStatementType.load(cursor.nextInt(), cursor)
          println("Statement to run: $stmt - oxd2 == ${0xd2}")
        }
        // TODO - this is not what I would call a good plan...
        cursor.peekInt() == SCONVERSE -> cursor.nextInt()
        cursor.peekInt() == Conversation.SPREFIX -> cursor.nextInt()
        cursor.peekInt() == EVAL -> { cursor.nextInt(); println("EVAL marker...") }
        cursor.peekInt() == WAIT -> { cursor.nextInt(); println("Wait encountered. Pretending to trigger now") }
        cursor.peekInt() == ASK -> { cursor.nextInt(); println("You say: ") }
        cursor.peekInt() == KEYWORDS -> { waitingInput = true }
        else -> {
          println("Next: ${cursor.peekInt().toHex()}")
          waitingInput = true
        }
      }
    }
  }

  fun input(input: String) {

  }

  companion object {
    private const val SCONVERSE = 0xf2
    private const val EVAL = 0xa7
    private const val WAIT = 0xcb
    private const val ASK = 0xf7
    private const val KEYWORDS = 0xef
  }
}
