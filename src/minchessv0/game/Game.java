package minchessv0.game;

import java.util.ArrayList;
import java.util.List;

import minchessv0.board.Board;
import minchessv0.eval.Eval;
import minchessv0.gen.Gen;
import minchessv0.move.Move;
import minchessv0.search.Search;
import minchessv0.search.SearchParallel;
import minchessv0.test.EvalTest;
import minchessv0.test.Perft;
import minchessv0.test.SearchTest;
import minchessv0.uci.CommandQueue;
import minchessv0.uci.Engine;
import minchessv0.util.Value;

public enum Game {
    INSTANCE;

    public void run() {
        //Perft.all();
        //init();
        //loop();
        test();
    }

    public void sendCommand(String command) {
        this.commandQueue.add(command);
    }

    public void sendCommand(String command, String arg) {
        this.commandQueue.add(command);
        this.commandQueue.add(arg);
    }

    public void sendPriorityCommand(String command) {
        this.commandQueue.addFront(command);
    }

    public void sendPriorityCommand(String command, String arg) {
        this.commandQueue.addFront(arg);
        this.commandQueue.addFront(command);
    }

    public long[] getBoard() {
        return this.board;
    }

    private long[] board;
    private long[][] boardHistory;
    private int boardCount;
    private boolean quit;
    private List<String> commandParts;
    private CommandQueue commandQueue;
    private Engine engine;
    private Thread UCIThread;
    private boolean executeCommands;
    private int maxDepth;
    private Thread searchThread;
    private Search searchTask;
    private int maxSearchTime;
    private int whiteTimeRemaining;
    private int blackTimeRemaining;

    private Game() {}

    private void init() {
        this.board = new long[Board.MAX_BITBOARDS];
        this.boardHistory = new long[Board.MAX_BITBOARDS][512];
        this.boardCount = 0;
        this.commandParts = new ArrayList<>();
        this.quit = false;
        this.commandQueue = new CommandQueue();
        this.engine = new Engine();
        this.UCIThread = new Thread(this.engine);
        this.UCIThread.start();
        this.executeCommands = true;
        this.maxDepth = 100;
        this.maxSearchTime = 5000;
        this.whiteTimeRemaining = 120000;
        this.blackTimeRemaining = 120000;
        //Window.init();
    }

    private void loop() {
        System.out.println("Starting");
        //this.board = Board.fromFen("8/3b4/6k1/5P2/4K3/8/8/8 b - - 0 1");
        //test();
        this.board = Board.startingPosition();
        System.arraycopy(this.board, 0, this.boardHistory[boardCount ++], 0, this.board.length);
        while(!quit) {
            if(this.commandQueue.hasNext() && this.executeCommands) {
                String command = this.commandQueue.getNext();
                switch(command) {
                    case "uci": {
                        System.out.println("id name MinChessV0");
                        System.out.println("id author Charles Clark");
                        System.out.println("uciok");
                        break;
                    }
                    case "isready": {
                        // do check for engine ready for new commands
                        System.out.println("readyok");
                        break;
                    }
                    case "fen": {
                        String fen = this.commandQueue.getNext();
                        this.board = Board.fromFen(fen);
                        //Board.drawText(this.board);
                        break;
                    }
                    case "startpos": {
                        this.board = Board.startingPosition();
                        //Board.drawText(this.board);
                        break;
                    }
                    case "moves": {
                        String moveString = this.commandQueue.getNext();
                        int move = Move.stringToInt(this.board, moveString);
                        System.out.println(moveString + "->" + Move.string(move));
                        this.board = Board.makeMove(this.board, move);
                        //Board.drawText(this.board);
                        break;
                    }
                    case "depth": {
                        this.maxDepth = Integer.parseInt(this.commandQueue.getNext());
                        break;
                    }
                    case "infinite": {
                        this.maxDepth = 100;
                        break;
                    }
                    case "stop": {
                        if(this.searchThread != null && this.searchThread.isAlive()) {
                            this.searchTask.requestHalt();
                        }
                        break;
                    }
                    case "search": {
                        if(this.searchThread != null && this.searchThread.isAlive()) {
                            this.searchTask.requestHalt();
                            try {
                                this.searchThread.join();
                            } catch(InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        int thisSearchTime = this.maxSearchTime;
                        switch((int) this.board[Board.STATUS] & Board.PLAYER_BIT) {
                            case 0: {
                                if(this.whiteTimeRemaining < 60000) {
                                    thisSearchTime = this.whiteTimeRemaining / 15;
                                }
                            }
                            case 1: {
                                if(this.blackTimeRemaining < 60000) {
                                    thisSearchTime = this.blackTimeRemaining / 15;
                                }
                            }
                        }
                        this.searchTask = new SearchParallel(this.board, this.maxDepth, thisSearchTime);
                        this.searchThread = new Thread((Runnable) this.searchTask);
                        this.searchThread.start();
                    }
                    case "makemove": {
                        String move = this.commandQueue.getNext();
                        if(move.length() == 4 || move.length() == 5) {
                            int start = (move.charAt(0) - 'a') | ((move.charAt(1) - '1') << 3);
                            int target = (move.charAt(2) - 'a') | ((move.charAt(3) - '1') << 3);
                            int promotePiece = 0;
                            if(move.length() == 5) {
                                promotePiece = "qrbn".indexOf(move.charAt(4));
                                //System.out.println(promotePiece);
                                if(promotePiece == -1) break;
                                promotePiece = (promotePiece + 2) | (Board.player(this.board) << 3);
                            }
                            if(start >= 0 && start <= 63 && target >= 0 && target <= 63) {
                                long[] moveList = Gen.gen(this.board, true, false);
                                long m = Move.isValid(moveList, start, target);
                                if(m != Value.INVALID) {
                                    long[] boardAfterMove = Board.makeMove(board, m);
                                    System.arraycopy(boardAfterMove, 0, this.board, 0, boardAfterMove.length);
                                    System.arraycopy(this.board, 0, this.boardHistory[boardCount ++], 0, this.board.length);
                                    //Board.drawText(this.board);
                                }
                            }
                        }
                        break;
                    }
                    case "eval": {
                        System.out.println("eval " + new Eval(this.board).eval());
                        break;
                    }
                    case "searchcomplete": {
                        System.out.println("bestmove " + Move.string(this.searchTask.bestMove()));
                        break;
                    }
                    case "movetime": {
                        this.maxSearchTime = Integer.parseInt(this.commandQueue.getNext());
                        break;
                    }
                    case "pv": {
                        System.out.println("PV: " + this.searchTask.pv());
                        break;
                    }
                    case "quit": {
                        this.quit = true;
                        break;
                    }
                    case "ucinewgame": {
                        // indicate that the next search begins searching on a new game
                        // so reset all game parameters so that they don't carry over
                        // from previous game
                        break;
                    }
                    case "wtime": {
                        this.whiteTimeRemaining = Integer.parseInt(this.commandQueue.getNext());
                        break;
                    }
                    case "btime": {
                        this.blackTimeRemaining = Integer.parseInt(this.commandQueue.getNext());
                        break;
                    }
                    case "draw": {
                        Board.drawText(this.board);
                        break;
                    }
                }
            }
        }
    }

    private void test() {
        SearchTest.test();
    }
}
