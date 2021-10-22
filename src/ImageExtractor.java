
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

public class ImageExtractor extends Main {
  private static class Coords {
    private final int x, y;

    public Coords(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }
  
  private static final Stack<Coords> coordsStack = new Stack<>();
  
  public static final LinkedList<LinkedList<Image>> images = new LinkedList<>();
  
  public static void process(boolean[] screen, boolean[] background, int frame
      , BufferedImage backgroundImage) throws IOException {
    final int SAME = 0, CHANGED = 1;
    int x1, y1, x2, y2, imageNumber = 1;
    int[] pixels = new int[PIXEL_SIZE];
    
    int changed = 0;
    for(int addr = 0; addr < PIXEL_SIZE; addr++) {
      boolean isChanged = screen[addr] != background[addr];
      pixels[addr] = isChanged ? CHANGED : SAME;
      if(isChanged) changed++;
    }
    
    if(changed > MAX_CHANGED_PIXELS) {
      System.out.println("  Changed pixels of " + frame + " is " + changed);
      return;
    }
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      x: for(int x = 0; x < PIXEL_WIDTH; x++) {
        int addr = ySource | x;
        if(pixels[addr] == CHANGED) {
          x1 = x2 = x;
          y1 = y2 = y;
          imageNumber++;
          pixels[addr] = imageNumber;
          coordsStack.add(new Coords(x, y));
          while(!coordsStack.empty()) {
            Coords coords = coordsStack.pop();
            int x0 = coords.x;
            int y0 = coords.y;
            for(int dy = -MAX_DISTANCE; dy <= MAX_DISTANCE; dy++) {
              int yy = y0 + dy;
              int yAddr = yy * PIXEL_WIDTH;
              if(yy < 0 || yy >= PIXEL_HEIGHT) continue;
              for(int dx = -MAX_DISTANCE; dx <= MAX_DISTANCE; dx++) {
                if(dx == 0 && dy == 0) continue;
                int xx = x0 + dx;
                if(xx < 0 || xx >= PIXEL_WIDTH) continue;
                int addr2 = x0 + dx + yAddr;
                if(pixels[addr2] == CHANGED) {
                  x1 = Integer.min(x1, xx);
                  y1 = Integer.min(y1, yy);
                  x2 = Integer.max(x2, xx);
                  y2 = Integer.max(y2, yy);
                  pixels[addr2] = imageNumber;
                  coordsStack.add(new Coords(xx, yy));
                }
              }
            }
          } 
          x2++;
          y2++;
          
          if(Image.hasAcceptableSize(x1, y1, x2, y2)) {
            if(x2 - x1 >= MAX_WIDTH || y2 - y1 >= MAX_HEIGHT) {
              System.out.println("  " + frame + " - skipped big "
                  + (x2 - x1) + "x" + (y2 - y1) + "changed area.");
            }
            
            if(mode == Mode.DETECT_MAX_SIZE) continue;
            if(mode == Mode.EXTRACT_SPRITES) {
              Image image = new Image(pixels, screen, background, x1, y1, x2, y2
                  , imageNumber);
              for(LinkedList<Image> list: images) {
                for(Image listImage: list) {
                  switch(listImage.compareTo(image)) {
                    case EQUAL:
                      continue x;
                    case SIMILAR:
                      list.add(image);
                      continue x;
                    case OTHER:
                      break;
                  }
                }
              }

              LinkedList<Image> newList = new LinkedList<>();
              newList.add(image);
              images.add(newList);
            } else {
              Sprites.declash(pixels, imageNumber, screen, background
                  , x1, y1, x2, y2, backgroundImage);
            }
          } else {
            for(int yy = y1; yy < y2; yy++) {
              int yAddr = yy * PIXEL_WIDTH;
              for(int xx = x1; xx < x2; xx++) {
                if(pixels[xx + yAddr] == imageNumber) {
                  backgroundImage.setRGB(xx, yy, particleColor);
                }
              }
            }
          }
        }
      }
    }
    Screen.saveImage(backgroundImage, frame);
  }

  public static void saveImages() throws IOException {
    int listNum = 0;
    for(LinkedList<Image> list: images) {
      int maxWeight = -1;
      Image maxImage = null;
      for(Image image: list) {
        int size = image.getWeight();
        if(maxWeight < size) {
          maxWeight = size;
          maxImage = image;
        }
      }
      listNum++;
      maxImage.save(listNum);
    }
  }
}
