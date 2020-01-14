package realjenius.moongate.converse

import realjenius.moongate.CharCursor

enum class AccumulatorType { Plain, Runic, Keyword, SingularSuffix, PluralSuffix }

sealed class ConversationText {
  companion object {
    private const val NL = 0x0a
    private const val TILDE = 0x7e
    private const val OPEN_BKT = 0x7b
    private const val SPACE = 0x20
    private val CONVERSE_CHARS = (SPACE..OPEN_BKT).plus(NL).plus(TILDE).toIntArray()

    fun isPrintable(char: Char) = CONVERSE_CHARS.contains(char.toInt())

    fun parseText(chars: CharCursor) : List<ConversationText> {
      val tokens: MutableList<ConversationText> = arrayListOf()
      val accumulator = SectionAccumulator(AccumulatorType.Plain, ::PlainSegment)

      while(chars.hasNext() && isPrintable(chars.peekChar())) {
        val it = chars.nextChar()
        var nextToken: ConversationText? = null
        when {
          it == '$' || it == '#' -> {
            val nextIt = chars.peekChar()
            val sentinel = "$it$nextIt"
            when {
              sentinel == "\$P" -> nextToken = PlayerNameSegment
              sentinel == "\$G" -> nextToken = GenderTitleSegment
              sentinel == "\$N" -> nextToken = NpcNameSegment
              sentinel == "\$T" -> nextToken = TimeOfDaySegment
              sentinel == "\$Y" -> nextToken = SelectedNameSegment
              sentinel == "\$Z" -> nextToken = LastInputSegment
              it == '$' && nextIt.isDigit() -> nextToken = StringVarSegment(Character.getNumericValue(nextIt))
              it == '#' && nextIt.isDigit() -> nextToken = VarSegment(Character.getNumericValue(nextIt))
              else -> accumulator.append(it)
            }
            if(nextToken != null) chars.nextChar()
          }
          it == '/' -> {
            nextToken = accumulator.transition(AccumulatorType.SingularSuffix, ::SingularSuffix)
          }
          it == '\\' -> {
            nextToken = accumulator.transition(AccumulatorType.PluralSuffix, ::PluralSuffix)
          }
          it == '<' -> {
            nextToken = accumulator.transition(AccumulatorType.Runic, ::RunicSegment)
          }
          it == '>' && accumulator.type == AccumulatorType.Runic -> {
            nextToken = accumulator.transition(AccumulatorType.Plain, ::PlainSegment)
          }
          it == '*' -> nextToken = PageBreakToken
          it == '@' -> {
            nextToken = accumulator.transition(AccumulatorType.Keyword, ::KeywordSegment)
          }
          !it.isLetterOrDigit() && accumulator.isInType(AccumulatorType.Keyword, AccumulatorType.PluralSuffix, AccumulatorType.SingularSuffix) -> {
            nextToken = accumulator.transition(AccumulatorType.Plain, ::PlainSegment)
          }
          it == '\n' -> nextToken = NewLineToken
          else -> accumulator.append(it)
        }
        if(nextToken != null) {
          accumulator.flush()?.let { tokens.add(it) }
          tokens.add(nextToken)
        }
      }
      accumulator.flush()?.let { tokens.add(it) }
      return tokens
    }
  }
}

data class SectionAccumulator(var type: AccumulatorType, private var factory: (String) -> ConversationText) {
  private var text: StringBuilder = StringBuilder()
  fun append(char: Char) {
    text.append(char)
  }

  fun isInType(vararg types: AccumulatorType) = types.contains(this.type)

  fun transition(type: AccumulatorType, factory: (String) -> ConversationText) : ConversationText? {
    val result = flush()
    this.type = type
    this.factory = factory
    this.text = StringBuilder()
    return result
  }

  fun flush() = if(text.isNotEmpty()){
    val result = factory.invoke(text.toString())
    text = StringBuilder()
    result
  } else null
}

data class PlainSegment(val text: String) : ConversationText() {
  override fun toString() = text
}

data class KeywordSegment(val keyword: String) : ConversationText() {
  override fun toString() = "@$keyword"
}

object PlayerNameSegment : ConversationText() {
  override fun toString() = "PlayerName"
}

object NpcNameSegment : ConversationText() {
  override fun toString() = "NpcName"
}

data class StringVarSegment(val index: Int) : ConversationText()

data class VarSegment(val index: Int) : ConversationText()

data class SingularSuffix(val suffix: String) : ConversationText()

data class PluralSuffix(val suffix: String) : ConversationText()

object SelectedNameSegment : ConversationText() {
  override fun toString() = "SelectedName"
}

object TimeOfDaySegment : ConversationText() {
  override fun toString() = "TimeOfDay"
}

object LastInputSegment : ConversationText() {
  override fun toString() = "LastInput"
}

object GenderTitleSegment : ConversationText() {
  override fun toString() = "GenderTitle"
}
data class RunicSegment(val text: String) : ConversationText()

object NewLineToken : ConversationText() {
  override fun toString() = "NewLine"
}

object PageBreakToken : ConversationText() {
  override fun toString() = "PageBreak"
}