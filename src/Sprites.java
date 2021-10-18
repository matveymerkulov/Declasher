import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
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
    final File folder = new File("sprites");
    for(final File file: folder.listFiles()) sprites.add(new Sprite(file));
  }
  
  public static BufferedImage declash(boolean[] screen, boolean[] background
      , int x1, int y1, int x2, int y2) {
    BufferedImage newImage = Screen.toImage(screen);

    for(int y = y1; y < y2; y++) {
      int yAddr = y * PIXEL_WIDTH;
      for(int x = x1; x < x2; x++) {
        int addr = x + yAddr;
        if(screen[addr] && !background[addr]) {
          newImage.setRGB(x, y, color[7]);
        } else {
          //newImage.setRGB(x, y, color[3]);
        }
      }
    }
    
    int width = x2 - x1;
    int height = y2 - y1;
    for(Sprite sprite: sprites) {
      int spriteWidth = sprite.width;
      int spriteHeight = sprite.height;
      if(spriteWidth < width || spriteHeight < height) continue;
      for(int dy = y2 - spriteHeight; dy <= y1; dy++) {
        dx: for(int dx = x2 - spriteWidth; dx <= x1; dx++) {
          for(int pass = 0; pass <= 1; pass++) {
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
                if(pass == 0) {
                  boolean screenValue = screen[screenX + yScreen];
                  switch(spriteValue) {
                    case ON:
                      if(!screenValue) errors++;
                      break;
                    case OFF:
                      if(screenValue) errors++;
                      break;
                    case ANY:
                      break;
                  }
                  if(errors > MAX_ERRORS) continue dx;
                } else if(spriteValue == SpritePixelType.ON) {
                  newImage.setRGB(screenX, screenY, color[7]);
                }
              }
            }
          }
        }
      }
    }
    
    return newImage;
  }
}
