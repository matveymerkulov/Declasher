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

var defaultArea = Rect(0, 0, 32, 18)

// debug
const val SAVE_COLORED = false
const val SHOW_DETECTION_AREA = false
const val RESIZED = false
const val BLACK_AND_WHITE = false
const val SAVE_SIMILAR = true



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

fun process() {
  if(mode == Mode.FIND_SPRITE_POSITION) {
    Sprites.loadSeveral("static", 0.9, 15, false)
    Screen.process()
    Screen.saveBackgrounds()
  } else {
    Sprites.setLocations("arrow_block", listOf(2246, 184, 112
      , 2246, 96, 104, 2246, 96, 112, 27654, 216, 104, 27945, 24, 104
      , 27945, 56, 104, 27945, 88, 104, 27945, 184, 104, 27945, 216, 104
      , 21450, 24, 104, 21450, 88, 104, 21450, 216, 104, 5038, 120, 104
      , 28366, 176, 104, 752, 96, 112, 53521, 88, 112, 53521, 136, 112
      , 53521, 184, 112, 5652, 40, 112, 5652, 128, 112, 5302, 88, 104
      , 5302, 152, 104, 5302, 184, 104, 14646, 8, 112, 57206, 56, 104
      , 57206, 128, 104, 3192, 176, 112, 2041, 80, 104
      , 20249, 120, 96, 52858, 176, 112, 52858, 224, 112, 5371, 88, 104
      , 5371, 120, 104, 5371, 216, 104, 17627, 96, 112, 17627, 176, 104
      , 13052, 144, 104, 25310, 96, 104, 2655, 200, 112))
    Sprites.setLocations("hourglass_block", listOf(17732, 144, 112
      , 6309, 40, 112, 15467, 168, 88, 46957, 64, 104, 54125, 120, 112
      , 29968, 104, 112, 10065, 104, 112, 49553, 144, 112, 23474, 120, 112
      , 5302, 24, 112, 34454, 120, 112, 38391, 8, 56, 43257, 128, 96))

    Sprites.setLocations("axe_block", listOf(14848, 128, 120))
    Sprites.setLocations("cheese_block", listOf(6915, 184, 112
      , 2437, 48, 64))
    Sprites.setLocations("coat_block", listOf(6915, 120, 104
      , 36376, 16, 112))
    Sprites.setLocations("fish_block", listOf(6541, 80, 96))
    Sprites.setLocations("head_dress_block", listOf(6915, 112, 120
      , 28698, 40, 112))
    Sprites.setLocations("plug_block", listOf(3012, 224, 112))
    Sprites.setLocations("salt_block", listOf(44041, 80, 72))
    Sprites.setLocations("sand_block", listOf(25632, 224, 120
      , 27654, 104, 104, 27654, 104, 112))
    Sprites.setLocations("sphinx_block", listOf(18227, 88, 120))
    Sprites.setLocations("umbrella_block", listOf(1105, 120, 120
      , 3012, 216, 112, 44041, 80, 72))
    Sprites.setLocations("wrench_block", listOf(13225, 136, 112))

    Sprites.loadSeveral("sprites/player", 0.7, 1000
      , true)

    val planeArea = Rect(0, 0, 32, 18)
    Sprites.load("sprites/plane", 0.4, 1000
      , true) { frame: Int -> if(frame == 1) defaultArea else null }

    val islandArea = Rect(0, 16, 32, 2)
    Sprites.load("sprites/island1", 0.8, 20
      , false) { frame: Int -> if(frame == 23687) islandArea else null }
    Sprites.load("sprites/island2", 0.8, 20
      , false) { frame: Int -> if(frame == 3012) islandArea else null }
    Sprites.load("sprites/island3", 0.8, 20
      , false) { frame: Int -> when(frame) {
      24350 -> islandArea
      45276 -> Rect(14, 14, 8, 4)
      else -> null
    }}
    //Screen.process(34000, 35000,34234)
    Screen.process(2430, 5000)
    //Screen.process(20000, 30000)
    //Screen.process()
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
  }
}