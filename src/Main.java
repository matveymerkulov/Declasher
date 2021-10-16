import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

class Main {
  public static final int BORDER_SIZE = 4, MAX_DISTANCE = 2
      , MIN_WIDTH = 8, MAX_WIDTH = 24, MIN_HEIGHT = 8, MAX_HEIGHT = 32
      , AREA_X = 0, AREA_Y = 6, AREA_WIDTH = 32, AREA_HEIGHT = 18
      , MAX_DIFFERENCE = 8, MIN_FRAMES = 20;
  public static final double PERCENT_ON = 0.7;
      
  public boolean colored = false;
  
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  
  private static final LinkedList<Image> images = new LinkedList<>();
  
  public static void main(String[] args) {
    Screen.process(34234, 34418, "source/");
    //Screen.process(12863, 13040, "source/");
    //Screen.process(34032, 37979, "source/");
    //Screen.process(1, 60299, "source/");
    //Screen.process(1, 60299, "D:/temp2/scr/");
    //for(Image image: images) image.save();
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
