import java.awt.image.BufferedImage

class Background {
  val pixels: BooleanArray
  val image: BufferedImage?
  val fileName: String
  val hasForcedColor: Boolean
  val skip: Boolean

  constructor(pixels: BooleanArray) {
    this.pixels = pixels
    this.image = null
    this.fileName = ""
    this.hasForcedColor = false
    this.skip = false
  }

  constructor(pixels: BooleanArray, image: BufferedImage?, fileName: String) {
    val num = fileName.substring(0, fileName.indexOf(".")).toInt();
    this.pixels = pixels
    this.image = image
    this.fileName = fileName
    this.hasForcedColor = forcedColor.contains(num)
    this.skip = skippedBackgrounds.contains(num)
  }

  fun difference(screen: BooleanArray): Int {
    var difference = 0
    for(i in 0 until MAIN_SCREEN.pixelSize()) if(pixels[i] != screen[i]) difference++
    return difference
  }
}