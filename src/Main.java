import java.awt.image.BufferedImage;
import java.io.IOException;

class Main {
  public static final int BORDER_SIZE = 4, MAX_DISTANCE = 2
      , MIN_WIDTH = 8, MAX_WIDTH = 24, MIN_HEIGHT = 8, MAX_HEIGHT = 32
      , AREA_X = 0, AREA_Y = 6, AREA_WIDTH = 32, AREA_HEIGHT = 18
      , MAX_DIFFERENCE = 8, MIN_FRAMES = 20, MIN_QUANTITY = 3;
  public static final double PERCENT_ON = 0.7;
      
  public static final int BYTE_SIZE = 32 * 24 * 8, ATTR_SIZE = BYTE_SIZE / 8
      , PIXEL_WIDTH = AREA_WIDTH << 3, PIXEL_HEIGHT = AREA_HEIGHT << 3
      , AREA_SIZE = AREA_WIDTH * AREA_HEIGHT, PIXEL_SIZE = AREA_SIZE << 6
      , MAX_AREA_X = AREA_X + AREA_WIDTH, MAX_AREA_Y = AREA_Y + AREA_HEIGHT;
  
  public boolean colored = false;
  
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  
  public static void main(String[] args) {
    try {
      //Screen.process(28875, 29154, "source/"); // sprites
      //Screen.process(12863, 13040, "source/"); // declash
      //Screen.process(34032, 37979, "source/"); // declash
      //Screen.process(34234, 34418, "source/"); // particles
      Screen.process(1, 60299, "source/");
      //Screen.process(1, 60299, "D:/temp2/scr/");
      
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
