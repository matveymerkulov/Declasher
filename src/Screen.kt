import Background.Companion.backgrounds
import Background.Companion.composeBackground
import Background.Companion.findBackground
import org.tukaani.xz.SeekableFileInputStream
import org.tukaani.xz.SeekableXZInputStream
import java.awt.Color
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

object Screen {
  private val backgroundOn = IntArray(PIXEL_SIZE)
  private var inStream: SeekableXZInputStream? = null

  @Throws(IOException::class)
  fun init() {
    val file = SeekableFileInputStream("$project/video.xz")
    inStream = SeekableXZInputStream(file)
    Background.loadBackgrounds()
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

  // processing

  fun process() {
    process(0, -1)
  }

  @Throws(IOException::class)
  fun process(from: Int, to: Int, frequency: Int = 1) {
    var firstFrame = from
    var oldScreen: Area? = null
    var frame = from
    while(frame <= to || to < 0) {
      if(frame % 1000 == 0) println(frame)

      val screen: Area = try {
        load(frame, MAIN_SCREEN)
      } catch(ex: IOException) {
        break
      }

      if(mode == Mode.SCREENSHOTS) {
        saveImage(toImage(load(frame, WHOLE_SCREEN), true), frame)
        frame++
        continue
      }

      if(oldScreen == null) {
        oldScreen = screen
        frame++
        continue
      }

      if(!ONLY_ABSENT || !File("D:\\output_final\\"
            + String.format("%06d", frame) + ".png").exists()) {
        if(mode == Mode.COLOR_BACKGROUNDS) {
          val background = Background.findBackground(screen.pixels, frame, false)
          if(background != null && background.frame < 0) {
            background.frame = frame
          }
        } else if(mode == Mode.EXTRACT_BACKGROUNDS) {
          println("$frame background switch")

          var difference = 0
          for(addr in 0 until PIXEL_SIZE) {
            val isChanged = screen.pixels[addr] != oldScreen.pixels[addr]
            if(screen.pixels[addr] == Pixel.ON) backgroundOn[addr]++
            if(isChanged) difference++
          }

          if(difference > MAX_BG_DIFFERENCE) {
            println("Processing sequence $firstFrame - $frame "
                  + if(mode === Mode.EXTRACT_SPRITES) ", "
                  + ImageExtractor.images.size else ""
            )
            val frames = frame - firstFrame
            if(frames >= MIN_FRAMES) {
              val background = composeBackground(frames, backgroundOn)
              if(SAVE_SIMILAR || findBackground(background, frame
                  , false) == null) {
                oldScreen = Area(background, oldScreen.attrs, oldScreen.area)
                saveImage(toImage(oldScreen, false), firstFrame)
                Background.addBackground(background)
                //saveImage(toImage(oldScreen, null), frame - 1)
                //saveImage(toImage(screen, null), frame)
                println("Saved background $firstFrame with difference"
                    + " $difference and $frames frames")
              }
            }
            Arrays.fill(backgroundOn, 0)
            firstFrame = frame
          }
        } else if(mode == Mode.FIND_PIXELS_TO_SKIP) {
          val background = findBackground(screen.pixels, frame
            , ONLY_BACKGROUND.isNotEmpty())
          if(background != null) {
            background.frame = frame
            background.total++
            for(addr in 0 until PIXEL_SIZE) {
              if(screen.pixels[addr] != background.pixels[addr]) {
                background.changed!![addr]++
              }
            }
          }
        } else if(frame % frequency == 0) {
          val background = findBackground(screen.pixels, frame
            , ONLY_BACKGROUND.isNotEmpty())
          if(background != null) {
            if(mode == Mode.SHOW_DIFFERENCE) {
              saveImage(toImage(screen, true, background.pixels), frame)
            } else {
              val image = if(background.image == null) {
                toImage(screen, true)
              } else {
                copyImage(background.image)
              }
              ImageExtractor.process(screen, background, frame, image)
              if(mode == Mode.DECLASH) {
                composeScreen(frame, image)
              }
            }
          } else if(mode == Mode.DECLASH && ONLY_BACKGROUND.isEmpty()) {
            composeScreen(frame, toImage(screen, true))
          }
        }
      }

      oldScreen = screen
      frame++
    }
    when(mode) {
      Mode.FIND_PIXELS_TO_SKIP -> {
        for(background in backgrounds) {
          var maxChanged = 0
          if(background.frame < 0) continue
          val area = load(background.frame, MAIN_SCREEN)
          for(addr in 0 until PIXEL_SIZE) {
            if(background.changed!![addr] > maxChanged) {
              maxChanged = background.changed!![addr]
            }
            if(1.0 * background.changed!![addr] / background.total
              >= MIN_BG_CHANGED) {
              area.pixels[addr] = Pixel.ANY
            } else {
              area.pixels[addr] = background.pixels[addr]
            }
          }
          println("Background ${background.name} max changed $maxChanged)" +
              " of ${background.total}")
          val image = toImage(area, false)
          saveImage(image, background.fileName)
        }
      } Mode.COLOR_BACKGROUNDS -> {
        for(background in backgrounds) {
          if(background.frame < 0) continue
          val area = load(background.frame, MAIN_SCREEN)
          val image = toImage(area, true)
          saveImage(image, background.fileName)
        }
      } Mode.EXTRACT_SPRITES -> {
        ImageExtractor.saveImages()
      }
    }

  }

  private fun composeScreen(frame: Int, image: BufferedImage) {
    val i = if(TWO_FRAMES) 1 else 0
    val screenWidth = WHOLE_SCREEN.pixelWidth()
    val screenHeight = WHOLE_SCREEN.pixelHeight()
    val width = screenWidth * 2 + 7
    val height = width * 9 / 16
    val newImage = if(TWO_FRAMES) {
      BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    } else {
      BufferedImage(screenWidth, screenHeight,BufferedImage.TYPE_INT_RGB)
    }

    val dy = if(TWO_FRAMES) (height - (SCREEN_HEIGHT shl 3) - 4) / 2 else 0

    val g2d: Graphics2D = newImage.createGraphics()
    if(TWO_FRAMES) {
      g2d.color = Color.black
      g2d.fillRect(0, 0, newImage.width, newImage.height)
      g2d.color = Color.white
      g2d.drawRect(0, dy - 2, newImage.width - 1
        , screenHeight + 3)
      g2d.drawRect(screenWidth + 3, dy - 2, 0
        , screenHeight + 3)

      val fm: FontMetrics = g2d.getFontMetrics(g2d.font)
      g2d.drawString("NEW", 130 - fm.stringWidth("NEW") / 2
        , 206 + dy)
      g2d.drawString("ORIGINAL", 130 + 256
          - fm.stringWidth("ORIGINAL") / 2, 206 + dy)
    }

    pasteToImage(newImage, load(frame, STATUS_BAR)
      , (STATUS_BAR.x shl 3) + i * 2, (STATUS_BAR.y shl 3) + dy, true)
    g2d.drawImage(image, (MAIN_SCREEN.x shl 3) + i * 2
      , (MAIN_SCREEN.y shl 3) + dy, null)
    if(TWO_FRAMES) {
      g2d.drawImage(toImage(load(frame, WHOLE_SCREEN), true)
        , screenWidth + 5 * i, dy, null)
    }

    g2d.dispose()
    saveImage(newImage, frame)
  }

  // conversion and saving
  private fun toImage(area: Area, colored: Boolean
                      , bgData: Array<Pixel>? = null): BufferedImage {
    val image = BufferedImage(area.area.pixelWidth(), area.area.pixelHeight()
      , BufferedImage.TYPE_INT_RGB)
    pasteToImage(image, area, 0, 0, colored, bgData)
    return image
  }

  private fun pasteToImage(image: BufferedImage, area: Area, x0: Int, y0: Int
                           , colored: Boolean, bgData: Array<Pixel>? = null) {
    val width = area.area.pixelWidth()
    val height = area.area.pixelHeight()
    for(y in 0 until height) {
      val ySource = y shl 8
      val yAttrSource = (y shr 3) shl 5
      for(x in 0 until width) {
        val attr = area.attrs[yAttrSource or (x shr 3)]
        val addr = ySource or x
        val value = area.pixels[addr]
        image.setRGB(x + x0, y + y0,
          if(BLACK_AND_WHITE) {
            if(value == Pixel.ON) white else black
          } else if(bgData != null && value != bgData[addr]
            && bgData[addr] != Pixel.ANY && value != Pixel.ANY) {
            magenta
          } else if(colored) {
            if(value == Pixel.ON) {
              color[attr and 0b1111]
            } else {
              color[attr shr 4 and 0b1111]
            }
          } else if(value == Pixel.ANY) {
            magenta
          } else if(value == Pixel.ON) {
            white
          } else {
            black
          }
        )
      }
    }
  }

  @Throws(IOException::class)
  fun saveImage(image: BufferedImage, fileNumber: Int) {
    saveImage(image, format(fileNumber) + ".png")
  }

  @Throws(IOException::class)
  fun saveImage(image: BufferedImage, fileName: String) {
    val outputFile = File(OUT_DIR + fileName)
    ImageIO.write(x3(image), "png", outputFile)
  }
}