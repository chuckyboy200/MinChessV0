package minchessv0.util;

public class Value {
    
    public final static int INVALID = -1;
	public final static int EMPTY = 0;
	public final static int NONE = 0;
	public final static int WHITE = 0;
	public final static int WHITE_BIT = 0;
	public final static int BLACK = 1;
	public final static int BLACK_BIT = 8;
	public final static int[] KINGSIDE_BIT = { 1, 4 };
	public final static int[] QUEENSIDE_BIT = { 2, 8 };
	public final static int FILE = 7;
	public final static int FILE_A = 0;
	public final static int FILE_B = 1;
	public final static int FILE_C = 2;
	public final static int FILE_D = 3;
	public final static int FILE_E = 4;
	public final static int FILE_F = 5;
	public final static int FILE_G = 6;
	public final static int FILE_H = 7;

	public final static String FILE_STRING = "abcdefgh";

    private Value() {}

}