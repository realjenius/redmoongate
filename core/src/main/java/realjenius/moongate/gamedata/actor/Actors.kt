package realjenius.moongate.gamedata.actor

import okio.Buffer
import realjenius.moongate.io.readUByteToInt

object Actors {
  private val actors = (0 until 256).map { Actor(it) }

  fun load(buffer: Buffer) {
    // 0x000 - 0x100 = object flags (torches and such)
    actors.forEach { it.objectFlags = buffer.readUByteToInt() }
    // 0x100 - 0x400 = actor positions
    actors.forEach {
      // TODO - duplicated logic with gameobjects
      val coord1 = buffer.readUByteToInt()
      val coord2 = buffer.readUByteToInt()
      val coord3 = buffer.readUByteToInt()

      val x = coord1 + ((coord2 and 0x3) shl 8)
      val y = ((coord2 and 0xfc) shr 2) + ((coord3 and 0xf) shl 6)
      val z = (coord3 and 0xf0) shr 4
      

    }
  }
}

data class Actor(var id: Int = 0,
                 var x: Int = 0,
                 var y: Int = 0,
                 var z: Int = 0,
                 var temp: Boolean = false,
                 var objectFlags: Int = 0)