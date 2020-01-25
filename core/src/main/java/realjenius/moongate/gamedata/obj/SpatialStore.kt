package realjenius.moongate.gamedata.obj

import realjenius.moongate.gamedata.map.Maps
import java.util.*

class SpatialStore(level: Int) {
  private val levelSize = Maps.diameter(level)
  private val objects: SortedMap<Int, MutableList<GameObject>> = TreeMap()

  fun add(x: Int, y: Int, obj: GameObject) {
    objects.computeIfAbsent(toKey(x, y)) { arrayListOf() }.add(obj)
  }

  fun get(x: Int, y: Int, level: Int) = objects[toKey(x,y)] ?: emptyList<GameObject>()

  private fun toKey(x: Int, y: Int) = y * levelSize + x
}