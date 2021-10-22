import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
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
  
  private static class Attributes {
    private final int[] values;
    private final BufferedImage background;

    public Attributes(int[] values) {
      this.values = values;
      this.background = null;
    }

    public Attributes(int[] values, BufferedImage background) {
      this.values = values;
      this.background = background;
    }
    
    public boolean areSameAs(int[] attributes) {
      return !isDifferent(values, attributes);
    }
  }
  
  public static void loadBackgrounds() throws IOException {
    for(final File file: new File(project + "backgrounds").listFiles()) {
      String fileName = file.getName();
      int number = Integer.parseInt(
          fileName.substring(0, fileName.indexOf('.')));
      load(number);
      backgroundAttributes.add(new Attributes(attrs, ImageIO.read(file)));
    }
  }
  
  private static BufferedImage findRepaintedBackground() {
    for(Attributes attributes: backgroundAttributes) {
      if(attributes.areSameAs(backgroundAttrs)) return attributes.background;
    }
    return null;
  }
  
  private static LinkedList<Attributes> backgroundAttributes
      = new LinkedList<>();
  
  private static boolean backgroundIsNew() {
    for(Attributes attributes: backgroundAttributes) {
      if(attributes.areSameAs(backgroundAttrs)) return false;
    }
    backgroundAttributes.add(new Attributes(backgroundAttrs));
    return true;
  }

  private static void composeBackground(int frames) {
    int minFrames = (int) Math.floor(PERCENT_ON * frames);
    for(int addr = 0; addr < PIXEL_SIZE; addr++) 
      background[addr] = backgroundOn[addr] >= minFrames;
  }
  
  private static boolean isDifferent(int[] attributes, int[] otherAttributes) {
    int difference = 0;
    for(int i = 0; i < ATTR_SIZE; i++)
      if(attributes[i] != otherAttributes[i]) difference++;
    return difference > MAX_DIFFERENCE;
  }
  
  // processing

  public static void process(int... start) throws IOException {
    for(int i = 0; i < start.length; i++) process(start[i], -1, true);
  }
  
  public static void process(int from, int to, boolean singleScreen)
      throws IOException {
    int firstFrame = from;
    boolean newSection = true;
    for(int frame = from; frame <= to || to < 0; frame++) {
      boolean[] screen;
      try {
        screen = load(frame);
      } catch (IOException ex) {
        break;
      }
      
      if(!newSection && isDifferent(attrs, backgroundAttrs)) {
        processSequence(firstFrame, frame);
        if(singleScreen) return;
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
    System.out.println("Processing sequence " + from + " - " + to
        + (mode == Mode.EXTRACT_SPRITES
            ? ", " + ImageExtractor.images.size() : ""));
    int frames = to - from;
    BufferedImage repaintedBackground = findRepaintedBackground();
    if(frames >= MIN_FRAMES || mode == Mode.DECLASH) {
      composeBackground(frames);
      if(mode == Mode.EXTRACT_BACKGROUNDS) {
        if(backgroundIsNew()) saveImage(toImage(background), from);
        return;
      }
      for(int frame = from; frame < to; frame++) {
        if(frame % 100 == 0) System.out.println("  " + frame + " / " + to);
        BufferedImage backgroundImage = repaintedBackground == null ?
            toImage(background) : copyImage(repaintedBackground);
        ImageExtractor.process(load(frame), background, frame
            , backgroundImage);
      }
    }
  }
  
  // conversion and saving
  
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
    File outputfile = new File(outDir
        + String.format("%06d", fileNumber) + ".png");
    ImageIO.write(image, "png", outputfile);
  }
}