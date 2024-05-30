package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.EvalVerbose;
import minchessv0.move.Move;

public class SearchTest {
    
    public static void test() {
        long[] board = Board.startingPosition();
        Board.drawText(board);
        SearchParallelTest2 search = new SearchParallelTest2(board, 6, 1000000, false);
        search.run();
    }

    private SearchTest() {}

}
