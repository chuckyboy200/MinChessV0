package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.EvalVerbose;
import minchessv0.move.Move;

public class SearchTest {
    
    public static void test() {
        long[] board = Board.startingPosition();
        Board.drawText(board);
        SearchParallelTest search = new SearchParallelTest(board, 8, 1000000);
        search.run();
    }

    private SearchTest() {}

}
