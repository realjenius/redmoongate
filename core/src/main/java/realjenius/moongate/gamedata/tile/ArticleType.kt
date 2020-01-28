package realjenius.moongate.gamedata.tile

enum class ArticleType(val mask: Int) {
  None(0x00), A(0x01), An(0x10), The(0x11);

  companion object {
    fun forFlag(input: Int): ArticleType {
      val value = (input shr 6)
      return values().first { value and it.mask == it.mask }
    }
  }
}