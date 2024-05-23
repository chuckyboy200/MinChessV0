package minchessv0.search;

public interface Search {
    
    public void requestHalt();
    public long bestMove();
    public String pv();
    public long nodes();

}
