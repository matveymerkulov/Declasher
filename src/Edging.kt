import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JFileChooser

fun processPixel(image: BufferedImage, x: Int, y: Int) {
  if(image.getRGB(x, y) and 0xFF00 < 0x8000) image.setRGB(x, y, 0)
}

fun main(args: Array<String>) {
  val fc = JFileChooser("sceptre/sprites")
  fc.showOpenDialog(null)
  val image = ImageIO.read(fc.selectedFile)
  for(y in 1 until image.height - 1) {
    for(x in 1 until image.width - 1) {
      if(image.getRGB(x, y) and 0xFF00 >= 0x8000) {
        processPixel(image, x - 1, y)
        processPixel(image, x + 1, y)
        processPixel(image, x, y - 1)
        processPixel(image, x, y + 1)
      }
    }
  }
  ImageIO.write(image, "png", fc.selectedFile)
}