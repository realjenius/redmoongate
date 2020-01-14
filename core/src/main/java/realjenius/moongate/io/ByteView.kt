package realjenius.moongate.io

class ByteView(val array: ByteArray, var idx: Int) {
  val size: Int
    get() = array.size-idx
  operator fun get(index: Int) = array[idx+index]

  fun copyInto(destination: ByteArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.size) = array.copyInto(
      destination = destination,
      destinationOffset = destinationOffset,
      startIndex = startIndex + idx,
      endIndex = endIndex + idx
  )

  fun seek(index: Int) {
    idx = index
  }

  fun shift(indices: Int) {
    idx += indices
  }

}