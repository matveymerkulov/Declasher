import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class Screen extends Main {
  private static final int[] backgroundOn = new int[PIXEL_SIZE];
  private static final boolean[] background = new boolean[PIXEL_SIZE];
  private static int[] attrs, backgroundAttrs;

  private static boolean[] load(String path, int num) throws IOException {
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
  
  public static BufferedImage declash(boolean[] screen
      , BufferedImage backgroundImage) {
    ColorModel cm = backgroundImage.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = backgroundImage.copyData(null);
    BufferedImage image = new BufferedImage(cm, raster, isAlphaPremultiplied
        , null);
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int addr = ySource | x;
        boolean value = screen[addr];
        if(value && value != background[addr])
          image.setRGB(x, y, color[7]);
      }
    }
    
    return image;
  }

  private static void composeBackground(int frames) {
    int minFrames = (int) Math.floor(PERCENT_ON * frames);
    for(int addr = 0; addr < PIXEL_SIZE; addr++) 
      background[addr] = backgroundOn[addr] >= minFrames;
  }

  public static void process(int from, int to, String path) throws IOException {
    int firstFrame = from;
    boolean newSection = true;
    for(int frame = from; frame <= to; frame++) {
      boolean[] screen;
      try {
        screen = load(path, frame);
      } catch (IOException e) {
        continue;
      }
      
      if(!newSection && difference() > MAX_DIFFERENCE) {
        saveClip(path, firstFrame, frame);
        firstFrame = frame;
        newSection = true;
      }

      if(newSection) {
        //background = screen;
        Arrays.fill(backgroundOn, 0);
        newSection = false;
      }

      backgroundAttrs = attrs;
      for(int i = 0; i < PIXEL_SIZE; i++)
        if(screen[i]) backgroundOn[i]++;
    }
    saveClip(path, firstFrame, to + 1);
  }

  private static void saveClip(String path, int from, int to)
      throws IOException {
    System.out.println(from + " - " + to);
    int frames = to - from;
    if(frames >= MIN_FRAMES) {
      composeBackground(frames);
      BufferedImage backgroundImage = backgroundToImage();
      //saveBackground(backgroundImage);
      for(int frame = from; frame < to; frame++) {
        try {
          ImageExtractor.extract(load(path, frame), background);
        } catch (IOException e) {
          continue;
        }
        //saveImage(declash(load(path, frame), backgroundImage), frame);
      }
    }
  }
  
  public static BufferedImage backgroundToImage() {
    BufferedImage image = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT
        , BufferedImage.TYPE_INT_RGB);
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      int yAttrSource = ((y >> 3) + AREA_Y) << 5;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int attr = backgroundAttrs[yAttrSource | (x >> 3 + AREA_X)];
        int addr = ySource | x;
        boolean value = background[addr];
        image.setRGB(x, y, value ? color[attr & 7] : color[(attr >> 3) & 7]);
      }
    }
    
    return image;
  }
  
  public static void saveImage(BufferedImage image, int fileNumber)
      throws IOException {
    image = resizeImage(image, PIXEL_WIDTH * 3, PIXEL_HEIGHT * 3);
    File outputfile = new File("D:/temp2/output/"
        + String.format("%06d", fileNumber) + ".png");
    ImageIO.write(image, "png", outputfile);
  }
}
