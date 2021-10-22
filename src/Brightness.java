import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Brightness extends Main {
  private static final int brightness = 0x9F;
  
  public static void main(String[] args) throws IOException {
    final File folder = new File("D:/temp2/backgrounds");
    for(final File file: folder.listFiles()) {
      BufferedImage source = ImageIO.read(file);
      BufferedImage destination = new BufferedImage(source.getWidth()
          , source.getHeight(), BufferedImage.TYPE_INT_RGB);
      for(int y = 0; y < source.getHeight(); y++) {
        for(int x = 0; x < source.getWidth(); x++) {
          int value = source.getRGB(x, y);
          int col[] = new int[3];
          col[0] = value & 0xFF;
          col[1] = (value >> 8) & 0xFF;
          col[2] = (value >> 16) & 0xFF;
          value = 0xFF;
          for(int i = 2; i >= 0; i--) {
            if(col[i] > 0  && col[i] < 0xFF) col[i] = brightness;
            value = (value << 8) | col[i];
          }
          destination.setRGB(x, y, value);
        }
      }
      
      ImageIO.write(destination, "png"
          , new File(project + "backgrounds/" + file.getName() + ".png"));
    }
  }
}
