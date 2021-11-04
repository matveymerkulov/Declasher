import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

class Main {
  public static final int BORDER_SIZE = 4, MAX_DISTANCE = 2
      , MIN_WIDTH = 10, MAX_WIDTH = 48, MIN_HEIGHT = 6, MAX_HEIGHT = 90
      , AREA_X = 0, AREA_Y = 6, AREA_WIDTH = 32, AREA_HEIGHT = 18
      , MAX_CHANGED_PIXELS = 500, MIN_MATCHED_PIXELS = 60
      , MAX_BACKGROUND_DELAY = 10, MIN_FRAMES = 5, FRAME_FREQUENCY = 10;
      
  public static final int BYTE_SIZE = 32 * 24 * 8, ATTR_SIZE = BYTE_SIZE / 8
      , PIXEL_WIDTH = AREA_WIDTH << 3, PIXEL_HEIGHT = AREA_HEIGHT << 3
      , AREA_SIZE = AREA_WIDTH * AREA_HEIGHT, PIXEL_SIZE = AREA_SIZE << 6
      , MAX_AREA_X = AREA_X + AREA_WIDTH, MAX_AREA_Y = AREA_Y + AREA_HEIGHT;
  
  public static final int[] skippedBackgrounds = new int[]{};
  
  public static final String OUT_DIR = "D:/output/";
  
  public enum Mode {EXTRACT_SPRITES, EXTRACT_BACKGROUNDS, DECLASH
      , DETECT_MAX_SIZE, SHOW_DIFFERENCE, TO_BLACK_AND_WHITE
      , COLOR_BACKGROUNDS};
  
  public static final int [] color = 
      { 0x000000, 0x00009F, 0x9F0000, 0x9F009F
      , 0x009F00, 0x009F9F, 0x9F9F00, 0x9F9F9F
      , 0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
      , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};

  // options
  
  public static final Mode mode = Mode.COLOR_BACKGROUNDS;
  public static int SPRITE_COLOR = color[15], PARTICLE_COLOR = color[15];
  public static double PERCENT_ON = 0.7;
  public static int MIN_DETECTION_WIDTH = 8, MIN_DETECTION_HEIGHT = 8
      , MIN_DETECTION_PIXELS = 140, MIN_DIFFERENCE = 300
      , MIN_BG_DIFFERENCE = 900;
  public static String project = "ratime/";
  
  // debug
  
  public static final boolean SAVE_COLORED = false, SHOW_DETECTION_AREA = false
      , RESIZED = false, BLACK_AND_WHITE = false, SAVE_SIMILAR = true;
  
  // main
  
  public static void main(String[] args) throws IOException {
    Screen.init();
    Sprites.load();
    for(File file: (new File(OUT_DIR)).listFiles()) file.delete();
    
    switch(mode) {
      case COLOR_BACKGROUNDS:
        Screen.saveBackgrounds();
        break;
      case EXTRACT_BACKGROUNDS:
        //Screen.process(23073);
        //Screen.process(0, 10000, false);
        Screen.process(0, -1, false);
        break;
      case EXTRACT_SPRITES:
        ImageExtractor.saveImages();
        Screen.process(0, 10000, false);
        break;
      case DECLASH:
        //Screen.process(0, 10000, false);

        //Screen.process(2438, 2656, 2833, 23073); // bg 
        //Screen.process([28875, 29154]); // sprites
        //Screen.process([2490, 2438, 34234]); // particles
        //Screen.process([1367, 4413, 6732, 12660, 35055, 37795, 38183]); // declash
        //Screen.process(0, 10000, false);
        //Screen.process(0, -1, false);
        break;
      default:
        Screen.process(0, -1, false);
        break;
    }
    

    System.out.println("Min sprite pixels is " + Sprites.getMinSpritePixels());
    System.out.println("Min detection area size is "
        + Sprites.getMaxDetectionSize() + " pixels");
    System.out.println("Max image is " + Image.getMaxSize() + " pixels");
    System.out.println("Max errors is " + Sprites.getMaxErrors());
    System.out.println("Max difference is " + Sprites.getMaxDifference());
  }
  
  public static BufferedImage resizeImage(BufferedImage originalImage
      , int targetWidth, int targetHeight) {
    java.awt.Image resultingImage = originalImage.getScaledInstance(targetWidth
        , targetHeight, java.awt.Image.SCALE_DEFAULT);
    BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight
        , BufferedImage.TYPE_INT_RGB);
    outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
    return outputImage;
  }

  public static BufferedImage x3(BufferedImage image) {
    if(RESIZED) return resizeImage(image, image.getWidth() * 3
        , image.getHeight() * 3);
    return image;
  }
  
  public static BufferedImage copyImage(BufferedImage image) {
    ColorModel cm = image.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = image.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }
}
