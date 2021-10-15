import java.io.IOException;
import java.util.LinkedList;

class Main {
  public static final int borderSize = 4, minWidth = 8, maxWidth = 24
      , minHeight = 8, maxHeight = 32;
  public boolean colored = false;
  
  public static final int [] color = {0x000000, 0x0000FF, 0xFF0000, 0xFF00FF
          , 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  
  private static final LinkedList<Image> images = new LinkedList<>();
  
  public static void main(String[] args) {
    try {
      process(327, 475);
      /*process(483, 666);
      process(754, 912);
      process(923, 1101);
      process(1106, 1357);*/
      /*process(1369, 1546);
      process(1566, 1747);
      process(1840, 2025);
      process(2043, 2230);*/
      
      for(Image image: images) image.save();
    } catch (IOException e) {
      System.err.println("I/O error");
    }
  }

  private static void process(int from, int to) throws IOException {
    Screen.composeBackground(from, to);
    Screen.saveBackground();
  }
}
