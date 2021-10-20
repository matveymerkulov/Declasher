
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
  
  private static enum PixelType {
    OFF(false, true),
    ON(true, true),
    OFF_OR_TRANSPARENT(false, false) {
      @Override
      protected PixelType removeTransparency() {
        return OFF;
      }
    },
    ON_OR_TRANSPARENT(true, false) {
      @Override
      protected PixelType removeTransparency() {
        return ON;
      }
    };

    boolean value, cannotBeTransparent;

    private PixelType(boolean value, boolean cannotBeTransparent) {
      this.value = value;
      this.cannotBeTransparent = cannotBeTransparent;
    }

    protected PixelType removeTransparency() {
      return this;
    }    
  }
  
  private final PixelType[] data;
  private final int width, height, x1, y1, x2, y2;
  private final LinkedList<ImageEntry> matched = new LinkedList<>();
  
  public int getWeight() {
    return matched.size();
  }

  public boolean hasMatched(Image image) {
    for(ImageEntry entry: matched) if(entry.image == image) return true;
    return false;
  }

  private Image(PixelType[] data, int width, int height, int x1, int y1, int x2
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
    
  public Image(int[] pixels, boolean[] screen, boolean[] background
      , int x1, int y1, int x2, int y2, int imageNumber) {
    int leftBorder = Integer.min(BORDER_SIZE, x1);
    int topBorder = Integer.min(BORDER_SIZE, y1);
    int rightBorder = Integer.min(BORDER_SIZE, PIXEL_WIDTH - x2);
    int bottomBorder = Integer.min(BORDER_SIZE, PIXEL_HEIGHT - y2);
    this.width = leftBorder + x2 - x1 + rightBorder;
    this.height = topBorder + y2 - y1 + bottomBorder;
    this.x1 = leftBorder;
    this.y1 = topBorder;
    this.x2 = this.width - rightBorder;
    this.y2 = this.height - bottomBorder;
    this.data = new PixelType[this.width * this.height];
    int dx = x1 - leftBorder;
    int dy = y1 - topBorder;
    for(int y = 0; y < this.height; y++) {
      int ySource = y * this.width;
      int yDestination = (y + dy) * PIXEL_WIDTH + dx;
      for(int x = 0; x < this.width; x++) {
        int addr = yDestination + x;
        if(pixels[addr] == imageNumber) {
          this.data[x + ySource] = screen[addr] ? PixelType.ON : PixelType.OFF;
        } else {
          this.data[x + ySource] = screen[addr] ? PixelType.ON_OR_TRANSPARENT
              : PixelType.OFF_OR_TRANSPARENT;
        }
      }
    }
  }
  
  private static int maxImageWidth = 0, maxImageHeight = 0;
  
  public static void logMaxSize() {
    System.out.println("Max image is " + maxImageWidth + "x" + maxImageHeight);
  }
  
  public static boolean hasAcceptableSize(int x1, int y1, int x2, int y2) {
    int innerWidth = x2 - x1, innerHeight = y2 - y1;
    maxImageWidth = Integer.max(maxImageWidth, innerWidth);
    maxImageHeight = Integer.max(maxImageHeight, innerHeight);
    return innerWidth >= MIN_WIDTH && innerWidth <= MAX_WIDTH
        && innerHeight >= MIN_HEIGHT && innerHeight <= MAX_HEIGHT;
  }

  private void add(Image image, int dx, int dy) {
    matched.add(new ImageEntry(image, dx, dy));
  }
  
  public enum Comparsion {EQUAL, SIMILAR, OTHER}

  public Comparsion compareTo(Image image) {
    if(x2 - x1 == image.x2 - image.x1 && y2 - y1 == image.y2 - image.y1) {
      int dx = image.x1 - x1;
      int dy = image.y1 - y1;
      all: while(true) {
        for(int y = y1; y < y2; y++) {
          for(int x = x1; x < x2; x++) {
            PixelType value1 = data[x + y * width];
            if(value1.cannotBeTransparent) {
              PixelType value2 = image.data[(x + dx) + (y + dy) * image.width];
              if(value1 != value2) break all;
            }
          }
        }
        return Comparsion.EQUAL;
      }
    }
    
    int minDx = Integer.max(x2 - image.width, -image.x1);
    int maxDx = Integer.min(x1, width - image.x2);
    if(minDx > maxDx) return Comparsion.OTHER;
    int minDy = Integer.max(y2 - image.height, -image.y1);
    int maxDy = Integer.min(y1, height - image.y2);
    if(minDy > maxDy) return Comparsion.OTHER;
    for(int dy = minDy; dy <= maxDy; dy++) {
      int fromY = Integer.min(y1, image.y1 + dy);
      int toY = Integer.max(y2, image.y2 + dy);
      main: for(int dx = minDx; dx <= maxDx; dx++) {
        int fromX = Integer.min(x1, image.x1 + dx);
        int toX = Integer.max(x2, image.x2 + dx);
        for(int y = fromY; y < toY; y++) {
          for(int x = fromX; x < toX; x++) {
            PixelType value1 = data[x + y * width];
            PixelType value2 = image.data[(x - dx) + (y - dy) * image.width];
            switch(value1) {
              case OFF:
                if(value2 == PixelType.ON
                    || value2 == PixelType.ON_OR_TRANSPARENT) continue main;
                break;
              case ON:
                if(value2 == PixelType.OFF
                    || value2 == PixelType.OFF_OR_TRANSPARENT) continue main;
                break;
              case OFF_OR_TRANSPARENT:
                if(value2 == PixelType.ON) continue main;
                break;
              case ON_OR_TRANSPARENT:
                if(value2 == PixelType.OFF) continue main;
                break;
            }
          }
        }
        add(image, dx, dy);
        image.add(this, -dx, -dy);
        return Comparsion.SIMILAR;
      }
    }
    return Comparsion.OTHER;
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
          default: colIndex = COLORED ? 4 : 3; break;
        }
        image.setRGB(x, y, color[colIndex]);
      }
    }
    return RESIZED ? resizeImage(image, width * 10, height * 10) : image;
  }

  private static int outnum = 0;

  public void save(int num) throws IOException {
    if(getWeight() < MIN_QUANTITY) return;
    outnum++;
    File outputfile = new File("D:/temp2/output/"
        + String.format("%03d", getWeight()) + "_"
        + String.format("%03d", num) + "_"
        + String.format("%06d", outnum) + ".png");
    ImageIO.write(merge().toBufferedImage(), "png", outputfile);
  }

  private Image merge() {
    int fx1 = x1, fy1 = y1, fx2 = x2, fy2 = y2;
    for(ImageEntry entry: matched) {
      Image image = entry.image;
      int dx = entry.dx;
      int dy = entry.dy;
      fx1 = Integer.min(image.x1 + dx, fx1);
      fy1 = Integer.min(image.y1 + dy, fy1);
      fx2 = Integer.max(image.x2 + dx, fx2);
      fy2 = Integer.max(image.y2 + dy, fy2);
      for(int y = image.y1; y < image.y2; y++) {
        for(int x = image.x1; x < image.x2; x++) {
          PixelType value = image.data[x + y * image.width];
          if(value.cannotBeTransparent)
            data[(x + dx) + (y + dy) * width] = value.removeTransparency();
        }
      }
    }
    
    int fwidth = fx2 - fx1;
    int fheight = fy2 - fy1;
    PixelType[] newData = new PixelType[fwidth * fheight];
    for(int y = fy1; y < fy2; y++) {
      for(int x = fx1; x < fx2; x++) {
        newData[(x - fx1) + (y - fy1) * fwidth] = data[x + y * width];
      }
    }
    
    return new Image(newData, fwidth, fheight, 0, 0, fwidth, fheight);
  }

  @Override
  public String toString() {
    return (x2 - x1) + "x" + (y2 - y1);
  }
}
