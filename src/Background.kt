import java.awt.image.BufferedImage

class Background {
  val pixels: Array<Pixel>
  val image: BufferedImage?
  val fileName: String
  val hasForcedColor: Boolean
  val skip: Boolean
  val frame: Int

  constructor(pixels: Array<Pixel>) {
    this.pixels = pixels
    this.image = null
    this.fileName = ""
    this.hasForcedColor = false
    this.skip = false
    this.frame = -1
  }

  constructor(pixels: Array<Pixel>, image: BufferedImage?, fileName: String) {
    this.frame = fileName.substring(0, fileName.indexOf(".")).toInt();
    this.pixels = pixels
    this.image = image
    this.fileName = fileName
    this.hasForcedColor = forcedColor.contains(this.frame)
    this.skip = skippedBackgrounds.contains(this.frame)
  }

  fun difference(screen: Array<Pixel>): Int {
    var difference = 0
    for(i in 0 until MAIN_SCREEN.pixelSize()) {
      val pixel = pixels[i]
      if(pixel != screen[i] && pixel != Pixel.ANY) {
        difference++
        if(difference > MAX_BG_DIFFERENCE) break
      }
    }
    return difference
  }
}