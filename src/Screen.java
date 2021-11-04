import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.min;
import java.util.Arrays;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import org.tukaani.xz.SeekableFileInputStream;
import org.tukaani.xz.SeekableXZInputStream;

public class Screen extends Main {
  private static final int FRAME_SIZE = BYTE_SIZE + ATTR_SIZE;
  private static final int[] backgroundOn = new int[PIXEL_SIZE];
  private static int[] attrs;

  private static SeekableXZInputStream in;
  
  public static void init() throws IOException {
    SeekableFileInputStream file
        = new SeekableFileInputStream(project + "video.xz");
    in = new SeekableXZInputStream(file);
    loadBackgrounds();
  }
  
  // loading
  
  private static boolean[] load(int num) throws IOException {
    boolean[] data = new boolean[PIXEL_SIZE];
    
    byte[] byteScreen = new byte[FRAME_SIZE];
    in.seek(num * FRAME_SIZE);
    if(in.read(byteScreen, 0, FRAME_SIZE) < FRAME_SIZE) throw new IOException();

    attrs = new int[ATTR_SIZE];
    for(int x = 0; x < ATTR_SIZE; x++) {
      int value = byteScreen[BYTE_SIZE | x];
      int bright = (value & 0b1000000) == 0 ? 0 : 0b10001000;
      attrs[x] =  (value & 0b111) | ((value & 0b111000) << 1) | bright;
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
  
  // background
  
  private static class Background {
    private final boolean[] values;
    private final BufferedImage image;
    private final String fileName;

    public Background(boolean[] values) {
      this.values = values;
      this.image = null;
      this.fileName = "";
    }

    public Background(boolean[] values, BufferedImage image, String fileName) {
      this.values = values;
      this.image = image;
      this.fileName = fileName;
    }
    
    public int difference(boolean[] screen) {
      int difference = 0;
      for(int i = 0; i < PIXEL_SIZE; i++)
        if(values[i] != screen[i]) difference++;
      return difference;
    }
  }
  
  public static void loadBackgrounds() throws IOException {
    for(final File file: new File(project + "backgrounds").listFiles()) {
      if(file.isDirectory()) continue;
      BufferedImage image = ImageIO.read(file);
      boolean[] pixels = new boolean[PIXEL_SIZE];
      for(int y = 0; y < PIXEL_HEIGHT; y++) {
        for(int x = 0; x < PIXEL_WIDTH; x++) {
          pixels[x + y * PIXEL_WIDTH] = (image.getRGB(x, y) & 0xFF) > 0x7F;
        }
      }
      backgrounds.add(new Background(pixels, null, file.getName()));
    }
  }
  
  public static void saveBackgrounds() throws IOException {
    for(Background background: backgrounds) {
      int number = Integer.parseInt(
          background.fileName.substring(0, background.fileName.indexOf('.')));
      load(number + 1);
      saveImage(toImage(background.values, null), background.fileName);
    }
  }
  
  private static Background findBackground(boolean[] screen) {
    int minDifference = PIXEL_SIZE;
    Background minBackground = null;
    for(Background background: backgrounds) {
      int difference = background.difference(screen);
      if(difference < minDifference) {
        minDifference = difference;
        minBackground = background;
      }
    }
    //System.out.println("Min difference is " + minDifference);
    return minBackground;
  }
  
  private static LinkedList<Background> backgrounds
      = new LinkedList<>();

  private static boolean[] composeBackground(int frames) {
    int minFrames = (int) Math.floor(PERCENT_ON * frames);
    boolean[] background = new boolean[PIXEL_SIZE];
    for(int addr = 0; addr < PIXEL_SIZE; addr++) 
      background[addr] = backgroundOn[addr] >= minFrames;
    return background;
  }
  
  // processing

  public static void process(int... start) throws IOException {
    for(int i = 0; i < start.length; i++) process(start[i], -1, true);
  }
  
  public static void process(int from, int to, boolean singleScreen)
      throws IOException {
    int firstFrame = from;
    boolean[] oldScreen = null;
    for(int frame = from; frame <= to || to < 0; frame++) {
      boolean[] screen;
      try {
        screen = load(frame);
      } catch (IOException ex) {
        break;
      }
      
      if(oldScreen == null) {
        oldScreen = screen;
        continue;
      }
      
      //final int SAME = 0, CHANGED = 1;
      //int[] pixels = new int[PIXEL_SIZE];
      int difference = 0;
      for(int addr = 0; addr < PIXEL_SIZE; addr++) {
        boolean isChanged = screen[addr] != oldScreen[addr];
        //pixels[addr] = isChanged ? CHANGED : SAME;
        if(screen[addr]) backgroundOn[addr]++;
        if(isChanged) difference++;
      }
        
      if(difference >= MIN_DIFFERENCE) {
        int frames = frame - firstFrame;
        if(frames >= MIN_FRAMES) {
          oldScreen = composeBackground(frames);
          if(mode == Mode.EXTRACT_BACKGROUNDS) {
            if(findBackground(oldScreen) == null || SAVE_SIMILAR) {
              saveImage(toImage(oldScreen, null), firstFrame);
              backgrounds.add(new Background(oldScreen));
              //saveImage(toImage(oldScreen, null), frame - 1);
              //saveImage(toImage(screen, null), frame);
              System.out.println("Saved background " + firstFrame
                  + " with difference " + difference + " and " + frames
                  + " frames");
            }
          } else {
            System.out.println("Sequence " + firstFrame + " - " + (frame - 1));
          }
        }
        if(singleScreen) return;
        Arrays.fill(backgroundOn, 0);
        firstFrame = frame;
      } else if(frame % FRAME_FREQUENCY == 0) {
        switch(mode) {
          case SHOW_DIFFERENCE:
            Background background = findBackground(screen);
            if(background != null) saveImage(toImage(screen, background.values)
                , frame);
            break;
          case TO_BLACK_AND_WHITE:
            saveImage(toImage(screen, null), frame);
            break;
        }
      }

      oldScreen = screen;
    }
  }
  
  // conversion and saving
  
  public static BufferedImage toImage(boolean[] screenData, boolean[] bgData) {
    BufferedImage image = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT
        , BufferedImage.TYPE_INT_RGB);
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      int yAttrSource = ((y >> 3) + AREA_Y) << 5;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int attr = attrs[yAttrSource | (x >> 3 + AREA_X)];
        int addr = ySource | x;
        boolean value = screenData[addr];
        int col;
        if(BLACK_AND_WHITE) {
          col = color[value ? 15 : 0];
        } else {
          col = value ? color[attr & 15] : color[(attr >> 4) & 15];
        }
        if(bgData != null && value != bgData[addr]) col = color[11];
        image.setRGB(x, y, col);
      }
    }
    
    return image;
  }
  
  public static void saveImage(BufferedImage image, int fileNumber)
      throws IOException {
    saveImage(image, String.format("%06d", fileNumber) + ".png");
  }
  
  public static void saveImage(BufferedImage image, String fileName)
      throws IOException {
    File outputfile = new File(OUT_DIR + fileName);
    ImageIO.write(x3(image), "png", outputfile);
  }
}