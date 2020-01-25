package realjenius.moongate

fun Byte.uByteToInt() = this.toUByte().toInt()

fun UByte.toChar() = this.toShort().toChar()
fun UInt.toChar() = this.toLong().toChar()


fun lsbFourByte(first: Int, second: Int, third: Int, fourth: Int): Int = first + (second shl 8) + (third shl 16) + (fourth shl 24)
fun lsbThreeByte(first: Int, second: Int, third: Int): Int = first + (second shl 8) + (third shl 16)
fun lsbTwoByte(first: Int, second: Int) = first + (second shl 8)

fun hexDump(bytes: ByteArray) = hexDumpThing(bytes.asSequence()) { it.toUByte().toInt() }

fun hexDumpChars(chars: CharSequence) = hexDumpThing(chars.asSequence()) { it.toInt() }

private fun <T> hexDumpThing(seq: Sequence<T>, mapper: (T) -> Int) : String {
  val output = StringBuilder().append("HEX DUMP----")
  var asciiBuffer = StringBuilder()
  seq.forEachIndexed { idx, it ->
    if(idx % 16 == 0) {
      output.append("| $asciiBuffer\n  ")
      asciiBuffer = StringBuilder()
      output.append(String.format("%1\$04d-%2\$04d :\t", idx, idx+15))
    }
    val asInt = mapper(it)
    output.append(asInt.toHex()).append(" ")
    asciiBuffer.append(if(asInt in (0x21..0x7e)) asInt.toChar() else '.')
  }
  return output.toString()
}

fun Int.toHex() : String {
  return String.format("%1\$02x", this)
}