import java.awt.image.BufferedImage

class Background {
  val pixels: BooleanArray
  val image: BufferedImage?
  val fileName: String
  val hasForcedColor: Boolean
  val skip: Boolean
  val frame: Int

  constructor(pixels: BooleanArray) {
    this.pixels = pixels
    this.image = null
    this.fileName = ""
    this.hasForcedColor = false
    this.skip = false
    this.frame = -1
  }

  constructor(pixels: BooleanArray, image: BufferedImage?, fileName: String) {
    this.frame = fileName.substring(0, fileName.indexOf(".")).toInt();
    this.pixels = pixels
    this.image = image
    this.fileName = fileName
    this.hasForcedColor = forcedColor.contains(this.frame)
    this.skip = skippedBackgrounds.contains(this.frame)
  }

  fun difference(screen: BooleanArray): Int {
    var difference = 0
    for(i in 0 until MAIN_SCREEN.pixelSize()) {
      if(pixels[i] != screen[i]) difference++
    }
    return difference
  }
}