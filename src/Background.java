import java.io.IOException;
import java.util.LinkedList;

public class Background extends Screen {  
  private static class ChunkList extends LinkedList<Chunk> {};
  private static final ChunkList[] chunkList = new ChunkList[768];

  public static void compose(int from, int to) throws IOException {
    for(int i = 0; i < 768; i++) chunkList[i] = new ChunkList();
    
    for(int num = from; num <= to; num++) {
      load(num);
      addChunks();
    }

    setMaxChunk();
  }

  private static void addChunks() {
    chunk: for(int i = 0; i < 768; i++) {
      ChunkList list = chunkList[i];
      Chunk oldChunk = chunks[i];
      for(Chunk chunk: list) {
        if(chunk.equals(oldChunk)) {
          chunk.quantity++;
          continue chunk;
        }
      }
      list.add(oldChunk);
    }
  }

  private static void setMaxChunk() {
    for(int i = 0; i < 768; i++) {
      ChunkList list = chunkList[i];
      Chunk maxChunk = null;
      int maxQuantity = 1;
      for(Chunk chunk: list) {          
        if(chunk.quantity > maxQuantity) {
          maxChunk = chunk;
          maxQuantity = chunk.quantity;
        }
      }
      background[i] = maxChunk;
    }
  }

}
