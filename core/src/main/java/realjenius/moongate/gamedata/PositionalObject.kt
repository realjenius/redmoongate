package realjenius.moongate.gamedata

interface PositionalObject {
  var x: Int
  var y: Int
  var z: Int

  fun decodeCoordinates(coord1: Int, coord2: Int, coord3: Int) {
    x = coord1 + ((coord2 and 0x3) shl 8)
    y = ((coord2 and 0xfc) shr 2) + ((coord3 and 0xf) shl 6)
    z = (coord3 and 0xf0) shr 4
  }
}