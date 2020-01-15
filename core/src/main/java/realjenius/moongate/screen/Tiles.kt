package realjenius.moongate.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import okio.Buffer
import okio.buffer
import okio.source
import realjenius.moongate.io.ByteView
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.LZW
import realjenius.moongate.screen.Tiles.TRANSPARENT
import java.util.*

object Tiles {
  const val TRANSPARENT = 0xff.toByte() // move somewhere more appropriate.
  private const val TILE_COUNT = 2048
  private const val ARTICLE_SECTION_SKIP = (5120 - (TILE_COUNT *2)).toLong()
  private val TRIGGERED_ANIM = setOf(862, 1009, 1020) // crank, crank, chain

  // Animated tiles are from 16-48, we use this to find the base animation tile to file the base layer
  private val ANIMATION_SRC_TILES = listOf(
      0x16,0x16,0x1a,0x1a,0x1e,0x1e,0x12,0x12,0x1a,0x1e,0x16,0x12,0x16,0x1a,0x1e,0x12,
      0x1a,0x1e,0x1e,0x12,0x12,0x16,0x16,0x1a,0x12,0x16,0x1e,0x1a,0x1a,0x1e,0x12,0x16
  )
  private val ANIMATED_TILE_COUNT = ANIMATION_SRC_TILES.size
  private const val ANIMATED_TILE_OFFSET = 16

  lateinit var tiles: List<Tile>
  lateinit var masterTexture: Texture
  lateinit var animationData: AnimationInfo

  /*

  static const uint16 U6_ANIM_SRC_TILE[32] = {};
  Tile *TileManager::get_anim_base_tile(uint16 tile_num)
{
 return &tile[tileindex[U6_ANIM_SRC_TILE[tile_num-16]/2]];
}
   */

  fun load() {
    // TODO share a target buffer on map and obj tile files.
    val mapTiles = LZW.decompress(GameFiles.loadExternal("MAPTILES.VGA"))
    val objTiles = GameFiles.loadExternal("OBJTILES.VGA").readBytes()
    val maskTypes = LZW.decompress(GameFiles.loadExternal("MASKTYPE.VGA"))
    // TODO super inefficient copying here.
    val allTiles = Buffer().apply {
      write(mapTiles)
      write(objTiles)
    }.readByteArray()

    tiles = loadTiles(maskTypes, allTiles)
    assert(tiles.size == TILE_COUNT)
    loadTileFlags(tiles)
    animationData = loadAnimationData()
    loadAnimationMasks()
    generateSpriteSheet()
  }

  fun isAnimatedMapTile(idx: Int) = idx in (ANIMATED_TILE_OFFSET until ANIMATED_TILE_OFFSET + ANIMATED_TILE_COUNT)
  fun getAnimatedBaseTile(idx: Int) = tiles[ANIMATION_SRC_TILES[idx-ANIMATED_TILE_OFFSET] / 2]

  private fun loadTiles(maskTypes: ByteArray, allTiles: ByteArray) =
      GameFiles.loadExternal("TILEINDX.VGA").source().buffer().use { tileIndices ->

        val tileView = ByteView(allTiles, 0)
        (0 until 2048).map {
          val offset = tileIndices.readShortLe().toUInt() * 16U
          val num = it
          val maskType = MaskType.forByte(maskTypes[it].toUByte())
          tileView.seek(offset.toInt())
          val data = readTileData(tileView, maskType)
          Tile(num, data).apply { this.transparent = maskType.isTransparent() }
        }
      }

  private fun readTileData(allTiles: ByteView, maskType: MaskType) : ByteArray {
    val bytes = ByteArray(256)
    if (maskType == MaskType.PixelBlock) {
      bytes.fill(0xff.toByte())
      var sourceIdx = 1 // We don't need the tileLength at sourceOffset. We have markers to parse with.
      var dataIdx = 0
      while(true) {
        val displacement = (allTiles[sourceIdx].toUByte().toInt() or (allTiles[sourceIdx+1].toUByte().toInt() shl 8))
        val offset = displacement % 160 + (if(displacement >= 1760) 160 else 0)
        val len = allTiles[sourceIdx+2].toUByte().toInt()
        if (len == 0) break
        dataIdx += offset
        allTiles.copyInto(bytes, dataIdx, sourceIdx+3, sourceIdx+3+len)
        dataIdx += len
        sourceIdx += 3+len
      }
    } else {
      allTiles.copyInto(destination = bytes, destinationOffset = 0, endIndex = 256)
    }

    // TODO dithering application

    return bytes
  }

  private fun loadTileFlags(tiles: List<Tile>) {
    GameFiles.loadExternal("TILEFLAG").source().buffer().use { buffer ->
      // Flag 1:
      tiles.forEach { TileFlag.addTo(it.flags, buffer.readByte().toInt()) }
      // Flag 2:
      tiles.forEach { TileFlag.addTo(it.flags, buffer.readByte().toInt()) }
      buffer.skip(ARTICLE_SECTION_SKIP)
      tiles.forEach { it.articleType = ArticleType.forFlag(buffer.readByte().toUByte().toInt()) }
    }
  }

  private fun loadAnimationData() : AnimationInfo {
    return GameFiles.loadExternal("ANIMDATA").source().buffer().use { buffer ->
      val animCount = buffer.readShortLe().toUShort().toInt()
      val data = (0 until 32).map { AnimationData() }.toList().apply {
        forEach {
          it.tile = buffer.readShortLe().toUShort().toInt()
          if (it.tile in TRIGGERED_ANIM) it.loopCount = 0 else it.loopCount = -1
        }
        forEach { it.firstFrame = buffer.readShortLe().toUShort().toInt() }
        forEach { it.andMasks = buffer.readByte() }
        forEach { it.shiftValues = buffer.readByte() }
      }
      AnimationInfo(animCount, data)
    }
  }

  private fun loadAnimationMasks() {
    val maskData = ByteView(LZW.decompress(GameFiles.loadExternal("ANIMMASK.VGA")), 0)
    (0 until 32).forEach { maskIdx ->
      val tile = tiles[ANIMATED_TILE_OFFSET + maskIdx]
      tile.transparent = true
      var tileDataIndex = 0
      maskData.seek(maskIdx * 64)
      var clearByteCount = maskData[0].toUByte().toInt()
      if (clearByteCount > 0) {
        tile.clearPixels(tileDataIndex, clearByteCount)
        tileDataIndex += clearByteCount
      }
      maskData.shift(1)

      var displacement = maskData[0].toUByte().toInt()
      clearByteCount = maskData[1].toUByte().toInt()
      while (displacement != 0 && clearByteCount != 0) {
        maskData.shift(2)
        tileDataIndex += displacement
        tile.clearPixels(tileDataIndex, clearByteCount)
        tileDataIndex += clearByteCount
        displacement = maskData[0].toUByte().toInt()
        clearByteCount = maskData[1].toUByte().toInt()
      }
    }
  }

  private fun generateSpriteSheet() {
    val pixmap = Pixmap((tiles.size * 16) / 4, (tiles.size * 16) / 4, Pixmap.Format.RGBA8888)
    tiles.forEachIndexed { index, tile -> tile.generateTexture((index / 4 * 16), index % 4 * 16, pixmap) }
    val spriteSheet = Texture(pixmap).apply { pixmap.dispose() }
    tiles.forEachIndexed { index, tile -> tile.bindRegion(spriteSheet, (index / 4 * 16), index % 4 * 16) }
  }
}

enum class MaskType(val value: UByte) {
  Plain(0U), Trans(5U), PixelBlock(10U);

  fun isTransparent() = this != Plain

  companion object {
    fun forByte(value: UByte) = values().first { it.value == value }
  }
}

enum class TileFlag(val masks: List<Int>) {
  Passable(0x2),
  Water(0x1),
  Damages(0x8),
  TopTile(0x10),
  Boundary(listOf(0x4, 0x8)),
  DoubleHeight( 0x40),
  DoubleWidth( 0x80);

  fun matches(flag: Int) = this.masks.any { (flag and it) > 0 }

  constructor(mask: Int) : this(listOf(mask))

  companion object {
    fun emptySet() = EnumSet.noneOf(TileFlag::class.java)
    fun addTo(set: EnumSet<TileFlag>, flag: Int) = values().filterTo(set) { it.matches(flag) }
  }
}

enum class ArticleType(val mask: Int) {
  None(0x00), A(0x01), An(0x10), The(0x11);

  companion object {
    fun forFlag(input: Int): ArticleType {
      val value = (input shr 6)
      return values().first { value and it.mask == it.mask }
    }
  }
}

class Tile(val number: Int, data: ByteArray) {
  var data: ByteArray? = data // nullable so we can release the memory!
  var flags = TileFlag.emptySet()
  var transparent = false
  lateinit var sprite: TextureRegion
  lateinit var articleType: ArticleType

  fun clearPixels(offset: Int, count: Int) {
    data!!.fill(TRANSPARENT, offset, offset+count)
  }

  fun generateTexture(xOffset: Int, yOffset: Int, pixmap: Pixmap) {
    assert(data != null)

    (0 until 16).forEach { row ->
      (0 until 16).forEach { col ->
        val value = data!![row + col * 16]
        val color =
            if (this.transparent && value == TRANSPARENT) Color.argb8888(1.toFloat(), 1.toFloat(), 1.toFloat(), 0.toFloat())
            else Palettes.gamePalette.values[value.toUByte().toInt()].let {
              Color.argb8888(it.red, it.green, it.blue, 1.toFloat())
            }
        pixmap.drawPixel(xOffset + row, yOffset + col, color)
      }
    }
    this.data = null
  }
  fun bindRegion(texture: Texture, xOffset: Int, yOffset: Int) {
    this.sprite = TextureRegion(texture, xOffset, yOffset, 16, 16)
  }
}

data class AnimationInfo(val count: Int, val data: List<AnimationData>)

data class AnimationData(var tile: Int = 0, var firstFrame: Int = 0, var andMasks: Byte = 0, var shiftValues: Byte = 0, var loopDirection: Boolean = false, var loopCount: Int = 0)