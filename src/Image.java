
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Image extends Main {
  private final byte[] data;
  private final int width, height, x1, y1, x2, y2;

  public Image(byte[] data, int width, int height, int x1, int y1, int x2, int y2) {
    this.data = data;
    this.width = width;
    this.height = height;
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  private BufferedImage toBufferedImage() {
    BufferedImage image = new BufferedImage(width, height
        , BufferedImage.TYPE_INT_RGB);
    for(int y = 0; y < height; y++) {
      for(int x = 0; x < width; x++) {
        int colIndex;
        switch(data[x + y * width]) {
          case OFF: colIndex = 0; break;
          case ON: colIndex = 7; break;
          default: colIndex = 3; break;
        }
        image.setRGB(x, y, color[colIndex]);
      }
    }
    return image;
  }

  public boolean isLargeEnough() {
    return x2 - x1 >= 8 && y2 - y1 >= 8;
  }

  public boolean match(Image image) {
    int xSize = x2 - x1;
    int ySize = y2 - y1;
    int maxDx = image.width - xSize;
    int maxDy = image.height - ySize;
    for(int dy = 0; dy < maxDy; dy++) {
      d: for(int dx = 0; dx < maxDx; dx++) {
        int dx2 = dx - x1;
        int dy2 = dy - y1;
        for(int y = y1; y < y2; y++) {
          for(int x = x1; x < x2; x++) {
            byte value1 = data[x + y * width];
            if((value1 & UNKNOWN) != 0) continue;
            byte value2 = (byte) (image.data[x + dx2 + (y + dy2) * width] & 1);
            if(value1 != value2) continue d;
          }
        }

        for(int y = y1; y < y2; y++) {
          for(int x = x1; x < x2; x++) {
            byte value = image.data[x + dx2 + (y + dy2) * width];
            if(value < UNKNOWN) data[x + y * width] = value;
          }
        }
        return true;
      }
    }
    return false;
  }

  private static int outnum = 0;

  public void save() throws IOException {
    outnum++;
    File outputfile = new File("D:/temp2/output/"
        + String.format("%08d", outnum) + ".png");
    ImageIO.write(toBufferedImage(), "png", outputfile);
  }
}
