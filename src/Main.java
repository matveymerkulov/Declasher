import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import javax.imageio.ImageIO;

class Main {
  private static final Chunk zeroChunk = new Chunk(true);
  
  private static class Chunk {
    private final int[] value = new int[8];
    private int quantity = 1;
    private boolean used;

    private Chunk(boolean used) {
      this.used = used;
    }

    @Override
    public boolean equals(Object obj) {
      final Chunk other = (Chunk) obj;
      return Arrays.equals(this.value, other.value);
    }

    private void add(Chunk chunk) {
      for(int i = 0; i < 8; i++) value[i] = (value[i] ^ chunk.value[i]) & 0xFF;
    }
  }
  
  private static class Image {
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

    private boolean isLargeEnough() {
      return x2 - x1 >= 8 && y2 - y1 >= 8;
    }
  
    private boolean match(Image image) {
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
    
    private void save() throws IOException {
      outnum++;
      File outputfile = new File("D:/temp2/output/"
          + String.format("%08d", outnum) + ".png");
      ImageIO.write(toBufferedImage(), "png", outputfile);
    }
  }
  
  private static final byte OFF = 0, ON = 1
      , OFF_OR_TRANSPARENT = 2, ON_OR_TRANSPARENT = 3
      , VALUE = 1, UNKNOWN = 2;
  
  private static final Chunk[] chunks = new Chunk[768];
  private static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF}, attrs = new int[768];
    
  private static void loadScreen(int num) throws IOException {
    File source = new File("source/image-" + String.format("%08d", num)
        + ".scr");
    FileInputStream input = new FileInputStream(source);
    byte[] byteScreen = new byte[6912];
    input.read(byteScreen);
    
    for(int x = 0; x < 768; x++) attrs[x] = byteScreen[6144 | x] & 0xFF;
    
    for(int part = 0; part < 3; part++) {
      int partSource = (part << 11);
      int partDestination = part << 8;
      for(int y = 0; y < 8; y++) {
        int ySource = partSource + (y << 5);
        int yDestination = partDestination | (y << 5);
        for(int x = 0; x < 32; x++) {
          int xSource = ySource | x;
          int xDestination = yDestination | x;
          Chunk chunk = new Chunk(false);
          for(int yy = 0; yy < 8; yy++)
            chunk.value[yy] = byteScreen[xSource | (yy << 8)] & 0xFF;
          chunks[xDestination] = chunk;
        }
      }
    }
  }
  
  private static BufferedImage toImage(Chunk[] chunkArray, boolean colored) {
    int dx = x2 - x1 + 1, dy = y2 - y1 + 1;
    BufferedImage image = new BufferedImage(dx << 3, dy << 3
        , BufferedImage.TYPE_INT_RGB);
    int[] col = {color[0], color[7]};
    for(int y = 0; y < dy; y++) {
      int lineStart = (y + y1) << 5;
      int yPos = y << 3;
      for(int x = 0; x < dx; x++) {
        int addr = lineStart | (x + x1);
        if(colored) {
          int attr = attrs[addr];
          col[0] = color[(attr >> 3) & 7];
          col[1] = color[attr & 7];
        }
        Chunk chunk = chunkArray[addr];
        int xPos = x << 3;
        for(int yy = 0; yy < 8; yy++) {
          int val = chunk.value[yy];
          for(int xx = 7; xx >= 0; xx--) {
            image.setRGB(xPos | xx, yPos | yy, col[val & 1]);
            val = val >> 1;
          }
        }
      }
    }
    return image;
  }
  
  private static Image difference() {
    BufferedImage image = toImage(chunks, false);
    BufferedImage backImage = toImage(background, false);
    int width = image.getWidth(), height = image.getHeight();
    byte[] data = new byte[width * height];
    int ix1 = width, iy1 = height, ix2 = 0, iy2 = 0;
    for(int y = 0; y < height; y++) {
      for(int x = 0; x < width; x++) {
        int address = y * width + x;
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
    return new Image(data, width, height, ix1, iy1, ix2, iy2);
  }
  
  private static class ChunkList extends LinkedList<Chunk> {};
  
  private static final Chunk[] background = new Chunk[768];
  
  private static int x1, x2, y1, y2;
  
  private static boolean usedBlock(int i) {
    if(chunks[i].used) return true;
    
    if(chunks[i].equals(background[i])) {
      chunks[i] = zeroChunk;
      return true;
    }
    
    chunks[i].used = true;
    
    return false;
  }
  
  private static boolean findBlock(int i) {
    if(usedBlock(i)) return false;
    
    x1 = x2 = i & 31;
    y1 = y2 = i >> 5;
    
    spawn(x1, y1);
    
    return true;
  }
  
  private static void spawn(int x, int y) {
    x1 = Integer.min(x1, x);
    y1 = Integer.min(y1, y);
    x2 = Integer.max(x2, x);
    y2 = Integer.max(y2, y);
    
    if(x > 0) check(x - 1, y);
    if(y > 0) check(x, y - 1);
    if(x < 31) check(x + 1, y);
    if(y < 23) check(x, y + 1);
  }
  
  private static void check(int x, int y) {
    int i = x + (y << 5);
    if(usedBlock(i)) return;
    spawn(x, y);
  }
  
  public static void main(String[] args) {
    final int from = 754, to = 912;
    
    try {
      final ChunkList[] chunkList = new ChunkList[768];
      for(int i = 0; i < 768; i++) chunkList[i] = new ChunkList();
      
      for(int num = from; num <= to; num++) {
        loadScreen(num);
        chunk: for(int i = 0; i < 768; i++) {
          ChunkList list = chunkList[i];
          Chunk oldChunk = chunks[i];
          for(Chunk chunk: list) {
            if(chunk.equals(oldChunk)) {
              chunk.quantity++;
              continue chunk;
            }
          }
          list.add(oldChunk);
        }
      }
      
      for(int i = 0; i < 768; i++) {
        ChunkList list = chunkList[i];
        Chunk maxChunk = null;
        int maxQuantity = 1;
        for(Chunk chunk: list) {          
          if(chunk.quantity > maxQuantity) {
            maxChunk = chunk;
            maxQuantity = chunk.quantity;
          }
        }
        background[i] = maxChunk;
      }
      
      LinkedList<Image> images = new LinkedList<>();
      
      for(int num = from; num <= to; num++) {
        loadScreen(num);
        block: for(int i = 0; i < 768; i++) {
          if(findBlock(i)) {
            Image image = difference();
            if(image.isLargeEnough()) {
              for(Image oldImage: images)
                if(oldImage.match(image)) continue block;
              images.add(image);
            }
          }
        }
      }
      
      for(Image image: images) image.save();
    } catch (IOException e) {
      System.err.println("I/O error");
    }
  }
}
