package realjenius.moongate.gamedata.tile

data class AnimationInfo(val count: Int, val data: List<AnimationData>) {
  data class AnimationData(var tile: Int = 0,
                           var firstFrame: Int = 0,
                           var andMasks: Byte = 0,
                           var shiftValues: Byte = 0,
                           var loopDirection: Boolean = false,
                           var loopCount: Int = 0)
}
