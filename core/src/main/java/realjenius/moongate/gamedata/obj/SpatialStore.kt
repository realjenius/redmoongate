package realjenius.moongate.gamedata.obj

import realjenius.moongate.gamedata.map.Maps
import java.util.*

class SpatialStore<T>(level: Int) {
  private val levelSize = Maps.diameter(level)
  private val objects: SortedMap<Int, MutableList<T>> = TreeMap()

  fun add(x: Int, y: Int, obj: T) {
    objects.computeIfAbsent(toKey(x, y)) { arrayListOf() }.apply {
      add(obj)
    }
  }

  fun get(x: Int, y: Int, level: Int) = objects[toKey(x,y)] ?: emptyList<T>()

  private fun toKey(x: Int, y: Int) = y * levelSize + x
}