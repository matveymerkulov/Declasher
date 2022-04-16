import org.tukaani.xz.SeekableFileInputStream
import org.tukaani.xz.SeekableXZInputStream
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.floor

object Screen {
  private val backgroundOn = IntArray(PIXEL_SIZE)
  private var inStream: SeekableXZInputStream? = null

  @Throws(IOException::class)
  fun init() {
    val file = SeekableFileInputStream("$project/video.xz")
    inStream = SeekableXZInputStream(file)
    loadBackgrounds()
  }

  // loading
  @Throws(IOException::class)
  private fun load(num:Int, area: Rect): Area {
    val data = Array<Pixel>(area.pixelSize()) {Pixel.OFF}
    val attrs = IntArray(area.size())
    val byteScreen = ByteArray(FRAME_SIZE)
    inStream!!.seek((num * FRAME_SIZE).toLong())
    if(inStream!!.read(byteScreen, 0, FRAME_SIZE) < FRAME_SIZE)
      throw IOException()
    val attrOffset = BYTE_SIZE + (area.y shl 5)
    for(x in 0 until area.size()) {
      val value = byteScreen[x + attrOffset].toInt()
      val bright = if(value and 0b1000000 == 0) 0 else 0b10001000
      attrs[x] = value and 0b111 or (value and 0b111000 shl 1) or bright
    }
    main@ for(part in 0..2) {
      val partSource = part shl 11
      val partDestination = part shl 3
      for(y in 0 until 8) {
        val ySource = partSource or (y shl 5)
        var yDestination = partDestination or y
        if(yDestination < area.y) continue
        if(yDestination >= area.y + area.height) break@main
        yDestination = yDestination - area.y shl 3
        for(yy in 0 until 8) {
          val yySource = ySource or (yy shl 8)
          val yyDestination = (yDestination or yy) * area.width
          for(x in 0 until area.width) {
            val source = yySource or x + area.x
            val destination = yyDestination + x shl 3
            var byteValue = byteScreen[source].toInt()
            for(xx in 7 downTo 0) {
              val addr = destination or xx
              val pixelValue = if(byteValue and 1 > 0) Pixel.ON else Pixel.OFF
              data[addr] = pixelValue
              byteValue = byteValue shr 1
            }
          }
        }
      }
    }
    return Area(data, attrs, area)
  }

  @Throws(IOException::class)
  fun loadBackgrounds() {
    for(file in File("$project/backgrounds").listFiles()) {
      if(file.isDirectory) continue
      val image = ImageIO.read(file)
      val pixels = Array<Pixel>(PIXEL_SIZE) {Pixel.OFF}
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
      backgrounds.add(Background(pixels, if(repainted.exists())
        ImageIO.read(repainted) else null, file.name))
    }
  }

  @Throws(IOException::class)
  fun saveBackgrounds() {
    for(background in backgrounds) {
      val frame = background.frame
      val area = load(frame + 1, MAIN_SCREEN)
      val image = toImage(area, null)
      Sprites.highlightLocations(frame, image)
      saveImage(image, background.fileName)
    }
  }

  var maxBackgroundDifference = -1

  private fun findBackground(screen: Array<Pixel>, frame: Int): Background? {
    var minDifference = 100000
    var minBackground: Background? = null
    for(background in backgrounds) {
      val difference = background.difference(screen)
      if(difference < minDifference) {
        minDifference = difference
        minBackground = background
      }
    }
    if(minDifference > MAX_BG_DIFFERENCE) {
      println("$frame is too different ($minDifference)");
    } else if(minBackground != null && maxBackgroundDifference < minDifference) {
      maxBackgroundDifference = minDifference
      return minBackground
    }
    return null
  }

  private val backgrounds = LinkedList<Background>()
  private fun composeBackground(frames: Int): Array<Pixel> {
    val minFrames = floor(PERCENT_ON * frames).toInt()
    val background = Array<Pixel>(PIXEL_SIZE) {Pixel.OFF}
    for(addr in 0 until PIXEL_SIZE)
      background[addr] = if(backgroundOn[addr] >= minFrames) Pixel.ON else
        Pixel.OFF
    return background
  }

  // processing

  fun process() {
    process(0, -1)
  }

  fun process(backgroundNum: Int) {
    process(0, -1, backgroundNum)
  }

  @Throws(IOException::class)
  fun process(from: Int, to: Int, backgroundNum: Int = -1) {
    var firstFrame = from
    var oldScreen: Area? = null
    var frame = from
    while(frame <= to || to < 0) {
      val screen: Area = try {
        load(frame, MAIN_SCREEN)
      } catch(ex: IOException) {
        break
      }
      if(oldScreen == null) {
        oldScreen = screen
        frame++
        //System.out.println("$frame background switch")
        continue
      }

      if(mode === Mode.EXTRACT_BACKGROUNDS) {
        var difference = 0
        for(addr in 0 until PIXEL_SIZE) {
          val isChanged = screen.pixels[addr] != oldScreen.pixels[addr]
          if(screen.pixels[addr] == Pixel.ON) backgroundOn[addr]++
          if(isChanged) difference++
        }

        if(difference >= MIN_BG_DIFFERENCE) {
          System.out.println(
            "Processing sequence $firstFrame - $frame "
                + if(mode === Mode.EXTRACT_SPRITES) ", "
                + ImageExtractor.images.size else ""
          )
          val frames = frame - firstFrame
          if(frames >= MIN_FRAMES) {
            val background = composeBackground(frames)
            if(SAVE_SIMILAR || findBackground(background, frame) == null) {
              oldScreen = Area(background, oldScreen.attrs, oldScreen.area)
              saveImage(toImage(oldScreen, null), firstFrame)
              backgrounds.add(Background(background))
              //saveImage(toImage(oldScreen, null), frame - 1)
              //saveImage(toImage(screen, null), frame)
              println("Saved background $firstFrame with difference"
                  + " $difference and $frames frames")
            }
          }
          Arrays.fill(backgroundOn, 0)
          firstFrame = frame
        }
      } else if(mode == Mode.SCREENSHOTS) {
        saveImage(toImage(screen), frame)
      } else if(frame % FRAME_FREQUENCY == 0) {
        val background = findBackground(screen.pixels, frame)

        if(background != null) {
          if(backgroundNum < 0 || backgroundNum == background.frame) {
            if(mode == Mode.SHOW_DIFFERENCE) {
              saveImage(toImage(screen, background.pixels), frame)
            } else {
              val image = if(background.image == null) toImage(screen)
                  else copyImage(background.image)
              ImageExtractor.process(screen, background, frame, image)
              if(mode == Mode.DECLASH) {
                composeScreen(frame, image)
              }
            }
          }
        } else if(mode == Mode.DECLASH && backgroundNum < 0) {
          composeScreen(frame, toImage(screen))
        }
      }

      oldScreen = screen
      frame++
    }
  }

  private fun composeScreen(frame: Int, image: BufferedImage) {
    val screenImage = BufferedImage(SCREEN_WIDTH shl 3
      , SCREEN_HEIGHT shl 3, BufferedImage.TYPE_INT_RGB)
    pasteToImage(screenImage, load(frame, STATUS_BAR)
      , STATUS_BAR.x shl 3, STATUS_BAR.y shl 3)
    val g2d: Graphics2D = screenImage.createGraphics()
    g2d.drawImage(image, MAIN_SCREEN.x shl 3
      , MAIN_SCREEN.y shl 3, null)
    g2d.dispose()
    saveImage(screenImage, frame)
  }

  // conversion and saving
  private fun toImage(area: Area, bgData: Array<Pixel>? = null): BufferedImage {
    val image = BufferedImage(area.area.pixelWidth(), area.area.pixelHeight()
      , BufferedImage.TYPE_INT_RGB)
    pasteToImage(image, area, 0, 0, bgData)
    return image
  }

  private fun pasteToImage(image: BufferedImage, area: Area, x0: Int, y0: Int
                           , bgData: Array<Pixel>? = null) {
    val width = area.area.pixelWidth()
    val height = area.area.pixelHeight()
    for(y in 0 until height) {
      val ySource = y shl 8
      val yAttrSource = (y shr 3) shl 5
      for(x in 0 until width) {
        val attr = area.attrs[yAttrSource or (x shr 3)]
        val addr = ySource or x
        val value = area.pixels[addr]
        var col: Int
        col = if(BLACK_AND_WHITE) {
          if(value == Pixel.ON) white else black
        } else {
          if(value == Pixel.ON) color[attr and 0b1111] else
            color[attr shr 4 and 0b1111]
        }
        if(bgData != null && value != bgData[addr]) col = magenta
        image.setRGB(x + x0, y + y0, col)
      }
    }
  }

  @Throws(IOException::class)
  fun saveImage(image: BufferedImage, fileNumber: Int) {
    saveImage(image, String.format("%06d", fileNumber) + ".png")
  }

  @Throws(IOException::class)
  fun saveImage(image: BufferedImage, fileName: String) {
    val outputFile = File(OUT_DIR + fileName)
    ImageIO.write(x3(image), "png", outputFile)
  }
}