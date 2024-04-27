package minchessv0.util;

public class Piece {
    
    private final static int BLACK = 8;

    public final static int EMPTY = 0;
    public final static int KING = 1;
    public final static int WHITE_KING = KING;
    public final static int QUEEN = 2;
    public final static int WHITE_QUEEN = QUEEN;
    public final static int ROOK = 3;
    public final static int WHITE_ROOK = ROOK;
    public final static int BISHOP = 4;
    public final static int WHITE_BISHOP = BISHOP;
    public final static int KNIGHT = 5;
    public final static int WHITE_KNIGHT = KNIGHT;
    public final static int PAWN = 6;
    public final static int WHITE_PAWN = PAWN;
    public final static int TYPE = 7;
    public final static int BLACK_KING = BLACK|KING;
    public final static int BLACK_QUEEN = BLACK|QUEEN;
    public final static int BLACK_ROOK = BLACK|ROOK;
    public final static int BLACK_BISHOP = BLACK|BISHOP;
    public final static int BLACK_KNIGHT = BLACK|KNIGHT;
    public final static int BLACK_PAWN = BLACK|PAWN;

    public final static int[] VALUE = { 0, 20000, 975, 500, 330, 320, 100 };

    public final static String[] SHORT_STRING = { ".", "K", "Q", "R", "B", "N", "P", ".", ".", "k", "q", "r", "b", "n", "p"};
    public final static String[] LONG_STRING = { "Empty", "White King", "White Queen", "White Rook", "White Bishop", "White Knight", "White Pawn", "None", "None", "Black King", "Black Queen", "Black Rook", "Black Bishop", "Black Knight", "Black Pawn"};

    public static final int[] PAWN_ADVANCE = { 8, -8 };
	public final static int[] SLIDE = { 8, 1, -8, -1, 9, -7, -9, 7 };
	public final static int[] LEAP = { 17, 10, -6, -15, -17, -10, 6, 15 };
	public final static int[][] PAWN_CAPTURE = { { 7, 9 }, { -7, -9 } };

    private Piece() {}

}
