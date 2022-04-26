import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.floor

class Background {
  private class Scope(val from: Int, val until: Int)

  val pixels: Array<Pixel>
  val image: BufferedImage?
  val fileName: String
  val name: String

  var frame: Int = -1
  var changed: Array<Int>? = null
  var total: Int = 0
  private val scopes: LinkedList<Scope> = LinkedList()

  var particlesColor: Int = white
  var particlesArea: Rect? = null
  var maxDifference: Int = MAX_BG_DIFFERENCE
  var changedColor: Int = -1

  constructor(pixels: Array<Pixel>) {
    this.pixels = pixels
    this.image = null
    this.fileName = ""
    this.name = ""
  }

  constructor(pixels: Array<Pixel>, image: BufferedImage?
              , fileName: String) {
    this.name = fileName.substring(0, fileName.indexOf("."));
    this.pixels = pixels
    this.image = image
    this.fileName = fileName
    this.frame = frame
    if(mode == Mode.FIND_PIXELS_TO_SKIP) changed = Array(PIXEL_SIZE) { 0 }
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

  private fun inScope(frame: Int): Boolean {
    for(scope in scopes) {
      if(frame < scope.from || frame >= scope.until) return false
    }
    return true
  }

  companion object {
    val backgrounds = LinkedList<Background>()

    @Throws(IOException::class)
    fun loadBackgrounds() {
      for(file in File("$project/backgrounds").listFiles()) {
        if(file.isDirectory) continue
        val image = ImageIO.read(file)
        val pixels = Array(PIXEL_SIZE) {Pixel.OFF}
        for(y in 0 until PIXEL_HEIGHT) {
          for(x in 0 until PIXEL_WIDTH) {
            val pos = x + y * PIXEL_WIDTH
            val pixel = image.getRGB(x, y)
            if(pixel and 0xFF < 0x80) {
              pixels[pos] = Pixel.OFF
            } else if(pixel and 0xFF00 >= 0x80) {
              pixels[pos] = Pixel.ON
            } else {
              pixels[pos] = Pixel.ANY
            }
          }
        }
        val repainted = File("$project/backgrounds/repainted/"
            + file.name)
        if(repainted.exists()) println("${file.name} is repainted")
        this.backgrounds.add(Background(pixels, if(repainted.exists())
          ImageIO.read(repainted) else null, file.name))
      }
    }

    var maxBackgroundDifference = -1

    fun findBackground(screen: Array<Pixel>, frame: Int
                       , only:Boolean): Background? {
      var minDifference = if(SHOW_BG_DIFFERENCE) 100000
      else MAX_BG_DIFFERENCE
      var minBackground: Background? = null
      for(background in backgrounds) {
        if(only && background.name != ONLY_BACKGROUND) continue

        val scopes = background.scopes
        if(!scopes.isEmpty()) {
          if(!background.inScope(frame)) continue
        }

        val max = if(SHOW_BG_DIFFERENCE) {
          100000
        } else {
          background.maxDifference
        }
        val difference = background.difference(screen, if(SHOW_BG_DIFFERENCE)
          100000 else max)
        if(difference < minDifference && difference < max) {
          minDifference = difference
          minBackground = background
          if(only) break
        }
      }

      if(minBackground == null) {
        if(!only) println("$frame is too different ($minDifference)");
        return null
      }

      if(maxBackgroundDifference < minDifference) {
        maxBackgroundDifference = minDifference
      }

      return minBackground
    }

    private fun get(name: String): Background? {
      for(background in backgrounds) {
        if(background.name == name) return background
      }
      return null
    }

    fun getBackground(name: String): Background {
      val bg = get(name)
      if(bg != null) return bg
      println("Background $name is not found")
      throw Exception("Background is not found")
    }

    fun composeBackground(frames: Int, backgroundOn: IntArray): Array<Pixel> {
      val minFrames = floor(PERCENT_ON * frames).toInt()
      val background = Array<Pixel>(PIXEL_SIZE) { Pixel.OFF }
      for(addr in 0 until PIXEL_SIZE)
        background[addr] = if(backgroundOn[addr] >= minFrames) Pixel.ON else
          Pixel.OFF
      return background
    }

    fun addBackground(pixels: Array<Pixel>) {
      backgrounds.add(Background(pixels))
    }

    fun setParticles(name: String, area: Rect, color: Int = white) {
      val bg = getBackground(name)
      bg.particlesArea = area
      bg.particlesColor = color
    }

    fun setMaxDifference(name: String, diff: Int) {
      getBackground(name).maxDifference = diff
    }

    fun setChangedPixelsColor(name: String, color: Int) {
      getBackground(name).changedColor = color
    }

    fun addScope(name: String, from: Int, to: Int) {
      getBackground(name).scopes.add(Scope(from, to))
    }
  }
}