import java.awt.Color
import java.awt.Rectangle
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
    get() = "${minDetectionWidth.toString()} x $minDetectionHeight" +
        " $minDetectionPixels"
  private val sprites = LinkedList<LinkedList<Sprite>>()
  var maxErrors = 0
    private set
  var maxDifference = 0.0
    private set

  @Throws(IOException::class)
  fun load() {
    val folder = File("$project/sprites")
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

  fun declash(screen: BooleanArray, x1: Int, y1: Int, x2: Int, y2: Int
              , image: BufferedImage, frame: Int) {
    val width = x2 - x1
    val height = y2 - y1
    //System.out.print(width + "x" + height + ", ");
    for(list in sprites) {
      var bestDx = 0
      var bestDy = 0
      var bestErrors = -1
      var bestMatched = 0
      var bestSprite: Sprite = list.first
      list@ for(sprite in list) {
        val area = sprite.area[frame] ?: continue
        val areaX1 = area.x shl 3
        val areaY1 = area.y shl 3
        val areaX2 = (area.x + area.width) shl 3
        val areaY2 = (area.y + area.height) shl 3

        val spriteWidth = sprite.width
        val spriteHeight = sprite.height
        val dy1 = y1 - MIN_DETECTION_HEIGHT - spriteHeight
        val dy2 = y1 + height - MIN_DETECTION_HEIGHT
        for(dy in dy1..dy2) {
          val spriteY1 = Integer.max(0, -dy)
          val spriteY2 = Integer.min(spriteHeight, PIXEL_HEIGHT - dy)
          if(spriteY1 + dy < areaY1 || spriteY2 + dy >= areaY2) continue

          val areaHeight = spriteY2 - spriteY1
          if(areaHeight < MIN_DETECTION_HEIGHT) continue
          val dx1 = x1 + MIN_DETECTION_WIDTH - spriteWidth
          val dx2 = x1 + width - MIN_DETECTION_WIDTH
          dx@ for(dx in dx1..dx2) {
            val spriteX1 = Integer.max(0, -dx)
            val spriteX2 = Integer.min(spriteWidth, PIXEL_WIDTH - dx)
            if(spriteX1 + dx < areaX1 || spriteX2 + dx >= areaX2) continue

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
      maxDifference = max(maxDifference, (1.0 * bestSprite.pixelsQuantity
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
          val repainted = bestSprite.repainted[frame]
          if(repainted == null) {
            when(bestSprite.data[spriteX + ySprite]) {
              SpritePixelType.ON -> image.setRGB(screenX, screenY, SPRITE_COLOR)
              SpritePixelType.OFF -> image.setRGB(screenX, screenY, black)
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
    val data: Array<SpritePixelType>
    val repainted: DefaultMap<Int, BufferedImage>
    val area: DefaultMap<Int, Rect>
    val width: Int
    val height: Int
    var pixelsQuantity = 0
    val maxErrors: Int
    val minMatched: Double

    fun load(fileName: String):BufferedImage {
      return ImageIO.read(File("$project/repainted/$fileName"))
    }

    init {
      val name = file.name
      val repaintedFile = File("$project/repainted/$name")
      val defaultRepainted
          = if(repaintedFile.exists()) ImageIO.read(repaintedFile) else null
      if(defaultRepainted != null) println("$name is repainted")
      var repaintedMap = emptyMap<Int, BufferedImage>()
      var defaultArea = Rect(0, 0, 32, 18)
      var areaMap = emptyMap<Int, Rect>()
      when(name) {
        "arrow.png" -> {
          minMatched = 0.95
          maxErrors = 0
        } "island.png" -> {
          minMatched = 0.8
          maxErrors = 20
          val redIsland = load("island.png")
          val blueIsland = load("island2.png")
          val whiteIsland = load("island3.png")
          val islandArea = Rect(0, 16, 32, 2)
          repaintedMap = mapOf(3012 to blueIsland
            , 23687 to whiteIsland, 24350 to redIsland, 45276 to redIsland)
          areaMap = mapOf(3012 to islandArea
            , 23687 to islandArea, 24350 to islandArea
            , 45276 to Rect(14, 14, 8, 4))
        } else -> {
          minMatched = 0.9
          maxErrors = 15
        }
      }

      repainted = DefaultMap(defaultRepainted, repaintedMap)
      area = DefaultMap(defaultArea, areaMap)
      val image = ImageIO.read(file)
      width = image.width
      height = image.height
      data = Array<SpritePixelType>(width * height) { SpritePixelType.ANY }
      for(y in 0 until height) {
        val yAddr = y * width
        for(x in 0 until width) {
          when(image.getRGB(x, y) and 0xFFFFFF) {
            0 -> {
              data[yAddr + x] = SpritePixelType.OFF
              pixelsQuantity++
            } 0xFFFFFF -> {
              data[yAddr + x] = SpritePixelType.ON
              pixelsQuantity++
            } 0xFF00FF -> data[yAddr + x] = SpritePixelType.ANY
            else -> throw Exception("Invalid color")
          }
        }
      }
      if(minSpritePixels < 0 || pixelsQuantity < minSpritePixels) {
        minSpritePixels = pixelsQuantity
      }
    }
  }
}