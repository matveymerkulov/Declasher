import kotlin.Throws
import java.io.IOException
import kotlin.jvm.JvmStatic
import java.io.File
import java.awt.image.BufferedImage
import java.awt.image.WritableRaster

const val BORDER_SIZE = 4
const val MAX_DISTANCE = 2
const val MIN_WIDTH = 10
const val MAX_WIDTH = 48
const val MIN_HEIGHT = 6
const val MAX_HEIGHT = 90
const val AREA_X = 0
const val AREA_Y = 6
const val AREA_WIDTH = 32
const val AREA_HEIGHT = 18
const val MAX_CHANGED_PIXELS = 500
const val MIN_MATCHED_PIXELS = 60
const val MAX_BACKGROUND_DELAY = 10
const val MIN_FRAMES = 5
const val FRAME_FREQUENCY = 10
const val BYTE_SIZE = 32 * 24 * 8
const val ATTR_SIZE = BYTE_SIZE / 8
const val PIXEL_WIDTH = AREA_WIDTH shl 3
const val PIXEL_HEIGHT = AREA_HEIGHT shl 3
const val AREA_SIZE = AREA_WIDTH * AREA_HEIGHT
const val PIXEL_SIZE = AREA_SIZE shl 6
const val MAX_AREA_X = AREA_X + AREA_WIDTH
const val MAX_AREA_Y = AREA_Y + AREA_HEIGHT
val skippedBackgrounds = intArrayOf()
const val OUT_DIR = "D:/output/"

@JvmField
val color = intArrayOf(0x000000, 0x00009F, 0x9F0000, 0x9F009F, 0x009F00
  , 0x009F9F, 0x9F9F00, 0x9F9F9F, 0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
  , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF)

enum class Mode {
  EXTRACT_SPRITES, EXTRACT_BACKGROUNDS, DECLASH, DETECT_MAX_SIZE
  , SHOW_DIFFERENCE, TO_BLACK_AND_WHITE, COLOR_BACKGROUNDS
}

// options
@JvmField
val mode = Mode.EXTRACT_SPRITES

@JvmField
var SPRITE_COLOR = color[15]

@JvmField
var PARTICLE_COLOR = color[15]

@JvmField
var PERCENT_ON = 0.7

@JvmField
var MIN_DETECTION_WIDTH = 8

@JvmField
var MIN_DETECTION_HEIGHT = 8

@JvmField
var MIN_DETECTION_PIXELS = 140

@JvmField
var MIN_DIFFERENCE = 300
var MIN_BG_DIFFERENCE = 900

@JvmField
var project = "ratime"

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
  when(mode) {
    Mode.COLOR_BACKGROUNDS -> Screen.saveBackgrounds()
    Mode.EXTRACT_BACKGROUNDS -> {
      //Screen.process(23073);
      //Screen.process(0, 10000, false);
      Screen.process()
    }
    Mode.EXTRACT_SPRITES -> {
      Screen.process(0, 1000)
      ImageExtractor.saveImages()
    }
    Mode.DECLASH -> {
      Screen.process(0, 1000)
    }
    else -> Screen.process()
  }
  println("Min sprite pixels is ${Sprites.minSpritePixels}")
  println("Min detection area size is ${Sprites.maxDetectionSize} pixels")
  println("Max image is ${Image.maxSize} pixels")
  println("Max errors is ${Sprites.maxErrors}")
  println("Max difference is ${Sprites.maxDifference}")
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