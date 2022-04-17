import java.awt.Color
import kotlin.Throws
import java.io.IOException
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

const val brig = 0x9F
const val black = 0x000000
const val darkBlue = 0x000001 * brig
const val darkRed = 0x010000 * brig
const val darkMagenta = 0x010001 * brig
const val darkGreen = 0x000100 * brig
const val darkCyan = 0x000101 * brig
const val darkYellow = 0x010100 * brig
const val grey = 0x010101 * brig
const val blue = 0x0000FF
const val red = 0xFF0000
const val magenta = 0xFF00FF
const val green = 0x00FF00
const val cyan = 0x00FFFF
const val yellow = 0xFFFF00
const val white = 0xFFFFFF

@JvmField
val color = intArrayOf(black, darkBlue, darkRed, darkMagenta, darkGreen
  , darkCyan, darkYellow, grey, black, blue, red, magenta, green, cyan
  , yellow, white)

enum class Pixel {OFF, ON, ANY}

class Area(val pixels: Array<Pixel>, val attrs: IntArray, val area: Rect)

class Coords(val x: Int, val y: Int)

class ChangedArea(val num: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int) {
  fun draw(image: BufferedImage) {
    Rect(x1, y1, x2 - x1, y2 - y1).draw(image)
  }
}

class Rect(val x: Int, val y:Int, val width: Int, val height: Int) {
  fun pixelWidth(): Int {
    return width shl 3
  }

  fun pixelHeight(): Int {
    return height shl 3
  }

  fun size(): Int {
    return width * height
  }

  fun pixelSize(): Int {
    return size() shl 6
  }

  fun has(x0:Int, y0:Int): Boolean {
    return x0 >= x && y0 >= y && x0 < x + width && y0 < y + height
  }

  fun draw(image: BufferedImage) {
    val g = image.createGraphics()
    g.color = Color.cyan
    g.drawLine(x, y, x + width - 1, y)
    g.drawLine(x + width - 1, y, x + width - 1, y + height - 1)
    g.drawLine(x + width - 1, y + height - 1, x, y + height - 1)
    g.drawLine(x, y + height - 1, x, y)
  }
}

fun loadRepainted(fileName: String):BufferedImage {
  return ImageIO.read(File("$project/repainted/$fileName.png"))
}

enum class Mode {
  EXTRACT_SPRITES, EXTRACT_BACKGROUNDS, DECLASH, DETECT_MAX_SIZE
  , SHOW_DIFFERENCE, SCREENSHOTS, COLOR_BACKGROUNDS, FIND_SPRITE_POSITION
}

// main
@Throws(IOException::class)
fun main(args: Array<String>) {
  Screen.init()
  val files = File(OUT_DIR).listFiles()
  if(files != null) for(file in files) file.delete()
  process()
  println("Min sprite pixels is ${Sprites.minSpritePixels}")
  println("Min detection area size is ${Sprites.maxDetectionSize} pixels")
  println("Max image is ${Image.maxSize} pixels")
  println("Max errors is ${Sprites.maxErrors}")
  println("Max difference is ${Sprites.maxDifference}")
  println("Max background difference is ${Screen.maxBackgroundDifference}")
}

fun resizeImage(originalImage: BufferedImage, targetWidth: Int
                , targetHeight: Int): BufferedImage {
  val resultingImage = originalImage.getScaledInstance(targetWidth
      , targetHeight, java.awt.Image.SCALE_DEFAULT)
  val outputImage = BufferedImage(targetWidth, targetHeight
      , BufferedImage.TYPE_INT_RGB)
  outputImage.graphics.drawImage(resultingImage, 0, 0, null)
  return outputImage
}

fun x3(image: BufferedImage): BufferedImage {
  return if(RESIZED) resizeImage(image, image.width * 3
      , image.height * 3) else image
}

fun copyImage(image: BufferedImage): BufferedImage {
  val cm = image.colorModel
  val isAlphaPremultiplied = cm.isAlphaPremultiplied
  val raster = image.copyData(null)
  return BufferedImage(cm, raster, isAlphaPremultiplied, null)
}