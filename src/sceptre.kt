import Background.Companion.addScope
import Background.Companion.setChangedPixelsColor
import Sprites.loadSeveral

// screen areas

val MAIN_SCREEN = Rect(0, 0, 32, 20)
val STATUS_BAR = Rect(0, 20, 32, 4)
val PIXEL_WIDTH = MAIN_SCREEN.pixelWidth()
val PIXEL_HEIGHT = MAIN_SCREEN.pixelHeight()
val PIXEL_SIZE = MAIN_SCREEN.pixelSize()

// main settings

var project = "sceptre"
const val OUT_DIR = "D:/output/"
const val XOR = true
const val RESIZED = false
const val BLACK_AND_WHITE = false
const val ONLY_ABSENT = false
//const val ONLY_ABSENT = true
const val TWO_FRAMES = true
//const val TWO_FRAMES = false

// mode

//val mode = Mode.SCREENSHOTS
//val mode = Mode.EXTRACT_BACKGROUNDS
val mode = Mode.DECLASH
//val mode = Mode.SHOW_DIFFERENCE

// backgrounds

const val MIN_BG_CHANGED = 0.55
const val SAVE_SIMILAR = true
const val SHOW_BG_DIFFERENCE = false
//const val SHOW_BG_DIFFERENCE = true
const val MIN_FRAMES = 4
const val PERCENT_ON = 0.7
const val MAX_BG_DIFFERENCE = 1800

// declash

const val BORDER_SIZE_FROM = -1
const val BORDER_SIZE_TO = 1
const val MAX_DISTANCE = 2
const val MIN_WIDTH = 2
const val MAX_WIDTH = 256
const val MIN_HEIGHT = 2
const val MAX_HEIGHT = 256
const val MIN_PIXELS = 4

const val SHOW_DETECTION_AREA = false
//const val SHOW_DETECTION_AREA = true
const val ANY_IS_CHANGED = false
//const val ANY_IS_CHANGED = true

const val ONLY_BACKGROUND = "balcony_2"
//const val ONLY_BACKGROUND = ""

fun process() {
  sprites()
  //door, books_and_table, pool3, swords1, rock, balcony, sceptre
  //13358-13945
  Screen.process(1 - 1, 100000)
  //Screen.process()
}

fun sprites() {
  loadSeveral("player", 0.6, 0.3
    , true, white)
  loadSeveral("yellow", 0.3, 0.1
    , false, yellow)
  loadSeveral("white", 0.3, 0.1
    , false, white)
  loadSeveral("cyan", 0.3, 0.1
    , false, cyan)
  loadSeveral("coin", 0.3, 0.1
    , false, yellow, null, mapOf("storage" to defaultArea))

  addScope("pool_1", 0, 9309)
  addScope("pool_2", 9309, 12577)
  addScope("pool_3", 12577, 100000)
  addScope("throne_1", 0, 4690)
  addScope("throne_2", 4690, 100000)
  addScope("door_1", 0, 27964)
  addScope("door_2", 27964, 100000)
}

// TODO
// Static images detection / using
// background ranges export / use