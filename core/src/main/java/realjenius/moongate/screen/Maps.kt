package realjenius.moongate.screen

import realjenius.moongate.io.ByteView
import realjenius.moongate.io.GameFiles

object Maps {
  private const val SUPERCHUNK_SIZE = 384
  lateinit var chunks: List<Chunk>
  fun load() {
    val map = GameFiles.loadExternal("MAP").readBytes()
    val chunkData = GameFiles.loadExternal("CHUNKS").readBytes()
    val chunkView = ByteView(chunkData, 0)
    chunks = (0 until 1024).map {
      chunkView.seek(it*64)
      Chunk(ByteArray(64).apply { chunkView.copyInto(this, endIndex = 64) })
    }.toList()

//    var mapView = ByteView(map, 0)
//    (0 until 64).forEach {
//      readSuperChunk(surface, mapView, chunks, it)
//      mapView.shift(SUPERCHUNK_SIZE)
//    }
  }

  fun readSuperChunk(surface: ByteArray, map: ByteView, chunk: ByteArray, chunkNum: Int) {
    val worldX = chunkNum % 8 * 128
    val worldY = (chunkNum - (chunkNum % 8)) / 8 * 128
    var mapIndex = 0
    (0 until 16).forEach { first ->
      (0 until 16).forEach { second ->
        val c1 = map[mapIndex+1].toUByte().toInt() shl 8 and map[mapIndex+0].toUByte().toInt()
        val c2 = (map[mapIndex+2].toUByte().toInt() shl 4) or (map[mapIndex+1].toUByte().toInt() shr 4)
        readChunk(surface, chunk, c1*64, worldX + second * 8, worldY + first * 8)
        readChunk(surface, chunk, c2*64, worldX + (second+1) * 8, worldY + first * 8)
        mapIndex += 3
      }
    }
  }

  fun readChunk(surface: ByteArray, chunk: ByteArray, theChunkIndex: Int, x: Int, y: Int) {
    var chunkIndex = theChunkIndex
    var surfaceIndex = y * 1024 + x

    (0 until 8).forEach {
      chunk.copyInto(surface, destinationOffset = chunkIndex, startIndex = surfaceIndex, endIndex = chunkIndex + 8)
      surfaceIndex += 1024
      chunkIndex += 8
    }

  }
}

class Chunk(val tiles: ByteArray)