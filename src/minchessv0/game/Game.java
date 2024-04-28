package minchessv0.game;

import minchessv0.board.Board;
import minchessv0.gen.Gen;
import minchessv0.input.InputHandler;
import minchessv0.move.Move;
import minchessv0.test.Perft;

public enum Game {
    INSTANCE;
    
    public static Game get() {
        return INSTANCE;
    }

    public static void run() {
        Perft.all();
        //get().init();
        //get().loop();
        //Perft.fen("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 1);
    }

    private long[] board;
    private long[][] boardHistory;
    private int boardCount;
    private InputHandler inputHandler;
    private boolean quit;

    private Game() {}

    private void init() {
        this.board = new long[Board.MAX_BITBOARDS];
        this.boardHistory = new long[Board.MAX_BITBOARDS][512];
        this.boardCount = 0;
        this.inputHandler = new InputHandler();
        this.quit = false;
    }

    private void loop() {
        System.out.println("Starting");
        //this.board = Board.fromFen("8/3b4/6k1/5P2/4K3/8/8/8 b - - 0 1");
        this.board = Board.startingPosition();
        System.arraycopy(this.board, 0, this.boardHistory[boardCount ++], 0, this.board.length);
        while(true) {
            String command = inputHandler.getCommand();
            processCommand(command);
            if(quit) break;
        }
    }

    private void processCommand(String command) {
        String[] parts = command.split("\\s+");
        if(parts.length == 0 || parts[0].length() == 0) return;
        String commandToken = parts[0];
        switch(commandToken) {
            case InputHandler.DRAW_COMMAND: {
                inputHandler.commandDraw(this.board);
                break;
            }
            case InputHandler.GO_COMMAND: {
                int depth = 0;
                try {
                    depth = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return;
                }
                inputHandler.commandGo(this.board, depth);
                break;
            }
            case InputHandler.HALT_COMMAND: {
                inputHandler.commandHalt();
                break;
            }
            case InputHandler.MOVE_COMMAND: {
                String move = parts[1];
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
                        long[] boardAfterMove = inputHandler.commandMove(this.board, start, target, promotePiece);
                        if(boardAfterMove != board) {
                            this.board = boardAfterMove;
                            System.arraycopy(this.board, 0, this.boardHistory[boardCount ++], 0, this.board.length);
                            inputHandler.commandDraw(this.board);
                        }
                    }
                }
                break;
            }
            case InputHandler.UNDO_COMMAND: {
                if(boardCount > 1) {
                    System.arraycopy(this.board, 0, this.boardHistory[boardCount - 2], 0, this.boardHistory[boardCount - 2].length);
                    boardCount --;
                    inputHandler.commandDraw(this.board);
                }
                break;
            }
            case InputHandler.EVAL_COMMAND: {
                inputHandler.commandEval(this.board);
                break;
            }
            case InputHandler.FEN_COMMAND: {
                inputHandler.commandFen(parts[1]);
                break;
            }
            case InputHandler.GEN_COMMAND: {
                long[] moveList = inputHandler.commandGen(this.board);
                for(int i = 0; i < moveList[99]; i ++) {
                    System.out.println("Move " + (i + 1) + ": " + Move.string(moveList[i]));
                }
                break;
            }
            case InputHandler.PERFT_COMMAND: {
                inputHandler.commandPerft();
                break;
            }
            case InputHandler.QUIT_COMMAND: {
                this.quit = true;
                break;
            }
            default: {
                System.out.println("entered " + command);
                break;
            }
        }
    }

}
