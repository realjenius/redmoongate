package realjenius.moongate.screen

import okio.Buffer
import okio.buffer
import okio.source
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.readUByte
import java.io.File

object Palettes {
  lateinit var gamePalette: Palette
  lateinit var cutscenePalettes: List<Palette>

  fun load() {
    gamePalette = Palette.load(GameFiles.loadExternal("U6PAL"), 1)[0]
    cutscenePalettes = Palette.load(GameFiles.loadExternal("PALETTES.INT"), 6)
  }
}

data class Palette(val values: List<Rgb>) {
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