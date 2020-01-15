package realjenius.moongate.screen

import realjenius.moongate.io.ByteView
import realjenius.moongate.io.GameFiles

object Maps {
  private const val SUPERCHUNK_SIZE = 384
  val surfaceMap = ByteArray(1024 * 1024)

  fun load() {
    val map = GameFiles.loadExternal("MAP").readBytes()
    val chunkData = GameFiles.loadExternal("CHUNKS").readBytes()
    println("The map is ${map.size} bytes")
    val mapView = ByteView(map, 0)
    (0 until 64).forEach {
      readSuperChunk(surfaceMap, mapView, chunkData, it)
    }
  }

  fun getRowWidth(level: Int) = if(level == 0) 1024 else 256

  fun readSuperChunk(surface: ByteArray, map: ByteView, chunk: ByteArray, chunkNum: Int) {
    val worldX = chunkNum % 8 * 128
    val worldY = (chunkNum - (chunkNum % 8)) / 8 * 128
    (0 until 16).forEach { first ->
      (0 until 16 step 2).forEach { second ->
        val c1 = ((map[1].toUByte().toInt() and 0xf) shl 8) or map[0].toUByte().toInt()
        val c2 = (map[2].toUByte().toInt() shl 4) or (map[1].toUByte().toInt() shr 4)

        readChunk(surface, chunk, c1 * 64, worldX + second * 8, worldY + first * 8)
        readChunk(surface, chunk, c2 * 64, worldX + (second + 1) * 8, worldY + first * 8)
        map.shift(3)
      }
    }
  }

  fun readChunk(surface: ByteArray, chunk: ByteArray, theChunkIndex: Int, x: Int, y: Int) {
    var chunkIndex = theChunkIndex
    var surfaceIndex = y * 1024 + x

    (0 until 8).forEach {
      chunk.copyInto(surface, destinationOffset = surfaceIndex, startIndex = chunkIndex, endIndex = chunkIndex + 8)
      surfaceIndex += 1024
      chunkIndex += 8
    }
  }
}
