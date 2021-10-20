import java.awt.image.BufferedImage;
import java.io.IOException;

class Main {
  public static final int BORDER_SIZE = 4, MAX_DISTANCE = 2
      , MIN_WIDTH = 8, MAX_WIDTH = 32, MIN_HEIGHT = 8, MAX_HEIGHT = 40
      , AREA_X = 0, AREA_Y = 6, AREA_WIDTH = 32, AREA_HEIGHT = 18
      , MAX_DIFFERENCE = 8, MIN_FRAMES = 20, MIN_QUANTITY = 3;
      
  public static final int BYTE_SIZE = 32 * 24 * 8, ATTR_SIZE = BYTE_SIZE / 8
      , PIXEL_WIDTH = AREA_WIDTH << 3, PIXEL_HEIGHT = AREA_HEIGHT << 3
      , AREA_SIZE = AREA_WIDTH * AREA_HEIGHT, PIXEL_SIZE = AREA_SIZE << 6
      , MAX_AREA_X = AREA_X + AREA_WIDTH, MAX_AREA_Y = AREA_Y + AREA_HEIGHT;
  
  public enum Mode {EXTRACT_SPRITES, EXTRACT_BACKGROUNDS, DECLASH
  , DETECT_MAX_SIZE}
  
  public static final int [] color = 
      { 0x000000, 0x00007F, 0x7F0000, 0x7F007F
      , 0x007F00, 0x007F7F, 0x7F7F00, 0x7F7F7F
      , 0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
      , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  
  public static boolean COLORED = false, RESIZED = true, LOG_PROGRESS = true;
  public static Mode mode = Mode.DECLASH;
  public static int spriteColor = color[15], particleColor = color[15];
  public static double PERCENT_ON = 0.7;
  public static int MAX_ERRORS = 32;
  public static int MIN_DETECTION_WIDTH = 8, MIN_DETECTION_HEIGHT = 8;
  
  public static void main(String[] args) {
    try {
      if(mode == Mode.DECLASH) Sprites.load();
      
      //Screen.process("source/", 28875, 29154); // sprites
      
      //Screen.process("source/", 2490, 2530); // particles
      particleColor = color[13]; Screen.process("source/", 2438, 2642); // particles
      particleColor = color[15]; Screen.process("source/", 34234, 34418); // particles
      /*
      Screen.process("source/", 1367, 1545); // declash
      Screen.process("source/", 4413, 4600); // declash
      Screen.process("source/", 6732, 6908); // declash
      Screen.process("source/", 12660, 13040); // declash
      Screen.process("source/", 35055, 35232); // declash
      Screen.process("source/", 37795, 37973); // declash
      Screen.process("source/", 38183, 38361); // declash
      */
      //LOG_PROGRESS = false; Screen.process("source/", 1, 60299);
      //LOG_PROGRESS = false; Screen.process("D:/temp2/scr/", 1, 60299);
      
      ImageExtractor.saveImages();
      Image.logMaxSize();
      
    } catch (IOException ex) {
      System.err.println(ex.toString());
    }
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
}
