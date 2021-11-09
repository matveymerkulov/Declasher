import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.io.*

object Packer {
  @Throws(FileNotFoundException::class, IOException::class)
  @JvmStatic
  fun main(args: Array<String>) {
    val outFile = File("D:/Dropbox/Declasher/ratime/video.xz")
    val outStream = BufferedOutputStream(
        FileOutputStream(outFile))
    val options = LZMA2Options()
    options.dictSize = 4 * 1024 * 1024
    val out = XZOutputStream(outStream, options)
    val blockSize = 6912
    var i = 0
    while(true) {
      if(i % 100 == 0) println(i)
      val buf = ByteArray(blockSize)
      val inFile = File("D:/temp2/ratime/"
          + String.format("%05d", i) + ".scr")
      if(!inFile.exists()) break
      val inStream = BufferedInputStream(
          FileInputStream(inFile))
      inStream.read(buf, 0, blockSize)
      out.write(buf, 0, blockSize)
      i++
    }
    out.endBlock()
    out.finish()
    out.close()
    outStream.close()
  }
}