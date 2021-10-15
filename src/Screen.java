import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class Screen extends Main {
  private static final int BYTE_SIZE = 32 * 24 * 8, ATTR_SIZE = BYTE_SIZE / 8
      , PIXEL_WIDTH = AREA_WIDTH << 3, PIXEL_HEIGHT = AREA_HEIGHT << 3
      , AREA_SIZE = AREA_WIDTH * AREA_HEIGHT, PIXEL_SIZE = AREA_SIZE << 6
      , MAX_AREA_X = AREA_X + AREA_WIDTH, MAX_AREA_Y = AREA_Y + AREA_HEIGHT;
  
  private static final int[] backgroundOn = new int[PIXEL_SIZE];
  private static int[] attrs, backgroundAttrs;

  private static boolean[] load(String path, int num)
      throws IOException {
    boolean[] data = new boolean[PIXEL_SIZE];
    
    File image = new File(path + String.format("%05d", num)
        + ".scr");
    FileInputStream input = new FileInputStream(image);
    byte[] byteScreen = new byte[BYTE_SIZE + ATTR_SIZE];
    input.read(byteScreen);
    
    attrs = new int[ATTR_SIZE];
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
            int byteValue = byteScreen[source] & 0xFF;
            for(int xx = 7; xx >= 0; xx--) {
              int addr = destination | xx;
              boolean pixelValue = (byteValue & 1) > 0;
              data[addr] = pixelValue;
              byteValue = byteValue >> 1;
            }
          }
        }
      }
    }
    
    return data;
  }
  
  public static int difference() {
    int difference = 0;
    for(int i = 0; i < ATTR_SIZE; i++)
      if(attrs[i] != backgroundAttrs[i]) difference++;
    return difference;
  }

  public static void process(int from, int to, String path) throws IOException {
    int frames = 0;
    boolean newSection = true;
    for(int num = from; num <= to; num++) {
      boolean[] screen = load(path, num);
      if(!newSection && difference() > MAX_DIFFERENCE) {
        if(frames >= MIN_FRAMES) saveBackground(frames);
        frames = 0;
        newSection = true;
      }
      
      if(newSection) {
        //background = screen;
        Arrays.fill(backgroundOn, 0);
        newSection = false;
      }
      
      backgroundAttrs = attrs;
      frames++;
      for(int i = 0; i < PIXEL_SIZE; i++)
        if(screen[i]) backgroundOn[i]++;
    }
  }
  
  private static int fileNumber = -1;
      
  public static void saveBackground(int frames) throws IOException {
    BufferedImage image = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT
        , BufferedImage.TYPE_INT_RGB);
    
    int minFrames = (int) Math.floor(PERCENT_ON * frames);
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      int yAttrSource = ((y >> 3) + AREA_Y) << 5;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int attr = backgroundAttrs[yAttrSource | (x >> 3 + AREA_X)];
        int addr = ySource | x;
        boolean value = backgroundOn[addr] >= minFrames;
        image.setRGB(x, y, value ? color[attr & 7] : color[(attr >> 3) & 7]);
      }
    }
    
    fileNumber++;
    File outputfile = new File("D:/temp2/output/"
        + String.format("%06d", fileNumber) +".png");
    ImageIO.write(image, "png", outputfile);  }
}
