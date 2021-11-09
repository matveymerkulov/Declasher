import org.tukaani.xz.SeekableFileInputStream
import org.tukaani.xz.SeekableXZInputStream
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.floor

object Screen : Main() {
  private const val FRAME_SIZE = BYTE_SIZE + ATTR_SIZE
  private val backgroundOn = IntArray(PIXEL_SIZE)
  private var attrs: IntArray = IntArray(ATTR_SIZE)
  private var `in`: SeekableXZInputStream? = null

  @Throws(IOException::class)
  fun init() {
    val file = SeekableFileInputStream(project + "video.xz")
    `in` = SeekableXZInputStream(file)
    loadBackgrounds()
  }

  // loading
  @Throws(IOException::class)
  private fun load(num: Int): BooleanArray {
    val data = BooleanArray(PIXEL_SIZE)
    val byteScreen = ByteArray(FRAME_SIZE)
    `in`!!.seek((num * FRAME_SIZE).toLong())
    if(`in`!!.read(byteScreen, 0, FRAME_SIZE) < FRAME_SIZE) throw IOException()
    attrs = IntArray(ATTR_SIZE)
    for(x in 0 until ATTR_SIZE) {
      val value = byteScreen[BYTE_SIZE or x].toInt()
      val bright = if(value and 64 == 0) 0 else 136
      attrs[x] = value and 7 or (value and 56 shl 1) or bright
    }
    main@ for(part in 0..2) {
      val partSource = part shl 11
      val partDestination = part shl 3
      for(y in 0..7) {
        val ySource = partSource or (y shl 5)
        var yDestination = partDestination or y
        if(yDestination < AREA_Y) continue
        if(yDestination >= MAX_AREA_Y) break@main
        yDestination = yDestination - AREA_Y shl 3
        for(yy in 0..7) {
          val yySource = ySource or (yy shl 8)
          val yyDestination = (yDestination or yy) * AREA_WIDTH
          for(x in 0 until AREA_WIDTH) {
            val source = yySource or x + AREA_X
            val destination = yyDestination + x shl 3
            var byteValue = byteScreen[source].toInt()
            for(xx in 7 downTo 0) {
              val addr = destination or xx
              val pixelValue = byteValue and 1 > 0
              data[addr] = pixelValue
              byteValue = byteValue shr 1
            }
          }
        }
      }
    }
    return data
  }

  @Throws(IOException::class)
  fun loadBackgrounds() {
    for(file in File(project + "backgrounds").listFiles()) {
      if(file.isDirectory) continue
      val image = ImageIO.read(file)
      val pixels = BooleanArray(PIXEL_SIZE)
      for(y in 0 until PIXEL_HEIGHT) {
        for(x in 0 until PIXEL_WIDTH) {
          pixels[x + y * PIXEL_WIDTH] = image.getRGB(x, y) and 0xFF > 0x7F
        }
      }
      backgrounds.add(Background(pixels, null, file.name))
    }
  }

  @Throws(IOException::class)
  fun saveBackgrounds() {
    for(background in backgrounds) {
      val number = background.fileName.substring(0
          , background.fileName.indexOf('.')).toInt()
      load(number + 1)
      saveImage(toImage(background.values, null), background.fileName)
    }
  }

  private fun findBackground(screen: BooleanArray?): Background? {
    var minDifference = PIXEL_SIZE
    var minBackground: Background? = null
    for(background in backgrounds) {
      val difference = background.difference(screen)
      if(difference < minDifference) {
        minDifference = difference
        minBackground = background
      }
    }
    //System.out.println("Min difference is " + minDifference);
    return minBackground
  }

  private val backgrounds = LinkedList<Background>()
  private fun composeBackground(frames: Int): BooleanArray {
    val minFrames = floor(PERCENT_ON * frames).toInt()
    val background = BooleanArray(PIXEL_SIZE)
    for(addr in 0 until PIXEL_SIZE)
      background[addr] = backgroundOn[addr] >= minFrames
    return background
  }

  // processing
  @Throws(IOException::class)
  fun process(vararg start: Int) {
    for(j in start) process(j, -1, true)
  }

  @Throws(IOException::class)
  fun process(from: Int, to: Int, singleScreen: Boolean) {
    var firstFrame = from
    var oldScreen: BooleanArray? = null
    var frame = from
    while(frame <= to || to < 0) {
      var screen: BooleanArray = try {
        load(frame)
      } catch(ex: IOException) {
        break
      }
      if(oldScreen == null) {
        oldScreen = screen
        frame++
        continue
      }

      //final int SAME = 0, CHANGED = 1;
      //int[] pixels = new int[PIXEL_SIZE];
      var difference = 0
      for(addr in 0 until PIXEL_SIZE) {
        val isChanged = screen[addr] != oldScreen[addr]
        //pixels[addr] = isChanged ? CHANGED : SAME;
        if(screen[addr]) backgroundOn[addr]++
        if(isChanged) difference++
      }
      if(difference >= MIN_DIFFERENCE) {
        val frames = frame - firstFrame
        if(frames >= MIN_FRAMES) {
          oldScreen = composeBackground(frames)
          if(mode === Mode.EXTRACT_BACKGROUNDS) {
            if(findBackground(oldScreen) == null || SAVE_SIMILAR) {
              saveImage(toImage(oldScreen, null), firstFrame)
              backgrounds.add(Background(oldScreen))
              //saveImage(toImage(oldScreen, null), frame - 1);
              //saveImage(toImage(screen, null), frame);
              println("Saved background " + firstFrame
                  + " with difference " + difference + " and " + frames
                  + " frames")
            }
          } else {
            println("Sequence " + firstFrame + " - " + (frame - 1))
          }
        }
        if(singleScreen) return
        Arrays.fill(backgroundOn, 0)
        firstFrame = frame
      } else if(frame % FRAME_FREQUENCY == 0) {
        when(mode) {
          Mode.SHOW_DIFFERENCE -> {
            val background = findBackground(screen)
            if(background != null) saveImage(toImage(screen, background.values), frame)
          }
          Mode.TO_BLACK_AND_WHITE -> saveImage(toImage(screen, null), frame)
        }
      }
      oldScreen = screen
      frame++
    }
  }

  // conversion and saving
  fun toImage(screenData: BooleanArray?, bgData: BooleanArray?): BufferedImage {
    val image = BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT
        , BufferedImage.TYPE_INT_RGB)
    for(y in 0 until PIXEL_HEIGHT) {
      val ySource = y shl 8
      val yAttrSource = (y shr 3) + AREA_Y shl 5
      for(x in 0 until PIXEL_WIDTH) {
        val attr = attrs[yAttrSource or (x shr 3 + AREA_X)]
        val addr = ySource or x
        val value = screenData!![addr]
        var col: Int
        col = if(BLACK_AND_WHITE) {
          color[if(value) 15 else 0]
        } else {
          if(value) color[attr and 15] else color[attr shr 4 and 15]
        }
        if(bgData != null && value != bgData[addr]) col = color[11]
        image.setRGB(x, y, col)
      }
    }
    return image
  }

  @Throws(IOException::class)
  fun saveImage(image: BufferedImage, fileNumber: Int) {
    saveImage(image, String.format("%06d", fileNumber) + ".png")
  }

  @Throws(IOException::class)
  fun saveImage(image: BufferedImage, fileName: String) {
    val outputfile = File(OUT_DIR + fileName)
    ImageIO.write(x3(image), "png", outputfile)
  }

  // background
  private class Background {
    val values: BooleanArray?
    private val image: BufferedImage?
    val fileName: String

    constructor(values: BooleanArray?) {
      this.values = values
      image = null
      fileName = ""
    }

    constructor(values: BooleanArray?, image: BufferedImage?, fileName: String) {
      this.values = values
      this.image = image
      this.fileName = fileName
    }

    fun difference(screen: BooleanArray?): Int {
      var difference = 0
      for(i in 0 until PIXEL_SIZE) if(values!![i] != screen!![i]) difference++
      return difference
    }
  }
}