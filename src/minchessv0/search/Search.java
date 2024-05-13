package minchessv0.search;

public interface Search {
    
    public long beginSearch(long[] board, int maxDepth, int time);
    public void requestHalt();
    public long bestMove();
    public String pv();
    public long nodes();

}
