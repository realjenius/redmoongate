package realjenius.moongate.gamedata.tile

import java.util.*

enum class TileFlag(val flagId: Int, val masks: List<Int>) {
  Passable(1, 0x2),
  Water(1, 0x1),
  Damages(1, 0x8),
  TopTile(2, 0x10),
  Boundary(2, listOf(0x4, 0x8)),
  DoubleHeight(2,  0x40),
  DoubleWidth(2, 0x80),
  BottomTile(3, 0x4);

  fun matches(flagId: Int, flag: Int) = this.flagId == flagId && this.masks.any { (flag and it) > 0 }

  constructor(flagId: Int, mask: Int) : this(flagId, listOf(mask))

  companion object {
    fun emptySet() = EnumSet.noneOf(TileFlag::class.java)
    fun addTo(set: EnumSet<TileFlag>, flagId: Int, flag: Int) = values().filterTo(set) { it.matches(flagId, flag) }
  }
}