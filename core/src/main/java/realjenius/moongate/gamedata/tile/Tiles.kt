package realjenius.moongate.gamedata.tile

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
import realjenius.moongate.gamedata.tile.Tiles.TRANSPARENT
import realjenius.moongate.gamedata.palette.Palettes
import realjenius.moongate.io.readByteToInt
import realjenius.moongate.io.readUByte
import realjenius.moongate.io.readUByteToInt
import realjenius.moongate.io.readUShortLeToInt
import realjenius.moongate.toChar
import realjenius.moongate.uByteToInt
import java.util.*

object Tiles {
  const val TRANSPARENT = 0xff.toByte() // move somewhere more appropriate.
  private const val TILE_COUNT = 2048
  private const val OBJECT_WEIGHTS = (5120 - (TILE_COUNT * 2)).toLong()
  private val TRIGGERED_ANIM = setOf(862, 1009, 1020) // crank, crank, chain

  // Animated tiles are from 16-48, we use this to find the base animation tile to file the base layer
  private val ANIMATION_SRC_TILES = listOf(
      0x16, 0x16, 0x1a, 0x1a, 0x1e, 0x1e, 0x12, 0x12, 0x1a, 0x1e, 0x16, 0x12, 0x16, 0x1a, 0x1e, 0x12,
      0x1a, 0x1e, 0x1e, 0x12, 0x12, 0x16, 0x16, 0x1a, 0x12, 0x16, 0x1e, 0x1a, 0x1a, 0x1e, 0x12, 0x16
  )

  private val ANIMATED_TILE_COUNT = ANIMATION_SRC_TILES.size
  private const val ANIMATED_TILE_OFFSET = 16
  private const val CURSOR_IDX = 365
  private const val USE_IDX = 364

  private lateinit var spriteSheet: Texture
  lateinit var tiles: List<Tile>
  lateinit var cursorPixmap: Pixmap
  lateinit var usePixmap: Pixmap
  private lateinit var animationData: AnimationInfo

  fun load() {
    loadTiles()
    loadTileFlags()
    loadAnimationData()
    loadAnimationMasks()
    loadCursors()
    loadLookData()
    generateSpriteSheet()
  }

  fun isAnimatedMapTile(idx: Int) = idx in (ANIMATED_TILE_OFFSET until ANIMATED_TILE_OFFSET + ANIMATED_TILE_COUNT)
  fun getAnimatedBaseTile(idx: Int) = tiles[ANIMATION_SRC_TILES[idx - ANIMATED_TILE_OFFSET] / 2]

  fun dispose() {
    if (Tiles::spriteSheet.isInitialized) spriteSheet.dispose()
  }

  private fun loadTiles() {
    val allTiles = Buffer().apply {
      LZW.decompressInto(GameFiles.loadExternal("MAPTILES.VGA"), this)
      this.writeAll(GameFiles.loadExternal("OBJTILES.VGA").source())
    }.readByteArray()
    val maskTypes = LZW.decompress(GameFiles.loadExternal("MASKTYPE.VGA"))

    tiles = GameFiles.loadExternal("TILEINDX.VGA").source().buffer().use { tileIndices ->

      val tileView = ByteView(allTiles, 0)
      (0 until 2048).map {
        val offset = tileIndices.readUShortLeToInt() * 16
        val num = it
        val maskType = MaskType.forByte(maskTypes[it].toUByte())
        tileView.seek(offset)
        val data = readTileData(tileView, maskType)
        Tile(num, data).apply { this.transparent = maskType.isTransparent() }
      }
    }.apply { assert(this.size == TILE_COUNT) }
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

  private fun loadTileFlags() {
    GameFiles.loadExternal("TILEFLAG").source().buffer().use { buffer ->
      // Flag 1:
      tiles.forEach { TileFlag.addTo(it.flags, buffer.readByteToInt()) }
      // Flag 2:
      tiles.forEach { TileFlag.addTo(it.flags, buffer.readByteToInt()) }
      // Object weights are kept here... for some reason. Ideally this parsing would be shared between the two classes.
      buffer.skip(OBJECT_WEIGHTS)
      // Articles
      tiles.forEach { it.articleType = ArticleType.forFlag(buffer.readUByteToInt()) }
    }
  }

  private fun loadAnimationData() {
    animationData = GameFiles.loadExternal("ANIMDATA").source().buffer().use { buffer ->
      val animCount = buffer.readUShortLeToInt()
      val data = (0 until 32).map { AnimationData() }.toList().apply {
        forEach {
          it.tile = buffer.readUShortLeToInt()
          if (it.tile in TRIGGERED_ANIM) it.loopCount = 0 else it.loopCount = -1
        }
        forEach { it.firstFrame = buffer.readUShortLeToInt() }
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
      var clearByteCount = maskData[0].uByteToInt()
      if (clearByteCount > 0) {
        tile.clearPixels(tileDataIndex, clearByteCount)
        tileDataIndex += clearByteCount
      }
      maskData.shift(1)

      var displacement = maskData[0].uByteToInt()
      clearByteCount = maskData[1].uByteToInt()
      while (displacement != 0 && clearByteCount != 0) {
        maskData.shift(2)
        tileDataIndex += displacement
        tile.clearPixels(tileDataIndex, clearByteCount)
        tileDataIndex += clearByteCount
        displacement = maskData[0].uByteToInt()
        clearByteCount = maskData[1].uByteToInt()
      }
    }
  }

  private fun loadLookData() {
    val buffer = Buffer()
    LZW.decompressInto(GameFiles.loadExternal("LOOK.LZD"), target = buffer)
    var lastNotFilled = 0
    var idx = 0
    while (idx < 2048) {
      idx = buffer.readUShortLeToInt()
      if (idx >= 2048) break
      val str = StringBuilder()
      var char = buffer.readUByte().toChar()
      while(char != 0.toChar()) {
        str.append(char)
        char = buffer.readUByte().toChar()
      }
      val desc = TileDescription.parse(str.toString())
      (lastNotFilled..idx).forEach {
        tiles[it].description = desc
      }
      lastNotFilled = idx+1
    }
    (lastNotFilled until 2048).forEach {
      tiles[it].description = tiles[0].description
    }
  }

  private fun generateSpriteSheet() {
    val pixmap = Pixmap((tiles.size * 16) / 4, (tiles.size * 16) / 4, Pixmap.Format.RGBA8888)
    tiles.forEachIndexed { index, tile -> tile.generateTexture((index / 4 * 16), index % 4 * 16, pixmap) }
    spriteSheet = Texture(pixmap).apply { pixmap.dispose() }
    tiles.forEachIndexed { index, tile -> tile.bindRegion(spriteSheet, (index / 4 * 16), index % 4 * 16) }
  }

  private fun loadCursors() {
    cursorPixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
    tiles[CURSOR_IDX].generateTexture(0, 0, cursorPixmap)

    usePixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
    tiles[USE_IDX].generateTexture(0, 0, usePixmap)
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
  lateinit var description: TileDescription
  lateinit var sprite: TextureRegion
  lateinit var articleType: ArticleType

  fun isStackable() = description.isStackable

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
  }
  fun bindRegion(texture: Texture, xOffset: Int, yOffset: Int) {
    this.sprite = TextureRegion(texture, xOffset, yOffset, 16, 16)
    this.data = null
  }
}

data class AnimationInfo(val count: Int, val data: List<AnimationData>)

data class AnimationData(var tile: Int = 0, var firstFrame: Int = 0, var andMasks: Byte = 0, var shiftValues: Byte = 0, var loopDirection: Boolean = false, var loopCount: Int = 0)

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