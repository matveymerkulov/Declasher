import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

class Image {
  private class ImageEntry(val image: Image, val dx: Int, val dy: Int)
  private enum class PixelType(var value: Boolean
  , var cannotBeTransparent: Boolean) {
    OFF(false, true),
    ON(true, true),
    OFF_OR_TRANSPARENT(false, false) {
      override fun removeTransparency(): PixelType {
        return OFF
      }
    },
    ON_OR_TRANSPARENT(true, false) {
      override fun removeTransparency(): PixelType {
        return ON
      }
    };

    open fun removeTransparency(): PixelType {
      return this
    }
  }

  private val data: Array<PixelType>
  private val width: Int
  private val height: Int
  private val x1: Int
  private val y1: Int
  private val x2: Int
  private val y2: Int
  private val matched = LinkedList<ImageEntry>()
  private var quantity = 1

  fun incrementQuantity() {
    quantity++
  }

  private val MIN_QUANTITY = 5
  val weight: Int
    get() = matched.size * 5 + quantity

  fun hasMatched(image: Image): Boolean {
    for(entry in matched) if(entry.image === image) return true
    return false
  }

  private constructor(data: Array<PixelType>, width: Int, height: Int, x1: Int
                      , y1: Int, x2: Int, y2: Int) {
    this.data = data
    this.width = width
    this.height = height
    this.x1 = x1
    this.y1 = y1
    this.x2 = x2
    this.y2 = y2
    /*System.out.println(width + "x" + height + ", " + x1 + ", " + y1
    + ", " + x2 + ", " + y2);*/
  }

  constructor(pixels: IntArray, screen: Array<Pixel>, background: Array<Pixel>?
              , x1: Int, y1: Int, x2: Int, y2: Int, imageNumber: Int) {
    val leftBorder = Integer.min(BORDER_SIZE_FROM, x1)
    val topBorder = Integer.min(BORDER_SIZE_FROM, y1)
    val rightBorder = Integer.min(BORDER_SIZE_TO, MAIN_SCREEN.pixelWidth() - x2)
    val bottomBorder = Integer.min(BORDER_SIZE_TO, MAIN_SCREEN.pixelHeight() - y2)
    width = leftBorder + x2 - x1 + rightBorder
    height = topBorder + y2 - y1 + bottomBorder
    this.x1 = leftBorder
    this.y1 = topBorder
    this.x2 = width - rightBorder
    this.y2 = height - bottomBorder
    data = Array<PixelType>(width * height) { PixelType.OFF }
    val dx = x1 - leftBorder
    val dy = y1 - topBorder
    for(y in 0 until height) {
      val ySource = y * width
      val yDestination = (y + dy) * MAIN_SCREEN.pixelWidth() + dx
      for(x in 0 until width) {
        val addr = yDestination + x
        if(pixels[addr] == imageNumber) {
          data[x + ySource] = if(screen[addr] == Pixel.ON) PixelType.ON else
            PixelType.OFF
        } else {
          data[x + ySource] = if(screen[addr] == Pixel.ON)
            PixelType.ON_OR_TRANSPARENT else PixelType.OFF_OR_TRANSPARENT
        }
      }
    }
  }

  private fun add(image: Image, dx: Int, dy: Int) {
    matched.add(ImageEntry(image, dx, dy))
  }

  enum class Comparsion {
    EQUAL, SIMILAR, DIFFERENT
  }

  fun compareTo(image: Image): Comparsion {
    if(x2 - x1 == image.x2 - image.x1 && y2 - y1 == image.y2 - image.y1) {
      val dx = image.x1 - x1
      val dy = image.y1 - y1
      all@ while(true) {
        for(y in y1 until y2) {
          for(x in x1 until x2) {
            val value1 = data[x + y * width]
            if(value1.cannotBeTransparent) {
              val value2 = image.data[x + dx + (y + dy) * image.width]
              if(value1 !== value2) break@all
            }
          }
        }
        return Comparsion.EQUAL
      }
    }
    val minDx = Integer.max(x2 - image.width, -image.x1)
    val maxDx = Integer.min(x1, width - image.x2)
    if(minDx > maxDx) return Comparsion.DIFFERENT
    val minDy = Integer.max(y2 - image.height, -image.y1)
    val maxDy = Integer.min(y1, height - image.y2)
    if(minDy > maxDy) return Comparsion.DIFFERENT
    for(dy in minDy..maxDy) {
      val fromY = Integer.min(y1, image.y1 + dy)
      val toY = Integer.max(y2, image.y2 + dy)
      main@ for(dx in minDx..maxDx) {
        val fromX = Integer.min(x1, image.x1 + dx)
        val toX = Integer.max(x2, image.x2 + dx)
        for(y in fromY until toY) {
          for(x in fromX until toX) {
            val value1 = data[x + y * width]
            val value2 = image.data[x - dx + (y - dy) * image.width]
            when(value1) {
              PixelType.OFF -> if(value2 === PixelType.ON
                  || value2 === PixelType.ON_OR_TRANSPARENT) continue@main
              PixelType.ON -> if(value2 === PixelType.OFF
                  || value2 === PixelType.OFF_OR_TRANSPARENT) continue@main
              PixelType.OFF_OR_TRANSPARENT
                  -> if(value2 === PixelType.ON) continue@main
              PixelType.ON_OR_TRANSPARENT
                  -> if(value2 === PixelType.OFF) continue@main
            }
          }
        }
        add(image, dx, dy)
        image.add(this, -dx, -dy)
        return Comparsion.SIMILAR
      }
    }
    return Comparsion.DIFFERENT
  }

  private fun toBufferedImage(colored: Boolean): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for(y in 0 until height) {
      for(x in 0 until width) {
        val colIndex = when(data[x + y * width]) {
          PixelType.OFF -> 0
          PixelType.ON -> 15
          PixelType.OFF_OR_TRANSPARENT -> 11
          else -> if(colored) 12 else 11
        }
        image.setRGB(x, y, color[colIndex])
      }
    }
    return if(RESIZED) resizeImage(image, width * 10
        , height * 10) else image
  }

  @Throws(IOException::class)
  fun save(num: Int) {
    if(weight < MIN_QUANTITY) return
    outnum++
    val outputfile = File(OUT_DIR
        + String.format("%03d", weight) + "_"
        + String.format("%03d", num) + "_"
        + String.format("%06d", outnum) + ".png")
    ImageIO.write(merge().toBufferedImage(true), "png", outputfile)
  }

  private fun merge(): Image {
    var fx1 = x1
    var fy1 = y1
    var fx2 = x2
    var fy2 = y2
    for(entry in matched) {
      val image = entry.image
      val dx = entry.dx
      val dy = entry.dy
      fx1 = Integer.min(image.x1 + dx, fx1)
      fy1 = Integer.min(image.y1 + dy, fy1)
      fx2 = Integer.max(image.x2 + dx, fx2)
      fy2 = Integer.max(image.y2 + dy, fy2)
      for(y in image.y1 until image.y2) {
        for(x in image.x1 until image.x2) {
          val value = image.data[x + y * image.width]
          if(value.cannotBeTransparent) {
            data[x + dx + (y + dy) * width] = value.removeTransparency()
          }
        }
      }
    }
    val fwidth = fx2 - fx1
    val fheight = fy2 - fy1
    val newData = Array<PixelType>(fwidth * fheight) { PixelType.ON }
    for(y in fy1 until fy2) {
      for(x in fx1 until fx2) {
        newData[x - fx1 + (y - fy1) * fwidth] = data[x + y * width]
      }
    }
    return Image(newData, fwidth, fheight, 0, 0, fwidth, fheight)
  }

  override fun toString(): String {
    return "${x2 - x1} x ${y2 - y1}"
  }

  companion object {
    private var maxImageWidth = 0
    private var maxImageHeight = 0
    private var maxImagePixels = 0
    val maxSize: String
      get() = "$maxImageWidth x $maxImageHeight, $maxImagePixels"

    fun hasAcceptableSize(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
      val innerWidth = x2 - x1
      val innerHeight = y2 - y1
      maxImageWidth = Integer.max(maxImageWidth, innerWidth)
      maxImageHeight = Integer.max(maxImageHeight, innerHeight)
      maxImagePixels = Integer.max(maxImagePixels
          , maxImageWidth * maxImageHeight)
      return innerWidth in MIN_WIDTH..MAX_WIDTH
          && innerHeight in MIN_HEIGHT..MAX_HEIGHT
    }

    private var outnum = 0
  }
}