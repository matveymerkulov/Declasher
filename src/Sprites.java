import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class Sprites extends Main {
  private static enum SpritePixelType {ON, OFF, ANY};
  
  private static int minSpritePixels = -1, minDetectionWidth = MAX_WIDTH
      , minDetectionHeight = MAX_HEIGHT
      , minDetectionPixels = MAX_WIDTH * MAX_HEIGHT;
  
  public static String getMaxDetectionSize() {
    return minDetectionWidth + "x" + minDetectionHeight + ", "
        + minDetectionPixels;
  }
  
  public static int getMinSpritePixels() {
    return minSpritePixels;
  }
  
  private static class Sprite {
    SpritePixelType[] data;
    BufferedImage repainted = null;
    int width, height, pixelsQuantity = 0, maxErrors;
    double minMatched;
    
    public Sprite(File file) throws IOException {
      String name = file.getName();
      switch(name) {
        case "arrow.png":
          minMatched = 0.95;
          maxErrors = 0;
          break;
        case "island.png":
          minMatched = 0.8;
          maxErrors = 15;
          break;
        default:
          minMatched = 0.9;
          maxErrors = 15;
          break;
      }
      
      final File repaintedFile = new File(project + "repainted/" + name);
      if(repaintedFile.exists()) {
        repainted = ImageIO.read(repaintedFile);
        System.out.println(name + " is repainted");
      }
      
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
              pixelsQuantity++;
              break;
            case 0xFFFFFFFF:
              data[yAddr + x] = SpritePixelType.ON;
              pixelsQuantity++;
              break;
            default:
              data[yAddr + x] = SpritePixelType.ANY;
              break;
          }
        }
      }
      if(minSpritePixels < 0 || pixelsQuantity < minSpritePixels) {
        minSpritePixels = pixelsQuantity;
      }
    }
  }
  
  private static final LinkedList<LinkedList<Sprite>> sprites
      = new LinkedList<>();
  private static int maxErrors = 0;
  private static double maxDifference = 0;
  
  public static int getMaxErrors() {
    return maxErrors;
  }
  
  public static double getMaxDifference() {
    return maxDifference;
  }
  
  
  
  public static void load() throws IOException {
    final File folder = new File(project + "sprites");
    for(final File file: folder.listFiles()) {
      LinkedList<Sprite> list = new LinkedList<>();
      if(file.isDirectory()) {
        for(final File file2: file.listFiles()) list.add(new Sprite(file2));
      } else {
        list.add(new Sprite(file));
      }
      sprites.add(list);
    }
  }
  
  
  
  public static void declash( int [] pixels, int imageNumber, boolean[] screen
      , boolean[] background, int x1, int y1, int x2, int y2
      , BufferedImage image, boolean repaint) {
    if(repaint) {
      for(int y = y1; y < y2; y++) {
        int yAddr = y * PIXEL_WIDTH;
        for(int x = x1; x < x2; x++) {
          int addr = x + yAddr;
          if(pixels[addr] == imageNumber) {
            image.setRGB(x, y, screen[addr] ? PARTICLE_COLOR : 0);
          } else if(SHOW_DETECTION_AREA) {
            image.setRGB(x, y, color[3]);
          }
        }
      }
    }
    
    int width = x2 - x1;
    int height = y2 - y1;
    //System.out.print(width + "x" + height + ", ");
    for(LinkedList<Sprite> list: sprites) {
      int bestDx = 0, bestDy = 0, bestErrors = -1, bestMatched = 0;
      Sprite bestSprite = null;
      
      list: for(Sprite sprite: list) {
        int spriteWidth = sprite.width;
        int spriteHeight = sprite.height;
        int dy1 = y1 - MIN_DETECTION_HEIGHT - spriteHeight;
        int dy2 = y1 + height - MIN_DETECTION_HEIGHT;
        for(int dy = dy1; dy <= dy2; dy++) {
          int spriteY1 = Integer.max(0, -dy);
          int spriteY2 = Integer.min(spriteHeight, PIXEL_HEIGHT - dy);
          int areaHeight = spriteY2 - spriteY1;
          if(areaHeight < MIN_DETECTION_HEIGHT) continue;

          int dx1 = x1 + MIN_DETECTION_WIDTH - spriteWidth;
          int dx2 = x1 + width - MIN_DETECTION_WIDTH;
          dx: for(int dx = dx1; dx <= dx2; dx++) {
            int spriteX1 = Integer.max(0, -dx);
            int spriteX2 = Integer.min(spriteWidth, PIXEL_WIDTH - dx);
            int areaWidth = spriteX2 - spriteX1;
            if(areaWidth < MIN_DETECTION_WIDTH
                || areaWidth * areaHeight < MIN_DETECTION_PIXELS) continue;

            int errors = 0, matched = 0;
            for(int spriteY = spriteY1; spriteY < spriteY2; spriteY++) {
              int screenY = spriteY + dy;
              int yScreen = screenY * PIXEL_WIDTH;
              int ySprite = spriteY * spriteWidth;
              for(int spriteX = spriteX1; spriteX < spriteX2; spriteX++) {
                int screenX = spriteX + dx;
                if(screenX < 0 || screenX >= PIXEL_WIDTH) continue;
                SpritePixelType spriteValue = sprite.data[spriteX + ySprite];
                boolean screenValue = screen[screenX + yScreen];
                switch(spriteValue) {
                  case ON:
                    if(screenValue) matched++; else errors++;
                    break;
                  case OFF:
                    if(!screenValue) matched++;
                    break;
                }
                if(errors > sprite.maxErrors) continue dx;
              }
            }
            if((1.0 * matched / sprite.pixelsQuantity) < sprite.minMatched)
              continue;
            if(bestErrors < 0 || errors < bestErrors) {
              bestDx = dx;
              bestDy = dy;
              bestErrors = errors;
              bestSprite = sprite;
              bestMatched = matched;
              if(errors == 0) break list;
            }
          }
        }
      }

      if(bestErrors < 0) continue;

      if(SHOW_DETECTION_AREA) {
        Graphics2D gO = image.createGraphics();
        gO.setColor(Color.white);
        gO.drawString(bestMatched + "/" + bestErrors, bestDx, bestDy - 3);
      }

      maxErrors = Integer.max(maxErrors, bestErrors);
      maxDifference = Double.max(maxDifference, (1.0 * bestSprite.pixelsQuantity
          - bestMatched) / bestSprite.pixelsQuantity);
      minDetectionWidth = Integer.min(minDetectionWidth, width);
      minDetectionHeight = Integer.min(minDetectionHeight, height);
      minDetectionPixels = Integer.min(minDetectionPixels, width * height);

      int spriteWidth = bestSprite.width;
      int spriteHeight = bestSprite.height;
      for(int spriteY = 0; spriteY < spriteHeight; spriteY++) {
        int screenY = spriteY + bestDy;
        if(screenY < 0 || screenY >= PIXEL_HEIGHT) continue;
        int ySprite = spriteY * spriteWidth;
        for(int spriteX = 0; spriteX < spriteWidth; spriteX++) {
          int screenX = spriteX + bestDx;
          if(screenX < 0 || screenX >= PIXEL_WIDTH) continue;
          BufferedImage repainted = bestSprite.repainted;
          if(repainted == null) {
            switch(bestSprite.data[spriteX + ySprite]) {
              case ON:
                image.setRGB(screenX, screenY, SPRITE_COLOR);
                break;
              case OFF:
                image.setRGB(screenX, screenY, color[0]);
                break;
            }
          } else {
            int value = repainted.getRGB(spriteX, spriteY);
            if(value != 0xFFFF00FF) image.setRGB(screenX, screenY, value);
          }
        }
      }
    }
  }
}
