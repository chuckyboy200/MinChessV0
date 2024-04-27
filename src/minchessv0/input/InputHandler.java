package minchessv0.input;

import java.util.Scanner;

import minchessv0.board.Board;
import minchessv0.eval.Eval;
import minchessv0.gen.Gen;
import minchessv0.test.Perft;
import minchessv0.search.Search;

public class InputHandler {

    public static final String DRAW_COMMAND = "draw";
    public static final String GO_COMMAND = "go";
    public static final String QUIT_COMMAND = "quit";
    public static final String HALT_COMMAND = "halt";
    public static final String MOVE_COMMAND = "move";
    public static final String UNDO_COMMAND = "undo";
    public static final String EVAL_COMMAND = "eval";
    public static final String FEN_COMMAND = "fen";
    public static final String GEN_COMMAND = "gen";
    public static final String PERFT_COMMAND = "perft";

    public InputHandler() {
        this.scanner = new Scanner(System.in);
    }

    public String getCommand() {
        return scanner.nextLine();
    }

    public void commandDraw(long[] board) {
        Board.drawText(board);
    }

    public void commandGo(long[] board, int depth) {
        System.out.println("Searching to depth " + depth);
        if (searchThread != null && searchThread.isAlive()) {
            System.out.println("Error: Search is already running.");
        } else {
            searchTask = new Search(board, depth);
            searchThread = new Thread(searchTask);
            searchThread.start();
        }
    }

    public void commandEval(long[] board) {
        System.out.println("Eval: " + Eval.eval(board));
    }

    public void commandHalt() {
        if (searchThread != null && searchThread.isAlive()) {
            searchTask.requestHalt();
        }
    }

    public long[] commandMove(long[] board, int start, int target, int promote) {
        long[] moveList = Gen.gen(board, true, false);
        for(long move : moveList) {
            if((move & Board.SQUARE_BITS) == start && (move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS) == target && (move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS) == promote) {
                return Board.makeMove(board, move);
            }
        }
        return board;
    }

    public long[] commandFen(String fen) {
        return Board.fromFen(fen);
    }

    public long[] commandGen(long[] board) {
        return Gen.gen(board, true, false);
    }

    public void commandPerft() {
        Perft.all();
    }
    
    private Scanner scanner;
    private Search searchTask;
    private Thread searchThread;

}
