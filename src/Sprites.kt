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
  var maxErrors = 0.0
    private set
  var maxDifference = 0.0
    private set

  private val spriteLists = LinkedList<SpriteList>()

  private class SpriteList(val alwaysSingle: Boolean) {
    val sprites = LinkedList<Sprite>()
  }

  private class SpritePos(val dx: Int, val dy: Int, var errors: Double
                          , val matched: Double, val sprite: Sprite) {
    fun repaint(screen: Area, image: BufferedImage, remove: Boolean) {
      sprite.repaint(dx, dy, screen, image, remove)
    }
  }

  fun declash(screen: Area, image: BufferedImage, name: String
              , areas: LinkedList<ChangedArea>, background: Background) {

    //System.out.print(width + "x" + height + ", ");

    for(list in spriteLists) {
      var best: SpritePos = SpritePos(
        0, 0, 2.0, -1.0, list.sprites.first
      )

      if(SHOW_DETECTION_AREA) {
        list.sprites.first.areas[name]?.draw(image)
      }

      for(area in areas) {
        for(sprite in list.sprites) {
          best = sprite.check(best, area, name, screen, background)
          if(!list.alwaysSingle && best.errors < 1.0) {
            process(best, screen, image, area, background)
            best.errors = 2.0
          }
        }
      }
      if(list.alwaysSingle && best.errors < 1.0) {
        process(best, screen, image, null, background)
      }
    }
  }

  private fun process(best: SpritePos, screen: Area
          , image: BufferedImage, area:ChangedArea?, background: Background) {
    if(SHOW_DETECTION_AREA) {
      val g = image.createGraphics()
      g.color = Color.white
      g.drawString("${format((10000 * best.matched
          / best.sprite.pixelsQuantity).toInt())}" +
          "/${format((10000 * best.errors).toInt())}"
        , best.dx, best.dy - 3)
    }

    if(area != null) {
      val width = area.x2 - area.x1
      val height = area.y2 - area.y1

      maxErrors = max(maxErrors, best.errors)
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
    val maxErrors: Double,
    val spriteColor: Int,
    mirrored: Boolean,
    name: String,
    val areas: DefaultMap<String, Rect?>
  ) {
    var repainted: BufferedImage? = null
    val data: Array<Pixel>
    val width: Int
    val height: Int
    val isBlock: Boolean
    var pixelsQuantity = 0

    init {
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
        data = Array(width * height) { Pixel.ANY }

        for(y in 0 until height) {
          val yAddr = y * width
          for(x in 0 until width) {
            val pixel = image.getRGB(x, y)
            val xx = yAddr + if(mirrored) width - 1 - x else x
            if(pixel and 0xFF < 0x80) {
              data[xx] = Pixel.OFF
              pixelsQuantity++
            } else if(pixel and 0xFF00 < 0x8000) {
              data[xx] = Pixel.ANY
            } else {
              data[xx] = Pixel.ON
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
              , screen: Area, background: Background): SpritePos {
      var best = bestVal
      val area = areas[background.name] ?: return best
      val areaX1 = area.x
      val areaY1 = area.y
      val areaX2 = area.x + area.width
      val areaY2 = area.y + area.height

      val dy1 = changed.y1 - BORDER_SIZE
      val dy2 = changed.y2 - height + BORDER_SIZE
      for(dy in dy1 until dy2) {
        if(isBlock && (dy % 8) != 0) continue
        val spriteY1 = Integer.max(0, -dy)
        val spriteY2 = Integer.min(height, PIXEL_HEIGHT - dy)
        if(spriteY1 + dy < areaY1 || spriteY2 + dy > areaY2) continue

        val areaHeight = spriteY2 - spriteY1
        val dx1 = changed.x1 - BORDER_SIZE
        val dx2 = changed.x2 - width + BORDER_SIZE
        dx@ for(dx in dx1 until dx2) {
          if(isBlock && (dx % 8) != 0) continue

          val spriteX1 = Integer.max(0, -dx)
          val spriteX2 = Integer.min(width, PIXEL_WIDTH - dx)
          if(spriteX1 + dx < areaX1 || spriteX2 + dx > areaX2) continue

          val areaWidth = spriteX2 - spriteX1
          if(areaWidth * areaHeight < MIN_PIXELS) continue
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
              if(XOR) {
                val backgroundPixel = background.pixels[screenX + yScreen]
                val chang = screenPixel != backgroundPixel
                if((chang && spritePixel == Pixel.ON)
                  || (!chang && spritePixel != Pixel.ON)) {
                  matched++
                } else {
                  errors++
                }
                total++
              } else when(spritePixel) {
                Pixel.ON -> {
                  if(screenPixel == Pixel.ON) {
                    matched++
                  } else {
                    errors++
                  }
                  total++
                }
                Pixel.OFF -> {
                  if(screenPixel == Pixel.OFF) matched++
                  total++
                }
              }
              if(1.0 * errors / pixelsQuantity > maxErrors) continue@dx
            }
          }
          val matchedPercent = 1.0 * matched / total
          val errorPercent = 1.0 * errors / pixelsQuantity
          if(matchedPercent < minMatched) continue
          if(best.errors < 0 || errorPercent < best.errors) {
            best = SpritePos(dx, dy, errorPercent, matchedPercent, this)
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
            val screenPixel = screen.pixels[screenPos]
            when(data[spriteX + ySprite]) {
              Pixel.ON -> {
                if(screenPixel == Pixel.ON) {
                  image.setRGB(screenX, screenY, color[attr and 0xF])
                  screen.pixels[screenPos] = Pixel.ANY
                }
              }
              Pixel.OFF -> {
                image.setRGB(screenX, screenY, if(SHOW_DETECTION_AREA)
                  cyan else color[attr shr 4])
              }
              Pixel.ANY -> {
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
              Pixel.ON -> {
                image.setRGB(screenX, screenY, spriteColor)
              }
              Pixel.OFF -> {
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

  private fun oneOrBoth(sprites: LinkedList<Sprites.Sprite>, file: File
                        , minMatched: Double, maxErrors: Double, color: Int
                        , name: String, areas: DefaultMap<String, Rect?>) {
    sprites.add(Sprite(file, minMatched, maxErrors, color, false
      , name, areas))
    if(file.name.contains("_both.png")) {
      sprites.add(Sprite(file, minMatched, maxErrors, color, true, name
        , areas))
    }
  }

  @Throws(IOException::class)
  fun load(fileName: String, minMatched: Double, maxErrors: Double
           , alwaysSingle: Boolean, color: Int
           , default: Rect? = defaultArea
           , areas: Map<String, Rect?> = emptyMap()) {
    val list = SpriteList(alwaysSingle)
    oneOrBoth(list.sprites, File("$project/sprites/$fileName.png")
      , minMatched, maxErrors, color, fileName
      , DefaultMap(default, areas))
    spriteLists.add(list)
  }

  @Throws(IOException::class)
  fun loadSeveral(fileName: String, minMatched: Double, maxErrors: Double
                  , alwaysSingle: Boolean, color: Int = white
                  , default: Rect? = defaultArea
                  , areas: Map<String, Rect?> = emptyMap()) {
    val folder = File("$project/sprites/$fileName")
    val list = SpriteList(alwaysSingle)
    for(file in folder.listFiles()) {
      oneOrBoth(list.sprites, file, minMatched, maxErrors, color, fileName
        , DefaultMap(default, areas))
    }
    spriteLists.add(list)
  }

  fun setLocations(fileName: String, list: List<Int>) {
    val sprite = Sprite(File("$project/static/$fileName.png")
      , 0.0, 0.0, white, false, fileName
      , DefaultMap(defaultArea, emptyMap()))
    for(n in list.indices step 3) {
      val bg = Screen.getBackground(format(list[n]))
      val x0 = list[n + 1]
      val y0 = list[n + 2]
      val pixels = bg.pixels
      for(y in 0 until sprite.height) {
        for(x in 0 until sprite.width) {
          if(sprite.data[x + y * sprite.width] == Pixel.ON) {
            pixels[x0 + x + (y0 + y) * PIXEL_WIDTH] = Pixel.ANY
          }
        }
      }
    }
  }
}
