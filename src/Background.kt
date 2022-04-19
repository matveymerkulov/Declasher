import java.awt.image.BufferedImage

class Background {
  val pixels: Array<Pixel>
  val image: BufferedImage?
  val fileName: String
  val particlesArea: Rect?
  val name: String
  var frame: Int = -1

  constructor(pixels: Array<Pixel>) {
    this.pixels = pixels
    this.image = null
    this.fileName = ""
    this.particlesArea = null
    this.name = ""
  }

  constructor(pixels: Array<Pixel>, image: BufferedImage?, fileName: String) {
    this.name = fileName.substring(0, fileName.indexOf("."));
    this.pixels = pixels
    this.image = image
    this.fileName = fileName
    this.particlesArea = particles[this.name]
  }

  fun difference(screen: Array<Pixel>, maxDifference: Int): Int {
    var difference = 0
    for(i in 0 until MAIN_SCREEN.pixelSize()) {
      val pixel = pixels[i]
      if(pixel != screen[i] && pixel != Pixel.ANY) {
        difference++
        if(difference > maxDifference) return difference
      }
    }
    return difference
  }
}