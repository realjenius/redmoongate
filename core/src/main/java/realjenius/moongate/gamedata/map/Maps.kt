package realjenius.moongate.gamedata.map

import realjenius.moongate.io.ByteView
import realjenius.moongate.io.GameFiles

object Maps {
  val levelSpecs = listOf(
      LevelSpec(0, 1024, 16, 64), // Surface
      LevelSpec(1, 256, 32, 1), // Dungeons 1-5
      LevelSpec(2, 256, 32, 1),
      LevelSpec(3, 256, 32, 1),
      LevelSpec(4, 256, 32, 1),
      LevelSpec(5, 256, 32, 1)
  )

  fun load() {
    val map = GameFiles.loadExternal("MAP").readBytes()
    val chunkData = GameFiles.loadExternal("CHUNKS").readBytes()
    val mapView = ByteView(map, 0)
    for (level in levelSpecs)
      for (chunkNumber in 0 until level.chunkCount)
        readChunks(mapView, chunkData, chunkNumber, level)
  }

  fun diameter(level: Int) = levelSpecs[level].diameter

  fun mapForLevel(level: Int) = levelSpecs[level].data

  private fun readChunks(map: ByteView, chunk: ByteArray, chunkNum: Int, level: LevelSpec) {
    val worldX = chunkNum % 8 * 128
    val worldY = (chunkNum - (chunkNum % 8)) / 8 * 128
    val schunkDiam = level.blockSize
    for (first in 0 until schunkDiam) {
      for (second in 0 until schunkDiam step 2) {
        val c1 = ((map[1].toUByte().toInt() and 0xf) shl 8) or map[0].toUByte().toInt()
        val c2 = (map[2].toUByte().toInt() shl 4) or (map[1].toUByte().toInt() shr 4)

        readChunk(chunk, c1 * 64, worldX + second * 8, worldY + first * 8, level)
        readChunk(chunk, c2 * 64, worldX + (second + 1) * 8, worldY + first * 8, level)
        map.shift(3)
      }
    }
  }

  private fun readChunk(chunk: ByteArray, theChunkIndex: Int, x: Int, y: Int, level: LevelSpec) {
    var chunkIndex = theChunkIndex
    var mapIndex = y * level.diameter + x

    for (i in 0 until 8) {
      chunk.copyInto(level.data, destinationOffset = mapIndex, startIndex = chunkIndex, endIndex = chunkIndex + 8)
      mapIndex += level.diameter
      chunkIndex += 8
    }
  }
}

data class LevelSpec(val level: Int, val diameter: Int, val blockSize: Int, val chunkCount: Int) {
  val data: ByteArray = ByteArray(diameter * diameter)
}
