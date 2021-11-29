import java.awt.Rectangle
import kotlin.Throws
import java.io.IOException
import java.io.File
import java.awt.image.BufferedImage

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

class Area(val pixels: BooleanArray, val attrs: IntArray, val area: Rect)

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
}

const val SCREEN_WIDTH = 32
const val SCREEN_HEIGHT = 24
const val SCREEN_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT
const val BYTE_SIZE = SCREEN_SIZE * 8
const val FRAME_SIZE = BYTE_SIZE + SCREEN_SIZE

val STATUS_BAR = Rect(0, 0, 32, 6)
val MAIN_SCREEN = Rect(0, 6, 32, 18)

val PIXEL_WIDTH = MAIN_SCREEN.pixelWidth()
val PIXEL_HEIGHT = MAIN_SCREEN.pixelHeight()
val PIXEL_SIZE = MAIN_SCREEN.pixelSize()

const val BORDER_SIZE = 4
const val MAX_DISTANCE = 3
const val MIN_WIDTH = 10
const val MAX_WIDTH = 64
const val MIN_HEIGHT = 6
const val MAX_HEIGHT = 90
const val MAX_CHANGED_PIXELS = 900
const val MIN_MATCHED_PIXELS = 60
const val MAX_BACKGROUND_DELAY = 10
const val MIN_FRAMES = 5
const val FRAME_FREQUENCY = 1
const val MAX_BG_DIFFERENCE = 900
const val OUT_DIR = "D:/output/"

enum class Mode {
  EXTRACT_SPRITES, EXTRACT_BACKGROUNDS, DECLASH, DETECT_MAX_SIZE
  , SHOW_DIFFERENCE, TO_BLACK_AND_WHITE, COLOR_BACKGROUNDS
}

// options
val mode = Mode.DECLASH

var SPRITE_COLOR = white
var PARTICLE_COLOR = white
var PERCENT_ON = 0.7
var MIN_DETECTION_WIDTH = 8
var MIN_DETECTION_HEIGHT = 8
var MIN_DETECTION_PIXELS = 140
var MIN_BG_DIFFERENCE = 300

var project = "ratime"
var forcedColor = listOf(2437, 3012, 34234, 57410)
val skippedBackgrounds = intArrayOf()

// debug
const val SAVE_COLORED = false
const val SHOW_DETECTION_AREA = false
const val RESIZED = false
const val BLACK_AND_WHITE = false
const val SAVE_SIMILAR = true

// main
@Throws(IOException::class)
fun main(args: Array<String>) {
  Screen.init()
  Sprites.load()
  for(file in File(OUT_DIR).listFiles()) file.delete()
  Screen.process(34000, 35000,34234)
  //Screen.process(0, 3000)
  //Screen.process(20000, 40000)
  when(mode) {
    Mode.COLOR_BACKGROUNDS -> {
      Screen.saveBackgrounds()
    } Mode.EXTRACT_BACKGROUNDS -> {
    } Mode.EXTRACT_SPRITES -> {
      ImageExtractor.saveImages()
    } Mode.DECLASH -> {
    } else -> {
    }
  }
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