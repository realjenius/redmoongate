package realjenius.moongate.gamedata.palette

import okio.BufferedSource
import okio.buffer
import okio.source
import realjenius.moongate.io.readUByte
import java.io.File

data class Palette(val values: List<Rgb>) {

  fun rotate(index: Int) = Palette(ArrayList(this.values).apply {
    everyCycleRotated.forEach { rotateColor(this, it, 8) }
    if (index % 2 == 1)
      everyOtherCycleRotated.forEach { rotateColor(this, it, 4) }
  })

  private fun rotateColor(colors: MutableList<Rgb>, position: Int, length: Int) {
    val last = colors[position+length-1]
    for (it in length-1 downTo 1) {
      colors[position + it] = colors[position + (it -1)]
    }
    colors[position] = last
  }

  companion object {
    private const val PALETTE_LEN = 256
    private val everyCycleRotated = intArrayOf(0xe0, 0xe8)
    private val everyOtherCycleRotated = intArrayOf(0xf0, 0xf4, 0xf8)
    private val rotatedColors = everyCycleRotated + everyOtherCycleRotated

    fun isRotatedColor(color: Int) = color in rotatedColors

    fun load(file: File, count: Int) =
        file.source().buffer().use { source ->
          (0 until count).map { load(source) }
        }

    private fun load(buffer: BufferedSource) = Palette(
        (0 until PALETTE_LEN).map {
          Rgb(
              translateColor(buffer.readUByte()),
              translateColor(buffer.readUByte()),
              translateColor(buffer.readUByte())
          )
        }.toList()
    )

    // The left shift magnifies the color to match modern ranges. U6 stores these as 0-63, not 0-255
    private fun translateColor(byte: UByte) = (byte.toUInt() shl 2).toFloat() / 255
  }
}