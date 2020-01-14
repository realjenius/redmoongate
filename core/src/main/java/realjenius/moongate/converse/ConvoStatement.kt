package realjenius.moongate.converse

import realjenius.moongate.CharCursor
import realjenius.moongate.lsbFourByte
import realjenius.moongate.lsbTwoByte

private const val CONVO_DATA_SIZE_4 = 0xd2
private const val CONVO_DATA_SIZE_1 = 0xd3
private const val CONVO_DATA_SIZE_2 = 0xd4

enum class ConvoStatementType(val code: Int, private val factory: ()->ConvoStatement) {
  SetFlag(0xa4, { SetFlag() }),
  ClearFlag(0xa5, { ClearFlag() });

  companion object {

    fun isStatement(code: Int) = find(code) != null

    private fun find(code: Int) = values().firstOrNull { it.code == code }

    fun load(code: Int, chars: CharCursor) : ConvoStatement {
      val type = find(code) ?: throw NullPointerException("$code is not a valid statement identifier")
      return type.factory().apply { parse(chars) }
    }
  }
}

interface ConvoStatement {
  fun parse(chars: CharCursor) {

  }
  private tailrec fun doParse(chars: CharCursor, vars: MutableList<Int>) {
    if (!chars.hasNext() || ConvoStatementType.isStatement(chars.peekInt())) return
    if (ConversationText.isPrintable(chars.peekChar()) && (!chars.hasNextN(2) || !isValOp(chars.peekInt(2)))) return
    var sized = true
    val data = when (val dataSize = chars.nextInt()) {
      CONVO_DATA_SIZE_1 -> chars.nextInt()
      CONVO_DATA_SIZE_2 -> chars.nextInt2()
      CONVO_DATA_SIZE_4 -> chars.nextInt4()
      else -> { sized = false; dataSize }
    }


  }

  private fun isValOp(int: Int) = false
  companion object {

  }
}

data class Jump(private var jumpLocation: Int = 0) : ConvoStatement {
  override fun parse(chars: CharCursor) {
    jumpLocation = chars.nextInt4()
  }
}

data class SetFlag(private var npcId: Int = 0, private var flagId: Int = 0) : ConvoStatement

class ClearFlag() : ConvoStatement