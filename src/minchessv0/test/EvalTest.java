package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.EvalVerbose;

public class EvalTest {
    
    public static void test() {
        long[] board = Board.startingPosition();
        Board.drawText(board);
        System.out.println(new EvalVerbose(board).eval());
    }

    private EvalTest() {}

}
