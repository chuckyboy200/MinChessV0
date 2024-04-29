package minchessv0.game;

import java.util.ArrayList;
import java.util.List;

import minchessv0.board.Board;
import minchessv0.eval.Eval;
import minchessv0.gen.Gen;
import minchessv0.input.InputHandler;
import minchessv0.move.Move;
import minchessv0.search.Search;
import minchessv0.test.Perft;
import minchessv0.util.Value;

public enum Game {
    INSTANCE;
    
    public static Game get() {
        return INSTANCE;
    }

    public static void run() {
        //Perft.all();
        get().init();
        get().loop();
        //Perft.fen("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 1);
    }

    private long[] board;
    private long[][] boardHistory;
    private int boardCount;
    private InputHandler inputHandler;
    private boolean quit;
    private List<String> commandParts;
    private Search searchTask;
    private Thread searchThread;

    private Game() {}

    private void init() {
        this.board = new long[Board.MAX_BITBOARDS];
        this.boardHistory = new long[Board.MAX_BITBOARDS][512];
        this.boardCount = 0;
        this.inputHandler = new InputHandler();
        this.commandParts = new ArrayList<>();
        this.quit = false;
    }

    private void loop() {
        System.out.println("Starting");
        //this.board = Board.fromFen("8/3b4/6k1/5P2/4K3/8/8/8 b - - 0 1");
        this.board = Board.startingPosition();
        System.arraycopy(this.board, 0, this.boardHistory[boardCount ++], 0, this.board.length);
        while(true) {
            this.commandParts = inputHandler.getCommand();
            if(this.commandParts.size() != 0) {
                String commandToken = this.commandParts.get(0);
                switch(commandToken) {
                    case InputHandler.DRAW_COMMAND: {
                        Board.drawText(this.board);
                        break;
                    }
                    case InputHandler.GO_COMMAND: {
                        int depth = 0;
                        try {
                            depth = Integer.parseInt(this.commandParts.get(1));
                        } catch (NumberFormatException e) {
                            
                        }
                        System.out.println("Searching to depth " + depth);
                        if (searchThread != null && searchThread.isAlive()) {
                            System.out.println("Error: Search is already running.");
                        } else {
                            searchTask = new Search(board, depth);
                            searchThread = new Thread(searchTask);
                            searchThread.start();
                        }
                        break;
                    }
                    case InputHandler.HALT_COMMAND: {
                        if (searchThread != null && searchThread.isAlive()) {
                            searchTask.requestHalt();
                        }
                        break;
                    }
                    case InputHandler.MOVE_COMMAND: {
                        String move = this.commandParts.get(1);
                        if(move.length() == 4 || move.length() == 5) {
                            int start = (move.charAt(0) - 'a') | ((move.charAt(1) - '1') << 3);
                            int target = (move.charAt(2) - 'a') | ((move.charAt(3) - '1') << 3);
                            int promotePiece = 0;
                            if(move.length() == 5) {
                                promotePiece = "qrbn".indexOf(move.charAt(4));
                                System.out.println(promotePiece);
                                if(promotePiece == -1) break;
                                promotePiece = (promotePiece + 2) | (Board.player(this.board) << 3);
                            }
                            if(start >= 0 && start <= 63 && target >= 0 && target <= 63) {
                                long[] moveList = Gen.gen(this.board, true, false);
                                int m = Move.isValid(moveList, start, target);
                                if(m != Value.INVALID) {
                                    long[] boardAfterMove = Board.makeMove(board, m);
                                    if(boardAfterMove != board) {
                                        this.board = boardAfterMove;
                                        System.arraycopy(this.board, 0, this.boardHistory[boardCount ++], 0, this.board.length);
                                        Board.drawText(this.board);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case InputHandler.UNDO_COMMAND: {
                        if(boardCount > 1) {
                            System.arraycopy(this.board, 0, this.boardHistory[boardCount - 2], 0, this.boardHistory[boardCount - 2].length);
                            boardCount --;
                            Board.drawText(this.board);
                        }
                        break;
                    }
                    case InputHandler.EVAL_COMMAND: {
                        System.out.println(Eval.eval(this.board));
                        break;
                    }
                    case InputHandler.FEN_COMMAND: {
                        this.board = Board.fromFen(commandParts.get(1));
                        break;
                    }
                    case InputHandler.GEN_COMMAND: {
                        long[] moveList = Gen.gen(this.board, true, false);
                        for(int i = 0; i < moveList[99]; i ++) {
                            System.out.println("Move " + (i + 1) + ": " + Move.string(moveList[i]));
                        }
                        break;
                    }
                    case InputHandler.PERFT_COMMAND: {
                        Perft.all();
                        break;
                    }
                    case InputHandler.QUIT_COMMAND: {
                        this.quit = true;
                        break;
                    }
                    default: {
                        System.out.println("entered " + commandParts.get(0));
                        break;
                    }
                }
                if(quit) break;
            }
        }
    }
}
