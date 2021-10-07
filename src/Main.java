import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

class Main {
  public static final int borderSize = 4, minWidth = 8, maxWidth = 24
      , minHeight = 8, maxHeight = 32;
  public boolean colored = false;
  
  private static final int blockBorder = Math.floorDiv(borderSize + 7, 8);
  
  public static final byte OFF = 0, ON = 1
      , OFF_OR_TRANSPARENT = 2, ON_OR_TRANSPARENT = 3
      , VALUE = 1, UNKNOWN = 2;
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF}, attrs = new int[768];
  
  protected static int blockX1, blockY1, blockY2, blockX2;
  
  private static Image difference() {
    blockX1 = blockX1 > blockBorder ? blockX1 - blockBorder : 0;
    blockY1 = blockY1 > blockBorder ? blockY1 - blockBorder : 0;
    blockX2 = blockX2 < 31 - blockBorder ? blockX2 + blockBorder : 31;
    blockY2 = blockY2 < 23 - blockBorder ? blockY2 + blockBorder : 23;
    BufferedImage image = Screen.toImage(false);
    BufferedImage backImage = Screen.backgroundToImage(false);
    int width = image.getWidth(), height = image.getHeight();
    byte[] data = new byte[width * height];
    int x1 = width, y1 = height, x2 = 0, y2 = 0;
    for(int y = 0; y < height; y++) {
      for(int x = 0; x < width; x++) {
        int address = y * width + x;
        boolean imgPixel = (image.getRGB(x, y) & 1) == 1;
        boolean backPixel = (backImage.getRGB(x, y) & 1) == 1;
        if(imgPixel == backPixel) {
          data[address] = imgPixel ? ON_OR_TRANSPARENT : OFF_OR_TRANSPARENT;
        } else {
          data[address] = imgPixel ? ON : OFF;
          x1 = Integer.min(x1, x);
          y1 = Integer.min(y1, y);
          x2 = Integer.max(x2, x);
          y2 = Integer.max(y2, y);
        }
      }
    }
    x2++;
    y2++;
    /*System.out.println();
    System.out.println(width + "x" + height + ", " + x1 + ", " + y1
        + ", " + x2 + ", " + y2);*/
    
    int leftBorder = x1 < borderSize ? x1 : borderSize;
    int topBorder = y1 < borderSize ? y1 : borderSize;
    int rightBorder = width - x2 < borderSize ? width - x2 : borderSize;
    int bottomBorder = height - y2 < borderSize ? height - y2 : borderSize;
    int newWidth = leftBorder + (x2 - x1) + rightBorder;
    int newHeight = topBorder + (y2 - y1) + bottomBorder;
    byte[] newData = new byte[newWidth * newHeight];
    for(int y = 0; y < newHeight; y++) {
      int yy = y - topBorder + y1;
      for(int x = 0; x < newWidth; x++) {
        int xx = x - leftBorder + x1;
        newData[x + y * newWidth] = data[xx + yy * width];
      }
    }
    return new Image(newData, newWidth, newHeight, leftBorder, topBorder, 
        newWidth - rightBorder, newHeight - bottomBorder);
  }
  
  private static final LinkedList<Image> images = new LinkedList<>();
  
  public static void main(String[] args) {
    try {
      process(327, 475);
      process(483, 666);
      process(754, 912);
      process(923, 1101);
      process(1106, 1357);
      /*process(1369, 1546);
      process(1566, 1747);
      process(1840, 2025);
      process(2043, 2230);*/
      
      for(Image image: images) image.save();
    } catch (IOException e) {
      System.err.println("I/O error");
    }
  }

  private static void process(int from, int to) throws IOException {
    Background.compose(from, to);
    for(int num = from; num <= to; num++) {
      Screen.load(num);
      block: for(int i = 0; i < 768; i++) {
        if(Block.findBlock(i & 31, i >> 5)) {
          Image image = difference();
          if(image.hasAcceptableSize()) {
            //image.save();
            for(Image oldImage: images)
              if(oldImage.compareTo(image)) continue block;
            images.add(image);
          }
        }
      }
    }
  }
}
