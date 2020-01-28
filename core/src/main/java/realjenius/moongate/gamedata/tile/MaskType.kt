package realjenius.moongate.gamedata.tile

enum class MaskType(val value: UByte) {
  Plain(0U), Trans(5U), PixelBlock(10U);

  fun isTransparent() = this != Plain

  companion object {
    fun forByte(value: UByte) = values().first { it.value == value }
  }
}