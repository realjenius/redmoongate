package realjenius.moongate.gamedata.palette

import okio.Buffer
import okio.buffer
import okio.source
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.readUByte
import java.io.File

object Palettes {
  private val rotatedColors = intArrayOf(0xe0, 0xe8, 0xf0, 0xf4, 0xf8)
  private var gamePaletteRotations: MutableList<Palette> = arrayListOf()
  lateinit var cutscenePalettes: List<Palette>
  private var currentRotation = 0

  fun isRotatedColor(color: Int) = color in rotatedColors

  fun forRotation(index: Int) = gamePaletteRotations[index]

  fun load() {
    cutscenePalettes = Palette.load(GameFiles.loadExternal("PALETTES.INT"), 6)
    gamePaletteRotations.add(Palette.load(GameFiles.loadExternal("U6PAL"), 1)[0])
    (1 until rotationCount()).forEach {
      val palette = gamePaletteRotations.last()
      gamePaletteRotations.add(palette.rotate(it))
    }
    gamePaletteRotations.forEachIndexed { index, it ->
      println("Values @ $index: ${it.values.subList(0xe0, 0xe8)}")
    }
  }

  fun update() {
    currentRotation++
    if(currentRotation >= rotationCount()) currentRotation = 0
  }

  fun currentPalette() = currentRotation

  fun rotationCount() = 8
}

data class Palette(val values: List<Rgb>) {

  fun rotate(index: Int) : Palette {
    val valCopy = ArrayList(this.values)
    rotateColor(valCopy, 0xe0, 8)
    rotateColor(valCopy, 0xe8, 8)
    if (index % 2 == 1) {
      rotateColor(valCopy, 0xf0, 4)
      rotateColor(valCopy, 0xf4, 4)
      rotateColor(valCopy, 0xf8, 4)
    }
    return Palette(valCopy)
  }

  private fun rotateColor(colors: MutableList<Rgb>, position: Int, length: Int) {
    println("Rotating colors from $position to ${position + length}")
    val last = colors[position+length-1]
    (1 until length).reversed().forEach {
      println("Rotating ${colors[position+it-1]} to $it")
      colors[position + it] = colors[position + (it -1)]
    }
    colors[position] = last
  }

  companion object {

    fun load(file: File, count: Int) : List<Palette> {
      return file.source().buffer().use { source ->
        (0 until count).map {
          source.require(256)
          load(source.buffer)
        }
      }
    }

    private fun load(buffer: Buffer) = Palette(
        (0 until 256).map {
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

data class Rgb(val red: Float, val green: Float, val blue: Float)