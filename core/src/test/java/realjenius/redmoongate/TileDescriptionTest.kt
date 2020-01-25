package realjenius.redmoongate

import org.junit.Assert.assertEquals
import org.junit.Test
import realjenius.moongate.gamedata.tile.FixedSegment
import realjenius.moongate.gamedata.tile.PluralChoice
import realjenius.moongate.gamedata.tile.TileDescription

class TileDescriptionTest {

  @Test
  fun parseTest() {
    assertEquals(
        TileDescription(
            listOf(FixedSegment("loa"), PluralChoice("f", "ves"), FixedSegment(" of bread"))
        ),
        TileDescription.parse("loa/f\\ves of bread")
    )

    assertEquals(
        TileDescription(
            listOf(FixedSegment("gold coin"), PluralChoice("", "s"))
        ),
        TileDescription.parse("gold coin\\s")
    )

    assertEquals(
        TileDescription(
            listOf(FixedSegment("flask"), PluralChoice("", "s"), FixedSegment(" of oil"))
        ),
        TileDescription.parse("flask\\s of oil")
    )
  }

  @Test
  fun pluralityTest() {
    val desc = TileDescription.parse("loa/f\\ves of bread")
    assertEquals("loaf of bread", desc.forCount(1))
    assertEquals("loaves of bread", desc.forCount(2))
  }
}