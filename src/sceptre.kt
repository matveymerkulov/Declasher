const val PERCENT_ON = 0.7
const val MIN_DETECTION_WIDTH = 6
const val MIN_DETECTION_HEIGHT = 6
const val MIN_DETECTION_PIXELS = 140
const val MAX_DIFFERENCE_FOR_ALL_BG = 900

var project = "sceptre"

val MAIN_SCREEN = Rect(0, 0, 32, 20)
val STATUS_BAR = Rect(0, 20, 32, 4)

val PIXEL_WIDTH = MAIN_SCREEN.pixelWidth()
val PIXEL_HEIGHT = MAIN_SCREEN.pixelHeight()
val PIXEL_SIZE = MAIN_SCREEN.pixelSize()

const val BORDER_SIZE = 4
const val MAX_DISTANCE = 3
const val MIN_WIDTH = 10
const val MAX_WIDTH = 180
const val MIN_HEIGHT = 6
const val MAX_HEIGHT = 90
const val MIN_FRAMES = 5
const val FRAME_FREQUENCY = 1
const val OUT_DIR = "D:/output/"

var defaultArea = Rect(0, 0
  , MAIN_SCREEN.pixelWidth(), MAIN_SCREEN.pixelHeight())

val MAX_BG_DIFFERENCE = DefaultMap(900, mapOf("koo" to 900))

var particles = mapOf("koo" to Rect(40, 88, 64, 48))

// debug
const val SAVE_COLORED = false
const val RESIZED = false
const val BLACK_AND_WHITE = true
const val SAVE_SIMILAR = false
//const val SHOW_BG_DIFFERENCE = false
const val SHOW_BG_DIFFERENCE = true

const val SHOW_DETECTION_AREA = false
//const val SHOW_DETECTION_AREA = true
val mode = Mode.EXTRACT_SPRITES
//val mode = Mode.DECLASH
//val mode = Mode.SHOW_DIFFERENCE
//val mode = Mode.SCREENSHOTS
const val ONLY_BACKGROUND = ""
const val ONLY_ABSENT = false
//const val ONLY_ABSENT = true
//const val ANY_IS_CHANGED = false
const val ANY_IS_CHANGED = true

const val SPRITE_COLOR = white
const val PARTICLE_COLOR = white

fun process() {
  locations()

  Screen.process()

  when(mode) {
    Mode.COLOR_BACKGROUNDS -> {
      Screen.saveBackgrounds()
    } Mode.EXTRACT_SPRITES -> {
      ImageExtractor.saveImages()
    }
  }
}

fun locations() {}
