import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

object Sprites : Main() {
  var minSpritePixels = -1
    private set
  private var minDetectionWidth = MAX_WIDTH
  private var minDetectionHeight = MAX_HEIGHT
  private var minDetectionPixels = MAX_WIDTH * MAX_HEIGHT
  val maxDetectionSize: String
    get() = (minDetectionWidth.toString() + "x" + minDetectionHeight + ", "
        + minDetectionPixels)
  private val sprites = LinkedList<LinkedList<Sprite>>()
  var maxErrors = 0
    private set
  var maxDifference = 0.0
    private set

  @Throws(IOException::class)
  fun load() {
    val folder = File(project + "sprites")
    for(file in folder.listFiles()) {
      val list = LinkedList<Sprite>()
      if(file.isDirectory) {
        for(file2 in file.listFiles()) list.add(Sprite(file2))
      } else {
        list.add(Sprite(file))
      }
      sprites.add(list)
    }
  }

  fun declash(pixels: IntArray, imageNumber: Int, screen: BooleanArray, background: BooleanArray?, x1: Int, y1: Int, x2: Int, y2: Int, image: BufferedImage, repaint: Boolean) {
    if(repaint) {
      for(y in y1 until y2) {
        val yAddr = y * PIXEL_WIDTH
        for(x in x1 until x2) {
          val addr = x + yAddr
          if(pixels[addr] == imageNumber) {
            image.setRGB(x, y, if(screen[addr]) PARTICLE_COLOR else 0)
          } else if(SHOW_DETECTION_AREA) {
            image.setRGB(x, y, color[3])
          }
        }
      }
    }
    val width = x2 - x1
    val height = y2 - y1
    //System.out.print(width + "x" + height + ", ");
    for(list in sprites) {
      var bestDx = 0
      var bestDy = 0
      var bestErrors = -1
      var bestMatched = 0
      var bestSprite: Sprite? = null
      list@ for(sprite in list) {
        val spriteWidth = sprite.width
        val spriteHeight = sprite.height
        val dy1 = y1 - MIN_DETECTION_HEIGHT - spriteHeight
        val dy2 = y1 + height - MIN_DETECTION_HEIGHT
        for(dy in dy1..dy2) {
          val spriteY1 = Integer.max(0, -dy)
          val spriteY2 = Integer.min(spriteHeight, PIXEL_HEIGHT - dy)
          val areaHeight = spriteY2 - spriteY1
          if(areaHeight < MIN_DETECTION_HEIGHT) continue
          val dx1 = x1 + MIN_DETECTION_WIDTH - spriteWidth
          val dx2 = x1 + width - MIN_DETECTION_WIDTH
          dx@ for(dx in dx1..dx2) {
            val spriteX1 = Integer.max(0, -dx)
            val spriteX2 = Integer.min(spriteWidth, PIXEL_WIDTH - dx)
            val areaWidth = spriteX2 - spriteX1
            if(areaWidth < MIN_DETECTION_WIDTH
                || areaWidth * areaHeight < MIN_DETECTION_PIXELS) continue
            var errors = 0
            var matched = 0
            for(spriteY in spriteY1 until spriteY2) {
              val screenY = spriteY + dy
              val yScreen = screenY * PIXEL_WIDTH
              val ySprite = spriteY * spriteWidth
              for(spriteX in spriteX1 until spriteX2) {
                val screenX = spriteX + dx
                if(screenX < 0 || screenX >= PIXEL_WIDTH) continue
                val spriteValue = sprite.data[spriteX + ySprite]
                val screenValue = screen[screenX + yScreen]
                when(spriteValue) {
                  SpritePixelType.ON -> if(screenValue) matched++ else errors++
                  SpritePixelType.OFF -> if(!screenValue) matched++
                }
                if(errors > sprite.maxErrors) continue@dx
              }
            }
            if(1.0 * matched / sprite.pixelsQuantity < sprite.minMatched)
              continue
            if(bestErrors < 0 || errors < bestErrors) {
              bestDx = dx
              bestDy = dy
              bestErrors = errors
              bestSprite = sprite
              bestMatched = matched
              if(errors == 0) break@list
            }
          }
        }
      }
      if(bestErrors < 0) continue
      if(SHOW_DETECTION_AREA) {
        val gO = image.createGraphics()
        gO.color = Color.white
        gO.drawString("$bestMatched/$bestErrors", bestDx, bestDy - 3)
      }
      maxErrors = Integer.max(maxErrors, bestErrors)
      maxDifference = java.lang.Double.max(maxDifference
          , (1.0 * bestSprite!!.pixelsQuantity
          - bestMatched) / bestSprite.pixelsQuantity)
      minDetectionWidth = Integer.min(minDetectionWidth, width)
      minDetectionHeight = Integer.min(minDetectionHeight, height)
      minDetectionPixels = Integer.min(minDetectionPixels, width * height)
      val spriteWidth = bestSprite.width
      val spriteHeight = bestSprite.height
      for(spriteY in 0 until spriteHeight) {
        val screenY = spriteY + bestDy
        if(screenY < 0 || screenY >= PIXEL_HEIGHT) continue
        val ySprite = spriteY * spriteWidth
        for(spriteX in 0 until spriteWidth) {
          val screenX = spriteX + bestDx
          if(screenX < 0 || screenX >= PIXEL_WIDTH) continue
          val repainted = bestSprite.repainted
          if(repainted == null) {
            when(bestSprite.data[spriteX + ySprite]) {
              SpritePixelType.ON -> image.setRGB(screenX, screenY, SPRITE_COLOR)
              SpritePixelType.OFF -> image.setRGB(screenX, screenY, color[0])
            }
          } else {
            val value = repainted.getRGB(spriteX, spriteY)
            if(value != -0xff01) image.setRGB(screenX, screenY, value)
          }
        }
      }
    }
  }

  private enum class SpritePixelType {
    ON, OFF, ANY
  }

  private class Sprite(file: File) {
    var data: Array<SpritePixelType?>
    var repainted: BufferedImage? = null
    var width: Int
    var height: Int
    var pixelsQuantity = 0
    var maxErrors = 0
    var minMatched = 0.0

    init {
      val name = file.name
      when(name) {
        "arrow.png" -> {
          minMatched = 0.95
          maxErrors = 0
        }
        "island.png" -> {
          minMatched = 0.8
          maxErrors = 15
        }
        else -> {
          minMatched = 0.9
          maxErrors = 15
        }
      }
      val repaintedFile = File(project + "repainted/" + name)
      if(repaintedFile.exists()) {
        repainted = ImageIO.read(repaintedFile)
        println("$name is repainted")
      }
      val image = ImageIO.read(file)
      width = image.width
      height = image.height
      data = arrayOfNulls(width * height)
      for(y in 0 until height) {
        val yAddr = y * width
        for(x in 0 until width) {
          when(image.getRGB(x, y)) {
            -0x1000000 -> {
              data[yAddr + x] = SpritePixelType.OFF
              pixelsQuantity++
            }
            -0x1 -> {
              data[yAddr + x] = SpritePixelType.ON
              pixelsQuantity++
            }
            else -> data[yAddr + x] = SpritePixelType.ANY
          }
        }
      }
      if(minSpritePixels < 0 || pixelsQuantity < minSpritePixels) {
        minSpritePixels = pixelsQuantity
      }
    }
  }
}