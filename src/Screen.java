import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.tukaani.xz.SeekableFileInputStream;
import org.tukaani.xz.SeekableXZInputStream;

public class Screen extends Main {
  private static final int FRAME_SIZE = BYTE_SIZE + ATTR_SIZE;
  private static final int[] backgroundOn = new int[PIXEL_SIZE];
  private static final boolean[] background = new boolean[PIXEL_SIZE];
  private static int[] attrs, backgroundAttrs;

  private static SeekableXZInputStream in;
  
  public static void init() throws IOException {
    SeekableFileInputStream file
        = new SeekableFileInputStream(project + "video.xz");
    in = new SeekableXZInputStream(file);
  }
  
  private static boolean[] load(int num) throws IOException {
    boolean[] data = new boolean[PIXEL_SIZE];
    
    byte[] byteScreen = new byte[FRAME_SIZE];
    in.seek(num * FRAME_SIZE);
    in.read(byteScreen, 0, FRAME_SIZE);

    attrs = new int[ATTR_SIZE];
    for(int x = 0; x < ATTR_SIZE; x++) {
      int value = byteScreen[BYTE_SIZE | x];
      int bright = (value & 0b1000000) == 0 ? 0 : 0b10001000;
      attrs[x] =  (value & 0b111) | ((value & 0b111000) >> 2) | bright;
    }
    
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
            int byteValue = byteScreen[source];
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
  
  public static int screenDifference() {
    int difference = 0;
    for(int i = 0; i < ATTR_SIZE; i++)
      if(attrs[i] != backgroundAttrs[i]) difference++;
    return difference;
  }

  private static void composeBackground(int frames) {
    int minFrames = (int) Math.floor(PERCENT_ON * frames);
    for(int addr = 0; addr < PIXEL_SIZE; addr++) 
      background[addr] = backgroundOn[addr] >= minFrames;
  }

  public static void process(int from, int to) throws IOException {
    int firstFrame = from;
    boolean newSection = true;
    for(int frame = from; frame <= to; frame++) {
      boolean[] screen;
      screen = load(frame);
      
      if(!newSection && screenDifference() > MAX_DIFFERENCE) {
        processSequence(firstFrame, frame);
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
    processSequence(firstFrame, to + 1);
  }

  private static void processSequence(int from, int to)
      throws IOException {
    System.out.println(from + " - " + to + ", " + ImageExtractor.images.size());
    int frames = to - from;
    if(frames >= MIN_FRAMES || mode == Mode.DECLASH) {
      composeBackground(frames);
      if(mode == Mode.EXTRACT_BACKGROUNDS) {
        saveImage(toImage(background), from);
        return;
      }
      for(int frame = from; frame < to; frame++) {
        if(LOG_PROGRESS) System.out.println(frame + " / " + to);
        ImageExtractor.process(load(frame), background, frame);
      }
    }
  }
  
  public static BufferedImage toImage(boolean[] screenData) {
    BufferedImage image = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT
        , BufferedImage.TYPE_INT_RGB);
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      int yAttrSource = ((y >> 3) + AREA_Y) << 5;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int attr = backgroundAttrs[yAttrSource | (x >> 3 + AREA_X)];
        int addr = ySource | x;
        boolean value = screenData[addr];
        image.setRGB(x, y, value ? color[attr & 15] : color[(attr >> 4) & 15]);
      }
    }
    
    return image;
  }
  
  public static void saveImage(BufferedImage image, int fileNumber)
      throws IOException {
    if(RESIZED) image = resizeImage(image, PIXEL_WIDTH * 3, PIXEL_HEIGHT * 3);
    File outputfile = new File("D:/temp2/output/"
        + String.format("%06d", fileNumber) + ".png");
    ImageIO.write(image, "png", outputfile);
  }
}
