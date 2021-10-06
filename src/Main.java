import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class Main {
  public static Chunk zeroChunk = new Chunk();
  
  public static class Chunk {
    public final int[] value = new int[8];
    public int quantity = 1;

    @Override
    public boolean equals(Object obj) {
      final Chunk other = (Chunk) obj;
      if(!Arrays.equals(this.value, other.value)) {
        return false;
      }
      return true;
    }

    private void add(Chunk chunk) {
      for(int i = 0; i < 8; i++) value[i] = (value[i] ^ chunk.value[i]) & 0xFF;
    }
  }
  
  public static final Chunk[] chunks = new Chunk[768];
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF}, attrs = new int[768];
    
  public static void loadScreen(int num) throws IOException {
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
          Chunk chunk = new Chunk();
          for(int yy = 0; yy < 8; yy++)
            chunk.value[yy] = byteScreen[xSource | (yy << 8)] & 0xFF;
          chunks[xDestination] = chunk;
        }
      }
    }
  }
  
  public static BufferedImage toImage() {
    x1 = 0; y1 = 0; x2 = 31; y2 = 23;
    return toImagePart();
  }
  
  public static BufferedImage toImagePart() {
    int dx = x2 - x1 + 1, dy = y2 - y1 + 1;
    BufferedImage image = new BufferedImage(dx << 3, dy << 3
        , BufferedImage.TYPE_INT_RGB);
    int[] col = new int[2];
    for(int y = 0; y < dy; y++) {
      int lineStart = (y + y1) << 5;
      int yPos = y << 3;
      for(int x = 0; x < dx; x++) {
        int addr = lineStart | (x + x1);
        int attr = attrs[addr];
        col[0] = color[(attr >> 3) & 7];
        col[1] = color[attr & 7];
        Chunk chunk = chunks[addr];
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

  public static void saveImage(int num) throws IOException {
    File outputfile = new File("D:/temp2/output/"
        + String.format("%08d", num) + ".png");
    ImageIO.write(toImagePart(), "png", outputfile);
  }
  
  public static class ChunkList extends LinkedList<Chunk> {};
  
  public static Chunk[] background = new Chunk[768];
  
  public static int x1, x2, y1, y2;
  
  public static boolean usedBlock(int i) {
    if(chunks[i] == zeroChunk) return true;
    
    if(chunks[i].equals(background[i])) {
      chunks[i] = zeroChunk;
      return true;
    }
    
    if(attrs[i] == 0b001111) return true;
    
    attrs[i] = 0b001111;
    
    return false;
  }
  
  public static boolean findBlock(int i) {
    if(usedBlock(i)) return false;
    
    x1 = x2 = i & 31;
    y1 = y2 = i >> 5;
    
    spawn(x1, y1);
    
    return true;
  }
  
  public static void spawn(int x, int y) {
    x1 = Integer.min(x1, x);
    y1 = Integer.min(y1, y);
    x2 = Integer.max(x2, x);
    y2 = Integer.max(y2, y);
    
    if(x > 0) check(x - 1, y);
    if(y > 0) check(x, y - 1);
    if(x < 31) check(x + 1, y);
    if(y < 23) check(x, y + 1);
  }
  
  public static void check(int x, int y) {
    int i = x + (y << 5);
    if(usedBlock(i)) return;
    spawn(x, y);
  }
  
  public static void main(String[] args) {
    int from = 754, to = 912;
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
      
      int outnum = 0;
      for(int num = from; num <= to; num++) {
        loadScreen(num);
        for(int i = 0; i < 768; i++) {
          if(findBlock(i)) {
            outnum++;
            saveImage(outnum);
          }
        }
      }
    } catch (IOException e) {
      System.err.println("I/O error");
    }
  }
}
