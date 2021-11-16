import Image.Comparsion
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*

object ImageExtractor {
  private val coordsStack = Stack<Coords>()
  val images = LinkedList<LinkedList<Image>>()

  @Throws(IOException::class)
  fun process(screen: BooleanArray, background: Background, frame: Int
              , image: BufferedImage) {
    if(background.skip) return

    val SAME = 0
    val CHANGED = 1
    val pixels = IntArray(PIXEL_SIZE)
    var changed = 0
    val backgroundValues = background.values
    for(addr in 0 until PIXEL_SIZE) {
      val isChanged = screen[addr] != backgroundValues[addr]
      pixels[addr] = if(isChanged) CHANGED else SAME
      if(isChanged) changed++
    }

    /*if(changed > MAX_CHANGED_PIXELS) {
      println("  Changed pixels of $frame is $changed")
      return
    }*/

    var imageNumber = 1
    for(y in 0 until PIXEL_HEIGHT) {
      val ySource = y shl 8
      x@ for(x in 0 until PIXEL_WIDTH) {
        val addr = ySource or x
        if(pixels[addr] == CHANGED) {
          var x2: Int = x
          var x1: Int = x2
          var y2: Int = y
          var y1: Int = y2
          imageNumber++
          pixels[addr] = imageNumber
          coordsStack.add(Coords(x, y))
          while(!coordsStack.empty()) {
            val coords = coordsStack.pop()
            val x0 = coords.x
            val y0 = coords.y
            for(dy in -MAX_DISTANCE..MAX_DISTANCE) {
              val yy = y0 + dy
              if(yy < 0 || yy >= PIXEL_HEIGHT) continue
              val yAddr = yy * PIXEL_WIDTH
              for(dx in -MAX_DISTANCE..MAX_DISTANCE) {
                if(dx == 0 && dy == 0) continue
                val xx = x0 + dx
                if(xx < 0 || xx >= PIXEL_WIDTH) continue
                val addr2 = x0 + dx + yAddr
                if(pixels[addr2] == CHANGED) {
                  x1 = Integer.min(x1, xx)
                  y1 = Integer.min(y1, yy)
                  x2 = Integer.max(x2, xx)
                  y2 = Integer.max(y2, yy)
                  pixels[addr2] = imageNumber
                  coordsStack.add(Coords(xx, yy))
                }
              }
            }
          }
          x2++
          y2++
          if(Image.Companion.hasAcceptableSize(x1, y1, x2, y2)) {
            if(x2 - x1 >= MAX_WIDTH || y2 - y1 >= MAX_HEIGHT) {
              println("  $frame - skipped big ${x2 - x1}" +
                  " x ${y2 - y1} changed area.")
              continue
            }
            if(mode === Mode.DETECT_MAX_SIZE) continue
            if(mode === Mode.EXTRACT_SPRITES) {
              val image = Image(pixels, screen, backgroundValues
                , x1, y1, x2, y2, imageNumber)
              for(list in images) {
                for(listImage in list) {
                  when(listImage.compareTo(image)) {
                    Comparsion.EQUAL -> {
                      listImage.incrementQuantity()
                      continue@x
                    }
                    Comparsion.SIMILAR -> {
                      list.add(image)
                      continue@x
                    }
                  }
                }
              }
              val newList = LinkedList<Image>()
              newList.add(image)
              images.add(newList)
            } else {
              repaint(pixels, imageNumber, screen, x1, y1, x2, y2, image
                , background.hasParticles)
              Sprites.declash(screen, x1, y1, x2, y2, image)
            }
          } else {
            repaint(pixels, imageNumber, screen, x1, y1, x2, y2, image
              , background.hasParticles)
          }
        }
      }
    }
  }

  private fun repaint(pixels: IntArray, imageNumber: Int, screen: BooleanArray
                      , x1: Int, y1: Int, x2: Int, y2: Int
                      , image: BufferedImage, hasParticles: Boolean) {
    for(y in y1 until y2) {
      val yAddr = y * PIXEL_WIDTH
      val yAttrSource = (y shr 3) + AREA_Y shl 5
      for(x in x1 until x2) {
        val addr = x + yAddr
        if(pixels[addr] == imageNumber) {
          image.setRGB(x, y, color[if(hasParticles) {
            if(screen[addr]) PARTICLE_COLOR else 0
          } else {
            val attr = Screen.attrs[yAttrSource or (x shr 3 + AREA_X)]
            if(screen[addr]) attr and 0xF else attr shr 4
          }])
        } else if(SHOW_DETECTION_AREA) {
          image.setRGB(x, y, color[3])
        }
      }
    }
  }

  @Throws(IOException::class)
  fun saveImages() {
    var listNum = 0
    for(list in images) {
      var maxWeight = -1
      var maxImage: Image = list.first
      for(image in list) {
        val size = image.weight
        if(maxWeight < size) {
          maxWeight = size
          maxImage = image
        }
      }
      listNum++
      maxImage.save(listNum)
    }
  }

  private class Coords(val x: Int, val y: Int)
}