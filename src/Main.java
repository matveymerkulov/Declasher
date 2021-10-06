 
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
  private static final int[] screen = new int[6144], attrs = new int[768]
      , color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
    
  public static void loadScreen(int num) throws IOException {
    File source = new File("source/image-" + String.format("%08d", num)
        + ".scr");
    FileInputStream input = new FileInputStream(source);
    byte[] byteScreen = new byte[6912];
    input.read(byteScreen);
    for(int x = 0; x < 768; x++) attrs[x] = byteScreen[6144 | x] & 0xFF;
    for(int part = 0; part < 3; part++) {
      int partPos = part << 11;
      for(int yy = 0; yy < 8; yy++) {
        int yySource = partPos + (yy << 8);
        int yyDest = partPos + (yy << 5);
        for(int y = 0; y < 8; y++) {
          int ySource = yySource + (y << 5);
          int yDest = yyDest + (y << 8);
          for(int x = 0; x < 32; x++) {
            screen[yDest | x] = byteScreen[ySource | x] & 0xFF;
          }
        }
      }
    }
  }
  
  public static void saveImage(String fileName) throws IOException {
    BufferedImage image = new BufferedImage(256, 192
        , BufferedImage.TYPE_INT_RGB);
    int[] col = new int[2];
    for(int y = 0; y < 192; y++) {
      int lineStart = y << 5;
      for(int x = 0; x < 32; x++) {
        int attr = attrs[((y >> 3) << 5) | x];
        col[0] = color[(attr >> 3) & 7];
        col[1] = color[attr & 7];
        int val = screen[lineStart | x];
        int xx = x << 3;
        for(int i = 7; i >= 0; i--) {
          image.setRGB(xx | i, y, col[val & 1]);
          val = val >> 1;
        }
      }
    }

    File outputfile = new File(fileName);
    ImageIO.write(image, "png", outputfile);
  }
  
  public static void main(String[] args) {
    try {
      loadScreen(1);
      saveImage("D:/temp2/image.png");
    } catch (IOException e) {
      
    }
  }
}
