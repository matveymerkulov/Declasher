import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.Double.max
import java.util.*
import javax.imageio.ImageIO

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

  private class SpriteList(val alwaysSingle: Boolean) {
    val sprites = LinkedList<Sprite>()
  }

  private class SpritePos(val dx: Int, val dy: Int, var errors: Int
                          , val matched: Int, val sprite: Sprite) {
    fun repaint(screen: Area, image: BufferedImage, remove: Boolean) {
      sprite.repaint(dx, dy, screen, image, remove)
    }
  }

  fun declash(screen: Area, image: BufferedImage, name: String
              , areas: LinkedList<ChangedArea>) {

    //System.out.print(width + "x" + height + ", ");

    for(list in spriteLists) {
      var best: SpritePos = SpritePos(
        0, 0, -1, 0, list.sprites.first
      )

      if(SHOW_DETECTION_AREA) {
        list.sprites.first.areaFunction(name)?.draw(image)
      }

      for(area in areas) {
        for(sprite in list.sprites) {
          best = sprite.check(best, area, name, screen)
          if(!list.alwaysSingle && best.errors >= 0) {
            process(best, screen, image, area)
            best.errors = -1
          }
        }
      }
      if(list.alwaysSingle && best.errors >= 0) {
        process(best, screen, image, null)
      }
    }
  }

  private fun process(best: SpritePos, screen: Area
                      , image: BufferedImage, area:ChangedArea?) {
    if(SHOW_DETECTION_AREA) {
      val g = image.createGraphics()
      g.color = Color.white
      g.drawString("${100 * best.matched / best.sprite.pixelsQuantity}/${best.errors}"
        , best.dx, best.dy - 3)
    }

    if(area != null) {
      val width = area.x2 - area.x1
      val height = area.y2 - area.y1

      maxErrors = Integer.max(maxErrors, best.errors)
      maxDifference = max(maxDifference,(1.0 * best.sprite.pixelsQuantity
          - best.matched) / best.sprite.pixelsQuantity)
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
    val areaFunction: (String) -> Rect?
  ) {
    val name: String
    var repainted: BufferedImage? = null
    val data: Array<SpritePixelType>
    val width: Int
    val height: Int
    val isBlock: Boolean
    var pixelsQuantity = 0

    init {
      name = file.name
      try {
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
            val pixel = image.getRGB(x, y)
            if(pixel and 0xFF < 0x80) {
              data[yAddr + x] = SpritePixelType.OFF
              pixelsQuantity++
            } else if(pixel and 0xFF00 < 0x8000) {
              data[yAddr + x] = SpritePixelType.ANY
            } else {
              data[yAddr + x] = SpritePixelType.ON
              pixelsQuantity++
            }
          }
        }
        if(minSpritePixels < 0 || pixelsQuantity < minSpritePixels) {
          minSpritePixels = pixelsQuantity
        }
      } catch(ex: Exception) {
        throw Exception("Cannot load file $name")
      }
    }

    fun check(bestVal: SpritePos, changed: ChangedArea, name: String
              , screen: Area): SpritePos {
      var best = bestVal
      val area = areaFunction(name) ?: return best
      val areaX1 = area.x
      val areaY1 = area.y
      val areaX2 = area.x + area.width
      val areaY2 = area.y + area.height

      val dy1 = changed.y1 - MIN_DETECTION_HEIGHT
      val dy2 = changed.y2 - height + MIN_DETECTION_HEIGHT
      for(dy in dy1 until dy2) {
        if(isBlock && (dy % 8) != 0) continue
        val spriteY1 = Integer.max(0, -dy)
        val spriteY2 = Integer.min(height, PIXEL_HEIGHT - dy)
        if(spriteY1 + dy < areaY1 || spriteY2 + dy > areaY2) continue

        val areaHeight = spriteY2 - spriteY1
        if(areaHeight < MIN_DETECTION_HEIGHT) continue
        val dx1 = changed.x1 - MIN_DETECTION_WIDTH
        val dx2 = changed.x2 - width + MIN_DETECTION_WIDTH
        dx@ for(dx in dx1 until dx2) {
          if(isBlock && (dx % 8) != 0) continue

          val spriteX1 = Integer.max(0, -dx)
          val spriteX2 = Integer.min(width, PIXEL_WIDTH - dx)
          if(spriteX1 + dx < areaX1 || spriteX2 + dx > areaX2) continue

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
              val spritePixel = data[spriteX + ySprite]
              val screenPixel = screen.pixels[screenX + yScreen]
              when(spritePixel) {
                SpritePixelType.ON -> {
                  if(screenPixel == Pixel.ON) {
                    matched++
                  } else {
                    errors++
                  }
                  total++
                }
                SpritePixelType.OFF -> {
                  if(screenPixel == Pixel.OFF) matched++
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

    fun repaint(dx: Int, dy: Int, screen: Area, image: BufferedImage
                , remove:Boolean) {
      for(spriteY in 0 until height) {
        val screenY = spriteY + dy
        if(screenY < 0 || screenY >= PIXEL_HEIGHT) continue
        val ySprite = spriteY * width
        for(spriteX in 0 until width) {
          val screenX = spriteX + dx
          if(screenX < 0 || screenX >= PIXEL_WIDTH) continue
          val screenPos = screenX + PIXEL_WIDTH * screenY
          if(remove) {
            val yAttrSource = (screenY shr 3) shl 5
            val attr = screen.attrs[yAttrSource or (screenX shr 3)]
            when(data[spriteX + ySprite]) {
              SpritePixelType.ON -> {
                if(screen.pixels[screenPos] == Pixel.ON) {
                  image.setRGB(screenX, screenY, color[attr and 0xF])
                  screen.pixels[screenPos] = Pixel.ANY
                }
              }
              SpritePixelType.OFF -> {
                image.setRGB(screenX, screenY, if(SHOW_DETECTION_AREA)
                  cyan else color[attr shr 4])
              }
              SpritePixelType.ANY -> {
                if(SHOW_DETECTION_AREA) image.setRGB(screenX, screenY, cyan)
                /*image.setRGB(screenX, screenY, if(SHOW_DETECTION_AREA) {
                  cyan
                } else if(screen.pixels[screenPos] == Pixel.ON) {
                  color[attr and 0xF]
                } else {
                  color[attr shr 4]
                })*/
              }
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
            if(value < 0) image.setRGB(screenX, screenY, value)
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
           , alwaysSingle: Boolean
           , areaFunction: (String) -> Rect? = {defaultArea}) {
    val list = SpriteList(alwaysSingle)
    list.sprites.add(Sprite(File("$project/sprites/$fileName.png")
      , minMatched, maxErrors, areaFunction))
    spriteLists.add(list)
  }

  @Throws(IOException::class)
  fun loadSeveral(fileName: String, minMatched: Double, maxErrors: Int
                  , alwaysSingle: Boolean
                  , areaFunction: (String) -> Rect = {defaultArea}) {
    val folder = File("$project/sprites/$fileName")
    val list = SpriteList(alwaysSingle)
    for(file in folder.listFiles()) {
      list.sprites.add(Sprite(file, minMatched, maxErrors, areaFunction))
    }
    spriteLists.add(list)
  }

  fun setLocations(fileName: String, list: String) {
    val sprite = Sprite(File("$project/static/$fileName.png")
      , 0.0, 0) { defaultArea }
    for(n in list.split(";")) {
      val vars = list.split(",")
      val bg = Screen.getBackground(vars[0].trim())
      val x0 = vars[1].trim().toInt()
      val y0 = vars[2].trim().toInt()
      val pixels = bg.pixels
      for(y in 0 until sprite.height) {
        for(x in 0 until sprite.width) {
          if(sprite.data[x + y * sprite.width] == SpritePixelType.ON) {
            pixels[x0 + x + (y0 + y) * PIXEL_WIDTH] = Pixel.ANY
          }
        }
      }
    }
  }
}
