
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

public class Packer {
  public static void main(String[] args)
      throws FileNotFoundException, IOException {
    
    final File outFile = new File("D:/Dropbox/Declasher/ratime/video.xz");

    final BufferedOutputStream outStream = new BufferedOutputStream(
        new FileOutputStream(outFile));
    LZMA2Options options = new LZMA2Options();
    options.setDictSize(4 * 1024 * 1024);
    XZOutputStream out = new XZOutputStream(outStream, options);
    
    int blockSize = 6912;
    
    int i = 0;
    while(true) {
      if(i % 100 == 0) System.out.println(i);
      
      byte[] buf = new byte[blockSize];
      final File inFile = new File("D:/temp2/ratime/"
          + String.format("%05d", i) + ".scr");
      if(!inFile.exists()) break;
      BufferedInputStream inStream = new BufferedInputStream(
          new FileInputStream(inFile));
      inStream.read(buf, 0, blockSize);
      out.write(buf, 0, blockSize);
      i++;
    }

    out.endBlock();
    out.finish();
    out.close();
    outStream.close();
  }
}
