import java.io.IOException;
import java.util.LinkedList;

class Main {
  public static final int BORDER_SIZE = 4, MAX_DISTANCE = 2
      , MIN_WIDTH = 8, MAX_WIDTH = 24, MIN_HEIGHT = 8, MAX_HEIGHT = 32
      , AREA_X = 0, AREA_Y = 6, AREA_WIDTH = 32, AREA_HEIGHT = 18;
  public boolean colored = false;
  
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  
  private static final LinkedList<Image> images = new LinkedList<>();
  
  public static void main(String[] args) {
    try {
      Screen.process(216, 216);
      Screen.saveBackground();
      //for(Image image: images) image.save();
    } catch (IOException e) {
      System.err.println("I/O error");
    }
  }
}
