package realjenius.moongate.gamedata.tile

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import okio.Buffer
import okio.buffer
import okio.source
import realjenius.moongate.gamedata.palette.Palette
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
      (0 until TILE_COUNT).map {
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
      tiles.forEach { TileFlag.addTo(it.flags, 1, buffer.readByteToInt()) }
      // Flag 2:
      tiles.forEach { TileFlag.addTo(it.flags, 2, buffer.readByteToInt()) }
      // Object weights are kept here... for some reason. Ideally this parsing would be shared between the two classes.
      buffer.skip(OBJECT_WEIGHTS)
      // Flag 3 and Articles
      tiles.forEach {
        val flag3 = buffer.readUByteToInt()
        TileFlag.addTo(it.flags, 3, flag3)
        it.articleType = ArticleType.forFlag(flag3)
      }
    }
  }

  private fun loadAnimationData() {
    animationData = GameFiles.loadExternal("ANIMDATA").source().buffer().use { buffer ->
      val animCount = buffer.readUShortLeToInt()
      val data = (0 until 32).map { AnimationInfo.AnimationData() }.toList().apply {
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
    for (maskIdx in 0 until 32) {
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
    // We will put rotated tile copies on the bottom of the spritesheet, 1 row per tile rotation.
    val rotatedTiles = tiles.filter { it.containsRotatedColors() }
    val pixmap = Pixmap((tiles.size * 16) / 4, ((rotatedTiles.size * 16) + (tiles.size * 16) / 4), Pixmap.Format.RGBA8888)
    tiles.forEachIndexed { index, tile ->
      tile.generateTexture((index / 4 * 16), index % 4 * 16, Palettes.forRotation(0), pixmap)
    }
    for (palette in 1 until Palettes.rotationCount()) {
      rotatedTiles.forEachIndexed { index, tile ->
        tile.generateTexture(palette * 16, ((index * 16) + (tiles.size * 16) / 4), Palettes.forRotation(palette), pixmap)
      }
    }

    spriteSheet = Texture(pixmap).apply { pixmap.dispose() }
    tiles.forEachIndexed { index, tile -> tile.bindRegion(spriteSheet, 0, (index / 4 * 16),index % 4 * 16) }
    for (palette in 1 until Palettes.rotationCount()) {
      rotatedTiles.forEachIndexed { index, tile ->
        tile.bindRegion(spriteSheet, palette, palette * 16, ((index * 16) + (tiles.size * 16) / 4))
      }
    }
  }

  private fun loadCursors() {
    cursorPixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
    tiles[CURSOR_IDX].generateTexture(0, 0, Palettes.forRotation(0), cursorPixmap)

    usePixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
    tiles[USE_IDX].generateTexture(0, 0, Palettes.forRotation(0), usePixmap)
  }
}

