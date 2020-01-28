package realjenius.moongate.gamedata.tile

data class TileDescription(private val segments: List<TextSegment>) {
  val isStackable: Boolean by lazy { segments.any { it.supportsCount() } }
  fun forCount(count: Int) = segments.joinToString("") { it.forCount(count) }

  companion object {
    fun parse(input: String) : TileDescription {
      val segments = arrayListOf<TextSegment>()
      var segmentStart = 0
      var parseState = 0
      var singularPart = ""
      input.forEachIndexed { idx, char ->
        when {
          idx == input.length-1 -> {
            if (parseState == 0)  segments.add(FixedSegment(input.substring(segmentStart, idx + 1)))
            else segments.add(PluralChoice(singularPart, input.substring(segmentStart, idx + 1)))
          }
          parseState == 0 && char == '/' -> {
            parseState = 1
            if (segmentStart < idx) segments.add(FixedSegment(input.substring(segmentStart, idx)))
            segmentStart = idx+1
          }
          char == '\\' -> {
            if (segmentStart < idx) {
              val text = input.substring(segmentStart, idx)
              if (parseState == 1) singularPart = text
              else segments.add(FixedSegment(text))
            }
            parseState = 2
            segmentStart = idx + 1
          }
          parseState == 2 && char.isWhitespace() -> {
            parseState = 0
            if (segmentStart < idx) segments.add(PluralChoice(singularPart, input.substring(segmentStart, idx)))
            segmentStart = idx
            singularPart = ""
          }
        }
      }
      return TileDescription(segments)
    }
  }

  interface TextSegment {
    fun supportsCount() : Boolean
    fun forCount(count: Int) : String
  }
  data class FixedSegment(private val text: String) : TextSegment {
    override fun supportsCount() = false
    override fun forCount(count: Int) = text
  }

  data class PluralChoice(private val singular: String, private val plural: String) : TextSegment {
    override fun supportsCount() = true
    override fun forCount(count: Int) = if (count == 1) singular else plural
  }
}
