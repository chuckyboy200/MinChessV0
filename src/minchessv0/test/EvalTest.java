package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.Score;
import minchessv0.move.Move;

public class EvalTest {
    
    public static void test() {
        long[] board = Board.startingPosition();
        long move = Move.stringToInt(board, Move.string(Move.convertStartTarget(12, 28)));
        board = Board.makeMove(board, move);
        Board.drawText(board);
        SearchParallelTest search = new SearchParallelTest(board, 6, 10000);
        search.run();
    }

    private EvalTest() {}

}
