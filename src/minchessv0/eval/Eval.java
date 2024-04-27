package minchessv0.eval;

import minchessv0.board.Board;
import minchessv0.util.Piece;

public class Eval {
    
    public static int eval(long[] board) {
        int eval = (Board.materialValue(board, 0) - Board.materialValue(board, 1)) * (Board.player(board) == 0 ? 1 : -1);
        long bitboard;
        int perspective;
        int square;
        for(int type = Piece.KING; type <= Piece.PAWN; type ++) {
            for(int player = 0; player < 2; player ++) {
                perspective = Board.player(board) == player ? 1 : -1;
                bitboard = board[type | (player << 3)];
                for(; bitboard != 0L; bitboard &= bitboard - 1) {
                    square = Long.numberOfTrailingZeros(bitboard);
                    eval += Psqt.BONUS[type][player][square][0] * perspective;
                }
            }
        }
        return eval;
    }

    private Eval() {}

}
