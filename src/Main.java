import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class Main {
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
  
  public static void saveImage(String fileName) throws IOException {
    BufferedImage image = new BufferedImage(256, 192
        , BufferedImage.TYPE_INT_RGB);
    int[] col = new int[2];
    for(int y = 0; y < 24; y++) {
      int lineStart = y << 5;
      int yPos = y << 3;
      for(int x = 0; x < 32; x++) {
        int addr = lineStart | x;
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

    File outputfile = new File(fileName);
    ImageIO.write(image, "png", outputfile);
  }
  
  public static class ChunkList extends LinkedList<Chunk> {};
  
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
        int maxQuantity = 0;
        for(Chunk chunk: list) {
          if(chunk.quantity > maxQuantity) {
            maxChunk = chunk;
            maxQuantity = chunk.quantity;
          }
        }
        chunks[i] = maxChunk;
      }
      
      saveImage("D:/temp2/image.png");
    } catch (IOException e) {
      int a = 0;
    }
  }
}
