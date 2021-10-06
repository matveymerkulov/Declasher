 
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
  public static void main(String[] args) {
    try {
      int[] color = new int[8];
      color[0] = 0x000000;
      color[1] = 0x0000FF;
      color[2] = 0xFF0000;
      color[3] = 0xFF00FF;
      color[4] = 0x00FF00;
      color[5] = 0x00FFFF;
      color[6] = 0xFFFF00;
      color[7] = 0xFFFFFF;
      
      File source = new File("source/image-00000001.scr");
      FileInputStream input = new FileInputStream(source);
      byte[] byteScreen = new byte[6912];
      input.read(byteScreen);
      int[] screen = new int[6912];
      for(int i = 0; i < 6912; i++) screen[i] = byteScreen[i] & 0xFF;
      
      BufferedImage image = new BufferedImage(256, 192
          , BufferedImage.TYPE_INT_RGB);
      int[] col = new int[2];
      for(int y = 0; y < 192; y++) {
        int lineStart = ((((y >> 3) & 0b111) | ((y & 0b111) << 3)
            | (y & 0b111000000)) << 5);
        for(int x = 0; x < 32; x++) {
          int attr = screen[6144 | ((y >> 3) << 5) | x];
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
      
      File outputfile = new File("D:/temp2/image.png");
      ImageIO.write(image, "png", outputfile);
    } catch (IOException e) {
      
    }
  }
}
