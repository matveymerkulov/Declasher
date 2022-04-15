import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.Double.max
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.HashMap

object Sprites {
  var minSpritePixels = -1
    private set
  private var minDetectionWidth = MAX_WIDTH
  private var minDetectionHeight = MAX_HEIGHT
  private var minDetectionPixels = MAX_WIDTH * MAX_HEIGHT
  val maxDetectionSize: String
    get() = "$minDetectionWidth x $minDetectionHeight $minDetectionPixels"
  var maxErrors = 0
    private set
  var maxDifference = 0.0
    private set

  private val spriteLists = LinkedList<SpriteList>()
  private val locations = HashMap<Int, LinkedList<SpritePos>>()

  private class SpriteList(val alwaysSingle: Boolean) {
    val sprites = LinkedList<Sprite>()
  }

  private class SpritePos(val dx: Int, val dy: Int, val errors: Int
                          , val matched: Int, val sprite: Sprite) {
    fun repaint(screen: Array<Pixel>, image: BufferedImage, remove: Boolean) {
      sprite.repaint(dx, dy, screen, image, remove)
    }
  }

  fun declash(screen: Array<Pixel>, image: BufferedImage, frame: Int
              , areas: LinkedList<ChangedArea>) {

    //System.out.print(width + "x" + height + ", ");

    for(list in spriteLists) {
      var best: SpritePos = SpritePos(
        0, 0, -1, 0, list.sprites.first
      );

      for(area in areas) {
        for(sprite in list.sprites) {
          best = sprite.check(best, area, frame, screen, list.alwaysSingle)
        }
        if(!list.alwaysSingle && best.errors >= 0) {
          process(best, screen, image, frame, area)
        }
      }
      if(list.alwaysSingle && best.errors >= 0) {
        process(best, screen, image, frame, null)
      }
    }
  }

  private fun process(best: SpritePos, screen: Array<Pixel>
                      , image: BufferedImage, frame: Int, area:ChangedArea?) {
    if(SHOW_DETECTION_AREA) {
      val gO = image.createGraphics()
      gO.color = Color.white
      gO.drawString("${100 * best.matched / best.sprite.pixelsQuantity}/${best.errors}"
        , best.dx, best.dy - 3)
    }

    if(mode == Mode.FIND_SPRITE_POSITION) {
      val map = best.sprite.positions
      val positions:LinkedList<Coords>
      if(map.containsKey(frame)) {
        positions = map[frame]!!
      } else {
        positions = LinkedList()
        map[frame] = positions
      }

      for(pos in positions) {
        if(best.dx == pos.x && best.dy == pos.y) return
      }

      positions.add(Coords(best.dx, best.dy))
      setSpritePos(frame, best.dx, best.dy, best.sprite)
      return
    }

    if(area != null) {
      val width = area.x2 - area.x1
      val height = area.y2 - area.y1

      maxErrors = Integer.max(maxErrors, best.errors)
      maxDifference = max(
        maxDifference,
        (1.0 * best.sprite.pixelsQuantity - best.matched) / best.sprite.pixelsQuantity
      )
      minDetectionWidth = Integer.min(minDetectionWidth, width)
      minDetectionHeight = Integer.min(minDetectionHeight, height)
      minDetectionPixels = Integer.min(minDetectionPixels, width * height)
    }

    best.repaint(screen, image, false)
  }

  private class Sprite(
    file: File,
    val minMatched: Double,
    val maxErrors: Int,
    val areaFunction: (Int) -> Rect?
  ) {
    val name: String
    var repainted: BufferedImage? = null
    val data: Array<SpritePixelType>
    val width: Int
    val height: Int
    val isBlock: Boolean
    val positions: HashMap<Int, LinkedList<Coords>> = HashMap()
    var pixelsQuantity = 0

    init {
      name = file.name
      val image = ImageIO.read(file)
      val repaintedFile = File("$project/repainted/$name")
      if(repaintedFile.exists()) {
        repainted = ImageIO.read(repaintedFile)
        println("$name is repainted")
      }
      isBlock = name.contains("_block")
      width = image.width
      height = image.height
      data = Array(width * height) { SpritePixelType.ANY }
      for(y in 0 until height) {
        val yAddr = y * width
        for(x in 0 until width) {
          when(image.getRGB(x, y) and 0xFFFFFF) {
            0 -> {
              data[yAddr + x] = SpritePixelType.OFF
              pixelsQuantity++
            }
            0xFFFFFF -> {
              data[yAddr + x] = SpritePixelType.ON
              pixelsQuantity++
            }
            0xFF00FF -> data[yAddr + x] = SpritePixelType.ANY
            else -> {
              throw Exception("Invalid color")
            }
          }
        }
      }
      if(minSpritePixels < 0 || pixelsQuantity < minSpritePixels) {
        minSpritePixels = pixelsQuantity
      }
    }

    fun check(bestVal: SpritePos, changed: ChangedArea, frame: Int
              , screen: Array<Pixel>, alwaysSingle: Boolean): SpritePos {
      var best = bestVal
      val area = areaFunction(frame) ?: return best
      val areaX1 = area.x shl 3
      val areaY1 = area.y shl 3
      val areaX2 = (area.x + area.width) shl 3
      val areaY2 = (area.y + area.height) shl 3

      val dy1 = changed.y1 - MIN_DETECTION_HEIGHT
      val dy2 = changed.y2 - height + MIN_DETECTION_HEIGHT
      for(dy in dy1 until dy2) {
        if(isBlock && (dy % 8) != 0) continue
        val spriteY1 = Integer.max(0, -dy)
        val spriteY2 = Integer.min(height, PIXEL_HEIGHT - dy)
        if(spriteY1 + dy < areaY1 || spriteY2 + dy >= areaY2) continue

        val areaHeight = spriteY2 - spriteY1
        if(areaHeight < MIN_DETECTION_HEIGHT) continue
        val dx1 = changed.x1 - MIN_DETECTION_WIDTH
        val dx2 = changed.x2 - width + MIN_DETECTION_WIDTH
        dx@ for(dx in dx1 until dx2) {
          if(isBlock && (dx % 8) != 0) continue

          val spriteX1 = Integer.max(0, -dx)
          val spriteX2 = Integer.min(width, PIXEL_WIDTH - dx)
          if(spriteX1 + dx < areaX1 || spriteX2 + dx >= areaX2) continue

          val areaWidth = spriteX2 - spriteX1
          if(areaWidth < MIN_DETECTION_WIDTH
            || areaWidth * areaHeight < MIN_DETECTION_PIXELS) continue
          var errors = 0
          var matched = 0
          var total = 0
          for(spriteY in spriteY1 until spriteY2) {
            val screenY = spriteY + dy
            val yScreen = screenY * PIXEL_WIDTH
            val ySprite = spriteY * width
            for(spriteX in spriteX1 until spriteX2) {
              val screenX = spriteX + dx
              if(screenX < 0 || screenX >= PIXEL_WIDTH) continue
              val spriteValue = data[spriteX + ySprite]
              val screenValue = screen[screenX + yScreen]
              when(spriteValue) {
                SpritePixelType.ON -> {
                  if(screenValue == Pixel.ON) {
                    matched++
                  } else {
                    errors++
                  }
                  total++
                }
                SpritePixelType.OFF -> {
                  if(screenValue == Pixel.OFF) matched++
                  total++
                }
              }
              if(errors > maxErrors) continue@dx
            }
          }
          if(1.0 * matched / total < minMatched) continue
          if(best.errors < 0 || errors < best.errors) {
            best = SpritePos(dx, dy, errors, matched, this)
            if(errors == 0) return best
          }
        }
      }
      return best
    }

    fun repaint(dx: Int, dy: Int, screen: Array<Pixel>, image: BufferedImage
                , remove:Boolean) {
      for(spriteY in 0 until height) {
        val screenY = spriteY + dy
        if(screenY < 0 || screenY >= PIXEL_HEIGHT) continue
        val ySprite = spriteY * width
        for(spriteX in 0 until width) {
          val screenX = spriteX + dx
          if(screenX < 0 || screenX >= PIXEL_WIDTH) continue
          if(remove) {
            if(data[spriteX + ySprite] == SpritePixelType.ON) {
              screen[screenX + PIXEL_WIDTH * screenY] = Pixel.ANY
              if(SHOW_DETECTION_AREA) image.setRGB(screenX, screenY
                , 0xFFBF7F)
            }
          } else if(repainted == null) {
            when(data[spriteX + ySprite]) {
              SpritePixelType.ON -> {
                image.setRGB(screenX, screenY, SPRITE_COLOR)
              }
              SpritePixelType.OFF -> {
                image.setRGB(screenX, screenY, black)
              }
            }
          } else {
            val value = repainted!!.getRGB(spriteX, spriteY)
            if(value != -0xff01) image.setRGB(screenX, screenY, value)
          }
        }
      }
    }
  }

  private enum class SpritePixelType {
    ON, OFF, ANY
  }

  @Throws(IOException::class)
  fun load(fileName: String, minMatched: Double, maxErrors: Int
           , alwaysSingle: Boolean, areaFunction: (Int) -> Rect? = {defaultArea}) {
    val list = SpriteList(alwaysSingle)
    list.sprites.add(Sprite(File("$project/$fileName.png")
      , minMatched, maxErrors, areaFunction))
    spriteLists.add(list)
  }

  @Throws(IOException::class)
  fun loadSeveral(fileName: String, minMatched: Double, maxErrors: Int
                  , alwaysSingle: Boolean, areaFunction: (Int) -> Rect = {defaultArea}) {
    val folder = File("$project/$fileName")
    val list = SpriteList(alwaysSingle)
    for(file in folder.listFiles()) {
      list.sprites.add(Sprite(file, minMatched, maxErrors, areaFunction))
    }
    spriteLists.add(list)
  }

  fun highlightLocations(frame: Int, image: BufferedImage) {
    val gO = image.createGraphics()
    gO.color = Color.white
    val list = locations[frame]
    if(list != null) {
      for(pos in list) {
        gO.drawRect(pos.dx, pos.dy, pos.sprite.width, pos.sprite.height)
      }
    }
  }

  fun printLocations() {
    for(list in spriteLists) {
      for(sprite in list.sprites) {
        val name = sprite.name.removeSuffix(".png")
        var data = ""
        for((frame, coords) in sprite.positions) {
          for(pos in coords) {
            data += "$frame, ${pos.x}, ${pos.y}, "
          }
        }
        if(data.isEmpty()) continue
        data = data.removeSuffix(", ")
        println("Sprites.setLocations(\"$name\", listOf($data))")
      }
    }
  }

  fun setLocations(fileName: String, list: List<Int>) {
    val sprite = Sprite(File("$project/static/$fileName.png")
      , 0.0, 0, {defaultArea})
    for(n in list.indices step 3) {
      setSpritePos(list[n], list[n + 1], list[n + 2], sprite)
    }
  }

  private fun setSpritePos(frame: Int, x: Int, y: Int, sprite: Sprite) {
    val frame = frame
    var positions:LinkedList<SpritePos>
    if(locations.containsKey(frame)) {
      positions = locations[frame]!!
    } else {
      positions = LinkedList()
      locations[frame] = positions
    }

    positions += SpritePos(x, y, 0, 0, sprite)
  }

  fun removeStatic(frame: Int, screen: Array<Pixel>, image: BufferedImage) {
    val list = locations[frame] ?: return
    for(pos in list) pos.repaint(screen, image, true)
  }
}