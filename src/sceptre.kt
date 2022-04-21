/*
var project = "sceptre"

val MAIN_SCREEN = Rect(0, 0, 32, 20)
val STATUS_BAR = Rect(0, 20, 32, 4)

var defaultArea = Rect(0, 0
  , MAIN_SCREEN.pixelWidth(), MAIN_SCREEN.pixelHeight())

val PIXEL_WIDTH = MAIN_SCREEN.pixelWidth()
val PIXEL_HEIGHT = MAIN_SCREEN.pixelHeight()
val PIXEL_SIZE = MAIN_SCREEN.pixelSize()

const val BORDER_SIZE = 2
const val MAX_DISTANCE = 2
const val MIN_WIDTH = 10
const val MAX_WIDTH = 128
const val MIN_HEIGHT = 6
const val MAX_HEIGHT = 128
const val MIN_FRAMES = 4
const val FRAME_FREQUENCY = 1
const val OUT_DIR = "D:/output/"

val MAX_BG_DIFFERENCE = DefaultMap(900, mapOf("koo" to 900))

var particles = mapOf("koo" to Rect(40, 88, 64, 48))

const val PERCENT_ON = 0.7
const val MIN_DETECTION_WIDTH = 6
const val MIN_DETECTION_HEIGHT = 6
const val MIN_DETECTION_PIXELS = 36
const val MAX_DIFFERENCE_FOR_ALL_BG = 1800
const val MIN_BG_CHANGED = 0.55
const val XOR = true

// debug
const val RESIZED = false
const val SAVE_SIMILAR = true

const val SHOW_BG_DIFFERENCE = false
//const val SHOW_BG_DIFFERENCE = true

const val SHOW_DETECTION_AREA = false
//const val SHOW_DETECTION_AREA = true

//val mode = Mode.EXTRACT_BACKGROUNDS
//val mode = Mode.FIND_PIXELS_TO_SKIP
//val mode = Mode.EXTRACT_SPRITES
val mode = Mode.DECLASH
//val mode = Mode.SHOW_DIFFERENCE
//val mode = Mode.SCREENSHOTS

const val ONLY_BACKGROUND = ""
const val ONLY_ABSENT = false
//const val ONLY_ABSENT = true
const val ANY_IS_CHANGED = false
//const val ANY_IS_CHANGED = true
const val BLACK_AND_WHITE = false

val SPRITE_COLOR = DefaultMap(white, mapOf("yellow" to yellow))
const val PARTICLE_COLOR = white

fun process() {
  locations()
  Screen.process(2674 - 1, 5000)
  //Screen.process()
}

fun locations() {
  Sprites.loadSeveral(
    "player", 0.6, 0.5, true
  )

  Sprites.loadSeveral(
    "yellow", 0.6, 0.1, false
  ) { name -> if(name == "pool") null else defaultArea }

  Sprites.loadSeveral(
    "white", 0.6, 0.1, false
  ) { name -> if(name == "pool") null else defaultArea }

  Sprites.loadSeveral("fish", 0.1, 0.5
    , true) {  name -> if(name == "pool") defaultArea else null  }
}
*/