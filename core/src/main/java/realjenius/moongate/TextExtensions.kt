package realjenius.moongate
const val NL = 0x0a
const val TILDE = 0x7e
const val OPEN_BKT = 0x7b
const val SPACE = 0x20


class CharCursor(private val chars: CharSequence, private var index: Int = -1) : CharIterator() {
  val length : Int get() = chars.length

  override fun hasNext() = hasNextN(1)
  fun hasNextN(count: Int) = (chars.length-count) >= index+1
  override fun nextChar() = chars[++index]
  fun nextInt() = nextChar().toInt()
  fun nextInt2() = lsbTwoByte(nextInt(), nextInt())
  fun nextInt4() = lsbFourByte(nextInt(), nextInt(), nextInt(), nextInt())
  fun peekChar(depth: Int = 1) = chars[index+depth]
  fun peekInt(depth: Int = 1) = peekChar(depth).toInt()
  fun index() = index
  fun rewind(count: Int = 1) { index -= count }
  fun seek(pos: Int) { index = pos }
}

fun CharSequence.cursor() = CharCursor(this)
