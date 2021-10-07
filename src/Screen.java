import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class Screen extends Main {
  protected static class Chunk {
    private final int[] value = new int[8];
    protected int quantity = 1;
    protected boolean used;

    protected Chunk(boolean used) {
      this.used = used;
    }

    public boolean equals(Chunk other) {
      return Arrays.equals(this.value, other.value);
    }
  }

  protected static final Chunk[] chunks = new Chunk[768]
      , background = new Chunk[768];
  
  public static void load(int num) throws IOException {
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
  
  public static BufferedImage toImage(boolean colored) {
    return toImage(chunks, colored);
  }
  
  public static BufferedImage backgroundToImage(boolean colored) {
    return toImage(background, colored);
  }
  
  private static BufferedImage toImage(Chunk[] array, boolean colored) {
    int dx = blockX2 - blockX1 + 1, dy = blockY2 - blockY1 + 1;
    BufferedImage image = new BufferedImage(dx << 3, dy << 3
        , BufferedImage.TYPE_INT_RGB);
    int[] col = {color[0], color[7]};
    for(int y = 0; y < dy; y++) {
      int lineStart = (y + blockY1) << 5;
      int yPos = y << 3;
      for(int x = 0; x < dx; x++) {
        int addr = lineStart | (x + blockX1);
        if(colored) {
          int attr = attrs[addr];
          col[0] = color[(attr >> 3) & 7];
          col[1] = color[attr & 7];
        }
        Chunk chunk = array[addr];
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
}
