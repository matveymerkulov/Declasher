
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class Image extends Main {
  private class ImageEntry {
    private final Image image;
    private final int dx, dy;

    public ImageEntry(Image image, int dx, int dy) {
      this.image = image;
      this.dx = dx;
      this.dy = dy;
    }
  }
  
  private final byte[] data;
  private final int width, height, x1, y1, x2, y2;
  private int quantity = 0;
  private final LinkedList<ImageEntry> matched = new LinkedList<>();

  public Image(byte[] data, int width, int height, int x1, int y1, int x2
      , int y2) {
    this.data = data;
    this.width = width;
    this.height = height;
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    /*System.out.println(width + "x" + height + ", " + x1 + ", " + y1
        + ", " + x2 + ", " + y2);*/
  }

  public boolean hasAcceptableSize() {
    int innerWidth = x2 - x1, innerHeight = y2 - y1;
    return innerWidth >= minWidth && innerWidth <= maxWidth
        && innerHeight >= minHeight && innerHeight <= maxHeight;
  }

  private void add(Image image, int dx, int dy) {
    matched.add(new ImageEntry(image, dx, dy));
    quantity++;
  }
  
  private BufferedImage resizeImage(BufferedImage originalImage
      , int targetWidth, int targetHeight) {
    java.awt.Image resultingImage = originalImage.getScaledInstance(targetWidth
        , targetHeight, java.awt.Image.SCALE_DEFAULT);
    BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight
        , BufferedImage.TYPE_INT_RGB);
    outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
    return outputImage;
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
          case OFF_OR_TRANSPARENT: colIndex = 3; break;
          default: colIndex = colored ? 4 : 3; break;
        }
        image.setRGB(x, y, color[colIndex]);
      }
    }
    return resizeImage(image, width * 8, height * 8);
  }

  public boolean compareTo(Image image) {
    if(x2 - x1 == image.x2 - image.x1 && y2 - y1 == image.y2 - image.y1) {
      int dx = image.x1 - x1;
      int dy = image.y1 - y1;
      all: while(true) {
        for(int y = y1; y < y2; y++) {
          for(int x = x1; x < x2; x++) {
            byte value1 = data[x + y * width];
            if(value1 < UNKNOWN) {
              byte value2 = image.data[(x + dx) + (y + dy) * image.width];
              if(value1 != value2) break all;
            }
          }
        }
        quantity++;
        return true;
      }
    }
    
    int minDx = Integer.max(x2 - image.width, -image.x1);
    int maxDx = Integer.min(x1, width - image.x2);
    if(minDx > maxDx) return false;
    int minDy = Integer.max(y2 - image.height, -image.y1);
    int maxDy = Integer.min(y1, height - image.y2);
    if(minDy > maxDy) return false;
    for(int dy = minDy; dy <= maxDy; dy++) {
      int fromY = Integer.min(y1, image.y1 + dy);
      int toY = Integer.max(y2, image.y2 + dy);
      main: for(int dx = minDx; dx <= maxDx; dx++) {
        int fromX = Integer.min(x1, image.x1 + dx);
        int toX = Integer.max(x2, image.x2 + dx);
        for(int y = fromY; y < toY; y++) {
          for(int x = fromX; x < toX; x++) {
            byte value1 = data[x + y * width];
            byte value2 = image.data[(x - dx) + (y - dy) * image.width];
            switch(value1) {
              case OFF:
                if(value2 == ON || value2 == ON_OR_TRANSPARENT) continue main;
                break;
              case ON:
                if(value2 == OFF || value2 == OFF_OR_TRANSPARENT) continue main;
                break;
              case OFF_OR_TRANSPARENT:
                if(value2 == ON) continue main;
                break;
              case ON_OR_TRANSPARENT:
                if(value2 == OFF) continue main;
                break;
            }
          }
        }
        add(image, dx, dy);
        image.add(this, -dx, -dy);
        return false;
      }
    }
    return false;
  }

  private static int outnum = 0;

  public void save() throws IOException {
    merge();
    outnum++;
    File outputfile = new File("D:/temp2/output/"
        + String.format("%08d", outnum + 10000 * quantity) + ".png");
    ImageIO.write(toBufferedImage(), "png", outputfile);
  }

  private void merge() {
    for(ImageEntry entry: matched) {
      Image image = entry.image;
      int dx = entry.dx;
      int dy = entry.dy;
      for(int y = image.y1; y < image.y2; y++) {
        for(int x = image.x1; x < image.x2; x++) {
          int value = image.data[x + y * image.width];
          if(value < UNKNOWN)
            data[(x + dx) + (y + dy) * width] = (byte) (value & VALUE);
        }
      }
    }
  }

  @Override
  public String toString() {
    return (x2 - x1) + "x" + (y2 - y1);
  }
}
