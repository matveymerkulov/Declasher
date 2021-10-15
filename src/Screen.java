import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Screen extends Main {
  private static final int BYTE_SIZE = 32 * 24 * 8, ATTR_SIZE = BYTE_SIZE / 8
      , PIXEL_WIDTH = AREA_WIDTH << 3, PIXEL_HEIGHT = AREA_HEIGHT << 3
      , AREA_SIZE = AREA_WIDTH * AREA_HEIGHT, PIXEL_SIZE = AREA_SIZE << 6
      , MAX_AREA_X = AREA_X + AREA_WIDTH, MAX_AREA_Y = AREA_Y + AREA_HEIGHT;
  
  private static int[] background0, background1;
  private static final int[] attrs = new int[ATTR_SIZE];
  private static final boolean[] background = new boolean[PIXEL_SIZE];

  private static boolean[] load(int num, boolean withAttributes)
      throws IOException {
    boolean[] data = new boolean[PIXEL_SIZE];
    
    File image = new File("source/image-" + String.format("%08d", num)
        + ".scr");
    FileInputStream input = new FileInputStream(image);
    byte[] byteScreen = new byte[BYTE_SIZE + ATTR_SIZE];
    input.read(byteScreen);
    
    if(withAttributes)
      for(int x = 0; x < ATTR_SIZE; x++)
        attrs[x] = byteScreen[BYTE_SIZE | x] & 0xFF;
    
    main: for(int part = 0; part < 3; part++) {
      int partSource = part << 11;
      int partDestination = part << 3;
      for(int y = 0; y < 8; y++) {
        int ySource = partSource | (y << 5);
        int yDestination = partDestination | y;
        if(yDestination < AREA_Y) continue;
        if(yDestination >= MAX_AREA_Y) break main;
        yDestination = (yDestination - AREA_Y) << 3;
        for(int yy = 0; yy < 8; yy++) {
          int yySource = ySource | (yy << 8);
          int yyDestination = (yDestination | yy) * AREA_WIDTH;
          for(int x = 0; x < AREA_WIDTH; x++) {
            int source = yySource | (x + AREA_X);
            int destination = (yyDestination + x) << 3;
            int value = byteScreen[source] & 0xFF;
            for(int xx = 7; xx >= 0; xx--) {
              data[destination | xx] = (value & 1) > 0;
              value = value >> 1;
            }
          }
        }
      }
    }
    
    return data;
  }
  
  public static void loadBackground(int num, boolean withAttributes)
      throws IOException {
    boolean[] screen = load(num, withAttributes);
    for(int i = 0; i < PIXEL_SIZE; i++)
      if(screen[i]) background1[i]++; else background0[i]++;
  }

  public static void process(int from, int to) throws IOException {
    background0 = new int[PIXEL_SIZE];
    background1 = new int[PIXEL_SIZE];
    for(int num = from; num <= to; num++)
      loadBackground(num, num == from);
    for(int i = 0; i < PIXEL_SIZE; i++)
      background[i] = background1[i] >= background0[i];
  }
  
  public static void saveBackground() throws IOException {
    BufferedImage image = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT
        , BufferedImage.TYPE_INT_RGB);
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      int yAttrSource = ((y >> 3) + AREA_Y) << 5;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int attr = attrs[yAttrSource | (x >> 3 + AREA_X)];
        boolean value = background[ySource | x];
        image.setRGB(x, y, value ? color[attr & 7] : color[(attr >> 3) & 7]);
      }
    }
    
    File outputfile = new File("D:/temp2/output/background.png");
    ImageIO.write(image, "png", outputfile);  }
}
