import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

object Brightness {
  private const val brightness = 0x9F

  @Throws(IOException::class)
  @JvmStatic
  fun main(args: Array<String>) {
    val folder = File("D:/temp2/backgrounds")
    for(file in folder.listFiles()) {
      val source = ImageIO.read(file)
      val destination = BufferedImage(source.width, source.height
          , BufferedImage.TYPE_INT_RGB)
      for(y in 0 until source.height) {
        for(x in 0 until source.width) {
          var value = source.getRGB(x, y)
          val col = IntArray(3)
          col[0] = value and 0xFF
          col[1] = value shr 8 and 0xFF
          col[2] = value shr 16 and 0xFF
          value = 0xFF
          for(i in 2 downTo 0) {
            if(col[i] in 1..0xfe) col[i] = brightness
            value = value shl 8 or col[i]
          }
          destination.setRGB(x, y, value)
        }
      }
      ImageIO.write(destination, "png"
          , File("$project/backgrounds/${file.name}.png"))
    }
  }
}