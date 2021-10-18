import java.awt.image.BufferedImage;
import java.io.IOException;

class Main {
  public static final int BORDER_SIZE = 4, MAX_DISTANCE = 2
      , MIN_WIDTH = 8, MAX_WIDTH = 24, MIN_HEIGHT = 8, MAX_HEIGHT = 32
      , AREA_X = 0, AREA_Y = 6, AREA_WIDTH = 32, AREA_HEIGHT = 18
      , MAX_DIFFERENCE = 8, MIN_FRAMES = 20, MIN_QUANTITY = 3;
  public static final double PERCENT_ON = 0.7, MAX_ERRORS = 5;
      
  public static final int BYTE_SIZE = 32 * 24 * 8, ATTR_SIZE = BYTE_SIZE / 8
      , PIXEL_WIDTH = AREA_WIDTH << 3, PIXEL_HEIGHT = AREA_HEIGHT << 3
      , AREA_SIZE = AREA_WIDTH * AREA_HEIGHT, PIXEL_SIZE = AREA_SIZE << 6
      , MAX_AREA_X = AREA_X + AREA_WIDTH, MAX_AREA_Y = AREA_Y + AREA_HEIGHT;
  
  public enum Mode {EXTRACT, DECLASH}
  
  public static boolean colored = false, resized = true;
  public static Mode mode = Mode.DECLASH;
  
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  
  public static void main(String[] args) {
    try {
      if(mode == Mode.DECLASH) Sprites.load();
      
      //Screen.process("source/", 28875, 29154); // sprites
      Screen.process("source/", 1367, 1545); // declash
      Screen.process("source/", 4413, 4600); // declash
      Screen.process("source/", 6732, 6908); // declash
      Screen.process("source/", 12660, 13040); // declash
      Screen.process("source/", 35055, 35232); // declash
      Screen.process("source/", 37795, 37973); // declash
      Screen.process("source/", 38183, 38361); // declash
      //Screen.process("source/", 34234, 34418); // particles
      //Screen.process("source/", 1, 60299);
      //Screen.process("D:/temp2/scr/", 1, 10299);
      
      ImageExtractor.saveImages();
      
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
