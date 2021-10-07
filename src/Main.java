import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

class Main {
  public static final byte OFF = 0, ON = 1
      , OFF_OR_TRANSPARENT = 2, ON_OR_TRANSPARENT = 3
      , VALUE = 1, UNKNOWN = 2;
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF}, attrs = new int[768];
  
  protected static int blockX1, blockY1, blockY2, blockX2;
  
  private static Image difference() {
    BufferedImage image = Screen.toImage(false);
    BufferedImage backImage = Screen.backgroundToImage(false);
    int width = image.getWidth(), height = image.getHeight();
    int width2 = width + 8, height2 = height + 8;
    byte[] data = new byte[width2 * height2];
    int ix1 = width2, iy1 = height2, ix2 = 0, iy2 = 0;
    for(int i = 0; i < data.length; i++) data[i] = OFF_OR_TRANSPARENT;
    for(int y = 0; y < height; y++) {
      for(int x = 0; x < width; x++) {
        int address = (y + 4) * width2 + (x + 4);
        boolean imgPixel = (image.getRGB(x, y) & 1) == 1;
        boolean backPixel = (backImage.getRGB(x, y) & 1) == 1;
        if(imgPixel == backPixel) {
          data[address] = imgPixel ? ON_OR_TRANSPARENT : OFF_OR_TRANSPARENT;
        } else {
          data[address] = imgPixel ? ON : OFF;
          ix1 = Integer.min(ix1, x);
          iy1 = Integer.min(iy1, y);
          ix2 = Integer.max(ix2, x);
          iy2 = Integer.max(iy2, y);
        }
      }
    }
    return new Image(data, width2, height2, ix1 + 4, iy1 + 4, ix2 + 4, iy2 + 4);
  }
  
  private static final LinkedList<Image> images = new LinkedList<>();
  
  public static void main(String[] args) {
    try {
      process(327, 475);
      process(483, 666);
      process(754, 912);
      
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
            for(Image oldImage: images)
              if(oldImage.compareTo(image)) continue block;
            images.add(image);
          }
        }
      }
    }
  }
}
