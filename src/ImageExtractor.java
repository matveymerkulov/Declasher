public class ImageExtractor extends Main {
  private static boolean[] screen, background;
  private static int[] pixels;
  private static int x1, y1, x2, y2, imageNumber;
  private static final int SAME = 0, CHANGED = 1;
  
  public static void extract(boolean[] scr, boolean[] backgr) {
    screen = scr;
    background = backgr;
    imageNumber = 1;
    
    pixels = new int[PIXEL_SIZE];
    for(int addr = 0; addr < PIXEL_SIZE; addr++)
      pixels[addr] = screen[addr] == background[addr] ? SAME : CHANGED;
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      for(int x = 0; x < PIXEL_WIDTH; x++) {
        int addr = ySource | x;
        if(pixels[addr] == CHANGED) {
          x1 = x2 = x;
          y1 = y2 = y;
          imageNumber++;
          pixels[addr] = imageNumber;
          check(x, y);
          if(Image.hasAcceptableSize(x1, y1, x2, y2))
            images.add(new Image(pixels, screen, background, x1, y1, x2, y2
                , imageNumber));
        }
      }
    }
  }

  private static void check(int x, int y) {
    for(int dy = -MAX_DISTANCE; dy <= MAX_DISTANCE; dy++) {
      int yy = y + dy;
      int yAddr = yy * PIXEL_WIDTH;
      if(yy < 0 || yy >= PIXEL_HEIGHT) continue;
      for(int dx = -MAX_DISTANCE; dx <= MAX_DISTANCE; dx++) {
        if(dx == 0 && dy == 0) continue;
        int xx = x + dx;
        if(xx < 0 || xx >= PIXEL_WIDTH) continue;
        int addr = x + dx + yAddr;
        if(pixels[addr] == CHANGED) {
          x1 = Integer.min(x1, xx);
          y1 = Integer.min(y1, yy);
          x2 = Integer.max(x2, xx);
          y2 = Integer.max(y2, yy);
          pixels[addr] = imageNumber;
          check(xx, yy);
        }
      }
    }
  }
}
