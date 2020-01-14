package realjenius.moongate.screen

import okio.Buffer
import okio.buffer
import okio.source
import realjenius.moongate.io.ByteView
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.LZW
import java.util.*

object Tiles {
  private const val TILE_COUNT = 2048
  private const val ARTICLE_SECTION_SKIP = (5120 - (TILE_COUNT*2)).toLong()
  private val TRIGGERED_ANIM = setOf(862, 1009, 1020) // crank, crank, chain

  lateinit var tiles: List<Tile>
  lateinit var animationData: AnimationInfo

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
  }

  private fun loadTiles(maskTypes: ByteArray, allTiles: ByteArray) =
      GameFiles.loadExternal("TILEINDX.VGA").source().buffer().use { tileIndices ->

        val tileView = ByteView(allTiles, 0)
        (0 until 2048).map {
          val offset = tileIndices.readShortLe().toUInt() * 16U
          val num = it
          val maskType = MaskType.forByte(maskTypes[it].toUByte())
          tileView.seek(offset.toInt())
          val data = readTileData(tileView, maskType)
          Tile(num, data)
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

class Tile(val number: Int, val data: ByteArray) {
  var flags = TileFlag.emptySet()
  lateinit var articleType: ArticleType
}

data class AnimationInfo(val count: Int, val data: List<AnimationData>)

data class AnimationData(var tile: Int = 0, var firstFrame: Int = 0, var andMasks: Byte = 0, var shiftValues: Byte = 0, var loopDirection: Boolean = false, var loopCount: Int = 0)