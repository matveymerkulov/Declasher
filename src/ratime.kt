/*
const val PERCENT_ON = 0.7
var project = "ratime"

// debug
const val TWO_FRAMES = true
const val RESIZED = true
const val BLACK_AND_WHITE = false
const val SAVE_SIMILAR = true

val STATUS_BAR = Rect(0, 0, 32, 6)
val MAIN_SCREEN = Rect(0, 6, 32, 18)

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
const val MIN_PIXELS = 140
const val MIN_BG_CHANGED = 0.55
const val OUT_DIR = "D:/output/"

const val MAX_BG_DIFFERENCE = 300

const val SHOW_DETECTION_AREA = false
//const val SHOW_DETECTION_AREA = true
const val SHOW_BG_DIFFERENCE = false
//const val SHOW_BG_DIFFERENCE = true
val mode = Mode.DECLASH
//val mode = Mode.SHOW_DIFFERENCE
//val mode = Mode.SCREENSHOTS
const val ONLY_BACKGROUND = ""
//const val ONLY_ABSENT = false
const val ONLY_ABSENT = true
//const val ANY_IS_CHANGED = false
const val ANY_IS_CHANGED = true
const val XOR = false

fun process() {
  locations()
  Screen.process(0, -1, 100)
}

fun locations() {
  setMaxDifference("000001", 900)
  setMaxDifference("003012", 700)
  setMaxDifference("006915", 1500)
  setMaxDifference("024350", 900)
  setMaxDifference("027654", 900)
  setMaxDifference("028366", 900)
  setMaxDifference("034454", 900)
  setMaxDifference("055614", 900)
  setMaxDifference("057410", 900)
  setMaxDifference("038878", 900)
  setMaxDifference("045276", 900)
  setMaxDifference("046957", 900)
  setMaxDifference("050377", 900)
  setMaxDifference("056547", 1500)
  setMaxDifference("053902", 900)
  setMaxDifference("054125", 900)
  setMaxDifference("053902", 900)
  setMaxDifference("057206", 900)
  setMaxDifference("057410", 1500)
  
  setParticles("002437", Rect(40, 88, 64, 48))
  setParticles("003012", Rect(32, 136, 168, 8))
  setParticles("014848", Rect(48, 80, 64, 64))
  setParticles("023687", Rect(80, 136, 104, 8))
  setParticles("024350", Rect(88, 60, 128, 88))
  setParticles("034234", defaultArea)
  setParticles("043658", defaultArea)
  setParticles("045276", Rect(104, 120, 72, 24))
  setParticles("045623", defaultArea)
  setParticles("046091", defaultArea)
  setParticles("050159", defaultArea)
  setParticles("050377", Rect(48, 136, 40, 8))
  setParticles("053902", Rect(56, 136, 120, 8))
  setParticles("056547", Rect(48, 136, 144, 8))
  setParticles("057410", defaultArea)

  Sprites.loadSeveral("player", 0.6, 0.9
    , true)

  Sprites.load("plane", 0.7, 0.4
    , true, white, null
    , mapOf("000001" to defaultArea))

  Sprites.load("cyan_island", 0.6, 0.3
    , false, white, null
    , mapOf("003012" to Rect(0, 128, 256, 16)))
  Sprites.load("white_island", 0.6, 0.2
    , true, white, null
    , mapOf("023687" to Rect(72, 128, 112, 16)))
  Sprites.load("red_island", 0.6, 0.2
    , true, white, null, mapOf(
      "024350" to Rect(0, 128, 256, 16),
      "043658" to Rect(128, 120, 32, 24),
      "045276" to Rect(112, 112, 64, 32),
      "053902" to Rect(48, 132, 136, 16)))

  Sprites.load("croc", 0.6, 0.2
    , true, green, null
    , mapOf("053902" to Rect(52, 128, 128, 16)))

  Sprites.load("wall", 0.8, 0.1
    , true, white, null
    , mapOf("027654" to Rect(104, 112, 16, 32)))
  Sprites.load("stone", 0.8, 0.1
    , true, yellow, null
    , mapOf("027654" to Rect(28, 112, 60, 16)))
  Sprites.load("sand_bag", 0.8, 0.1
    , true, yellow, null
    , mapOf("027654" to Rect(104, 96, 16, 32)))

  Sprites.setLocations("arrow_block", listOf(
    752, 96, 112
    , 2041, 80, 112 // 104, 112
    , 2246, 184, 112 // 104, 112
    , 2246, 96, 112 // 104, 112
    , 2655, 200, 112
    , 3192, 176, 112
    , 5038, 120, 104
    , 5302, 88, 104, 5302, 152, 104, 5302, 184, 104
    , 5371, 88, 104, 5371, 120, 104, 5371, 216, 104
    , 5652, 40, 112, 5652, 128, 112
    , 13052, 144, 104
    , 14646, 8, 112
    , 14848, 128, 120
    , 17627, 96, 112, 17627, 176, 104
    , 20249, 120, 96
    , 21450, 24, 104, 21450, 88, 104, 21450, 216, 104
    , 25310, 96, 104
    , 27654, 216, 104
    , 27945, 24, 104, 27945, 56, 104, 27945, 88, 104, 27945, 184, 104
    , 27945, 216, 104
    , 28366, 176, 104
    , 52858, 176, 112, 52858, 224, 112
    , 53521, 88, 112, 53521, 136, 112, 53521, 184, 112
    , 57206, 56, 104, 57206, 128, 104
  ))

  Sprites.setLocations("hourglass_block", listOf(
    5302, 24, 112
    , 6309, 40, 112
    , 10065, 104, 112
    , 15467, 168, 88
    , 17732, 144, 112
    , 23474, 120, 112
    , 29968, 104, 112
    , 34454, 120, 112
    , 38391, 8, 56
    , 43257, 128, 96
    , 46957, 64, 104
    , 49553, 144, 112
    , 54125, 120, 112
  ))

  Sprites.setLocations("umbrella_block", listOf(
    1105, 120, 120
    , 3012, 216, 112
    , 44041, 80, 72))
  Sprites.setLocations("plug_block", listOf(
    3012, 224, 112))
  Sprites.setLocations("cheese_block", listOf(
    6915, 184, 112
    , 2437, 48, 64))
  Sprites.setLocations("coat_block", listOf(
    6915, 120, 104
    , 36376, 16, 112))
  Sprites.setLocations("fish_block", listOf(
    6541, 80, 96))
  Sprites.setLocations("head_dress_block", listOf(
    6915, 112, 120
    , 28698, 40, 112))
  Sprites.setLocations("wrench_block", listOf(
    13225, 136, 112))
  Sprites.setLocations("ball_block", listOf(
    14646, 168, 120))
  Sprites.setLocations("axe_block", listOf(
    14848, 128, 120))
  Sprites.setLocations("bush_block", listOf(
    17732, 168, 104))
  Sprites.setLocations("sphinx_block", listOf(
    18227, 88, 120))
  Sprites.setLocations("sand_block", listOf(
    25632, 224, 120))
  Sprites.setLocations("salt_block", listOf(
    44041, 80, 72))
}
*/