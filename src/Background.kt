import java.awt.image.BufferedImage

class Background {
  val values: BooleanArray
  val image: BufferedImage?
  val fileName: String
  val hasParticles: Boolean
  val skip: Boolean

  constructor(values: BooleanArray) {
    this.values = values
    this.image = null
    this.fileName = ""
    this.hasParticles = false
    this.skip = false
  }

  constructor(values: BooleanArray, image: BufferedImage?, fileName: String) {
    val num = fileName.substring(0, fileName.indexOf(".")).toInt();
    this.values = values
    this.image = image
    this.fileName = fileName
    this.hasParticles = particles.contains(num)
    this.skip = skippedBackgrounds.contains(num)
  }

  fun difference(screen: BooleanArray): Int {
    var difference = 0
    for(i in 0 until PIXEL_SIZE) if(values[i] != screen[i]) difference++
    return difference
  }
}