package realjenius.moongate.gamedata.palette

import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.readUByte
import java.io.File

object Palettes {
  private var gamePaletteRotations: MutableList<Palette> = arrayListOf()
  lateinit var cutscenePalettes: List<Palette>
  private var currentRotation = 0

  fun forRotation(index: Int) = gamePaletteRotations[index]

  fun load() {
    cutscenePalettes = Palette.load(GameFiles.loadExternal("PALETTES.INT"), 6)
    gamePaletteRotations.add(Palette.load(GameFiles.loadExternal("U6PAL"), 1)[0])
    (1 until rotationCount()).forEach {
      val palette = gamePaletteRotations.last()
      gamePaletteRotations.add(palette.rotate(it))
    }
  }

  fun update() {
    currentRotation++
    if(currentRotation >= rotationCount()) currentRotation = 0
  }

  fun currentPalette() = currentRotation

  fun rotationCount() = 8
}
