package realjenius.moongate.gamedata.tile

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import realjenius.moongate.gamedata.palette.Palette
import realjenius.moongate.gamedata.palette.Palettes
import java.util.ArrayList


class Tile(val number: Int, var data: ByteArray) {
  var flags = TileFlag.emptySet()
  var transparent = false
  lateinit var description: TileDescription
  val sprites: MutableList<TextureRegion> = ArrayList(Palettes.rotationCount())
  lateinit var articleType: ArticleType

  fun isStackable() = description.isStackable

  fun isDoubleWidth() = flags.contains(TileFlag.DoubleWidth)
  fun isDoubleHeight() = flags.contains(TileFlag.DoubleHeight)
  fun isTopTile() = flags.contains(TileFlag.TopTile)
  fun isBottomTile() = flags.contains(TileFlag.BottomTile)

  fun clearPixels(offset: Int, count: Int) {
    data.fill(Tiles.TRANSPARENT, offset, offset+count)
  }

  fun containsRotatedColors() = data.any { Palette.isRotatedColor(it.toUByte().toInt()) }

  fun generateTexture(xOffset: Int, yOffset: Int, palette: Palette, pixmap: Pixmap) {
    assert(data.isNotEmpty())

    for (row in 0 until 16) {
      for (col in 0 until 16) {
        val value = data[row + col * 16]
        val color =
            if (this.transparent && value == Tiles.TRANSPARENT)
              Color.rgba8888(1.toFloat(), 1.toFloat(), 1.toFloat(), 0.toFloat())
            else palette.values[value.toUByte().toInt()].let {
              Color.rgba8888(it.red, it.green, it.blue, 1.toFloat())
            }
        pixmap.drawPixel(xOffset + row, yOffset + col, color)
      }
    }
  }
  fun bindRegion(texture: Texture, paletteIndex: Int, xOffset: Int, yOffset: Int) {
    this.sprites.add(TextureRegion(texture, xOffset, yOffset, 16, 16))
    this.data = byteArrayOf()
  }

  fun spriteForPalette(palette: Int) = if(this.sprites.size <= palette) this.sprites[0] else this.sprites[palette]
}