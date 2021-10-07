public class Block extends Screen {
  private static boolean usedBlock(int x, int y) {
    int i = x | (y << 5);
    Chunk chunk = chunks[i];
    if(chunk.used) return true;
    chunk.used = true;
    
    if(chunk.equals(background[i])) {
      chunks[i] = background[i];
      return true;
    }
    
    return false;
  }
  
  public static boolean findBlock(int x, int y) {
    if(usedBlock(x, y)) return false;
    
    blockX1 = blockX2 = x;
    blockY1 = blockY2 = y;
    
    spawn(x, y);
    
    return true;
  }
  
  private static void spawn(int x, int y) {
    blockX1 = Integer.min(blockX1, x);
    blockY1 = Integer.min(blockY1, y);
    blockX2 = Integer.max(blockX2, x);
    blockY2 = Integer.max(blockY2, y);
    
    if(x > 0) check(x - 1, y);
    if(y > 0) check(x, y - 1);
    if(x < 31) check(x + 1, y);
    if(y < 23) check(x, y + 1);
  }
  
  private static void check(int x, int y) {
    if(usedBlock(x, y)) return;
    spawn(x, y);
  }
}
