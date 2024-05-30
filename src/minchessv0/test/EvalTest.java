package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.Score;
import minchessv0.move.Move;

public class EvalTest {
    
    public static void test() {
        long[] board = Board.fromFen("rnbq1bnr/pppppkpp/5p2/8/8/2N2P2/PPPPPKPP/R1BQ1BNR b - - 0 1");
        Board.drawText(board);
        Score score = new Eval(board).score();
        int eval = score.eval();
        Board.drawText(score.board());
        System.out.println(score.stringAll() + "\n" + score.stringCriteriaTotal());
    }

    private EvalTest() {}

}
