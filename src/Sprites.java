import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class Sprites extends Main {
  private static enum SpritePixelType {ON, OFF, ANY};
  
  private static class Sprite {
    SpritePixelType[] data;
    int width, height;
    
    public Sprite(File file) throws IOException {
      BufferedImage image = ImageIO.read(file);
      width = image.getWidth();
      height = image.getHeight();
      data = new SpritePixelType[width * height];
      for(int y = 0; y < height; y++) {
        int yAddr = y * width;
        for(int x = 0; x < width; x++) {
          switch(image.getRGB(x, y)) {
            case 0xFF000000:
              data[yAddr + x] = SpritePixelType.OFF;
              break;
            case 0xFFFFFFFF:
              data[yAddr + x] = SpritePixelType.ON;
              break;
            default:
              data[yAddr + x] = SpritePixelType.ANY;
              break;
          }
        }
      }
    }
  }
  
  private static final LinkedList<Sprite> sprites = new LinkedList<>();
  
  public static void load() throws IOException {
    final File folder = new File(project + "sprites");
    for(final File file: folder.listFiles()) sprites.add(new Sprite(file));
  }
  
  public static void declash(
      int [] pixels, int imageNumber, boolean[] screen, boolean[] background
      , int x1, int y1, int x2, int y2, BufferedImage image) {
    for(int y = y1; y < y2; y++) {
      int yAddr = y * PIXEL_WIDTH;
      for(int x = x1; x < x2; x++) {
        int addr = x + yAddr;
        if(pixels[addr] == imageNumber) {
          image.setRGB(x, y, screen[addr] ? particleColor : 0);
        } else if(SHOW_DETECTION_AREA) {
          image.setRGB(x, y, color[3]);
        }
      }
    }
    
    int width = x2 - x1;
    int height = y2 - y1;
    int bestDx = 0, bestDy = 0, bestErrors = -1;
    Sprite bestSprite = null;
    main: for(Sprite sprite: sprites) {
      int spriteWidth = sprite.width;
      int spriteHeight = sprite.height;
      int dy1 = y1 - MIN_DETECTION_HEIGHT - spriteHeight;
      int dy2 = y1 + height - MIN_DETECTION_HEIGHT;
      for(int dy = dy1; dy <= dy2; dy++) {
        int dx1 = x1 + MIN_DETECTION_WIDTH - spriteWidth;
        int dx2 = x1 + width - MIN_DETECTION_WIDTH;
        dx: for(int dx = dx1; dx <= dx2; dx++) {
          int errors = 0;
          for(int spriteY = 0; spriteY < spriteHeight; spriteY++) {
            int screenY = spriteY + dy;
            if(screenY < 0 || screenY >= PIXEL_HEIGHT) continue;
            int yScreen = screenY * PIXEL_WIDTH;
            int ySprite = spriteY * spriteWidth;
            for(int spriteX = 0; spriteX < spriteWidth; spriteX++) {
              int screenX = spriteX + dx;
              if(screenX < 0 || screenX >= PIXEL_WIDTH) continue;
              SpritePixelType spriteValue = sprite.data[spriteX + ySprite];
              boolean screenValue = screen[screenX + yScreen];
              switch(spriteValue) {
                case ON:
                  if(!screenValue) errors++;
                  break;
                case OFF:
                  //if(screenValue) errors++;
                  break;
                case ANY:
                  break;
              }
              if(errors > MAX_ERRORS) continue dx;
            }
          }
          if(bestErrors < 0 || errors < bestErrors) {
            bestDx = dx;
            bestDy = dy;
            bestErrors = errors;
            bestSprite = sprite;
            if(errors == 0) break main;
          }
        }
      }
    }
    
    if(bestErrors < 0) return;
    
    int spriteWidth = bestSprite.width;
    int spriteHeight = bestSprite.height;
    for(int spriteY = 0; spriteY < spriteHeight; spriteY++) {
      int screenY = spriteY + bestDy;
      if(screenY < 0 || screenY >= PIXEL_HEIGHT) continue;
      int yScreen = screenY * PIXEL_WIDTH;
      int ySprite = spriteY * spriteWidth;
      for(int spriteX = 0; spriteX < spriteWidth; spriteX++) {
        int screenX = spriteX + bestDx;
        if(screenX < 0 || screenX >= PIXEL_WIDTH) continue;
        switch(bestSprite.data[spriteX + ySprite]) {
          case ON:
            image.setRGB(screenX, screenY, spriteColor);
            break;
          case OFF:
            image.setRGB(screenX, screenY, color[0]);
            break;
        }              
      }
    }
  }
}
