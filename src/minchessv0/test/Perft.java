package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.gen.Gen;
import minchessv0.move.Move;

public class Perft {

    private final static String[] POSITION_NAME = {
        "1. Initial position ",
        "2.",
        "3.",
        "4.",
        "5.",
        "6.",
        "7.",
        "8. Enpassant capture gives check",
        "9. Short castling gives check",
        "10. Long castling gives check",
        "11. Castling",
        "12. Castling prevented",
        "13. Promote out of check",
        "14. Discovered check",
        "15. Promotion gives check",
        "16. Underpromotion gives check",
        "17. Self stalemate",
        "18. Stalemate/Checkmate",
        "19. Double check"
    };

    private final static String[] POSITION_FEN = {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
        "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
        "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6",
        "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
        "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
        "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1",
        "5k2/8/8/8/8/8/8/4K2R w K - 0 1",
        "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1",
        "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1",
        "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1",
        "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
        "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1",
        "4k3/1P6/8/8/8/8/K7/8 w - - 0 1",
        "8/P1k5/K7/8/8/8/8/8 w - - 0 1",
        "K1k5/8/P7/8/8/8/8/8 w - - 0 1",
        "8/k1P5/8/1K6/8/8/8/8 w - - 0 1",
        "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1"
    };

    private final static long[] POSITION_PERFT_VALUE = {
        119060324,193690690,178633661,706045033,53392,6923051137L,824064,
        1440467,661072,803711,1274206,1720476,3821001,1004658,217342,
        92683,2217,567584,23527
    };

    public final static int[] POSITION_PERFT_DEPTH = {
            6,5,7,6,3,6,6,6,6,6,4,4,6,5,6,6,6,7,4
    };

    private static long elapsedTime;

    private Perft() {}

    public final static void all() {
        some(0, POSITION_NAME.length - 1);
    }

    public final static long some(int firstPosition, int lastPosition) {
        elapsedTime = 0;
        long totalTime = 0;
        for(int positionNumber = firstPosition; positionNumber <= lastPosition; positionNumber ++) {
            println(POSITION_NAME[positionNumber]);
            long thisPositionTotal = fen(POSITION_FEN[positionNumber], POSITION_PERFT_DEPTH[positionNumber]);
            totalTime += elapsedTime;
            if(thisPositionTotal == POSITION_PERFT_VALUE[positionNumber]) {
                println("Passed\n");
            } else {
                println("Failed ( " + POSITION_PERFT_VALUE[positionNumber] + " ( " + (POSITION_PERFT_VALUE[positionNumber] - thisPositionTotal) + " ) )\n");
            }
            if(positionNumber < lastPosition) {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        println("Done\n");
        return totalTime;
    }

    public final static long fen(String fen, int depth) {
        long[] board = Board.fromFen(fen);
        println("\"" + fen + "\"");
        Board.drawText(board);
        return perftPositionSpeed(board, depth);
    }

    public final static long fen(String fen, String moveString, int depth) {
        long[] board = Board.fromFen(fen);
        println("\"" + fen + "\"");
        Board.drawText(board);
        return perftPositionDebug(board, depth, moveString + (moveString.length() == 4 ? " " : ""));
    }

    private final static long perftPositionSpeed(long[] board, int maxDepth) {
        int maxNum = 0;
        long currentTime = 0L;
        long nodes = 0L;
        long total = 0L;
        for(int depth = 1; depth <= maxDepth; depth ++) {
            currentTime = System.currentTimeMillis();
            nodes = perftSearchSpeed(board, depth, maxDepth, maxNum);
            elapsedTime = System.currentTimeMillis() - currentTime;
            println("Positions for depth " + depth + "/" + maxDepth + " = " + nodes + " Elapsed: " + (elapsedTime));
            if(depth == 1) {
                maxNum = (int) nodes;
            }
            if(depth == maxDepth) {
                double nodesPerSecond = elapsedTime == 0 ? nodes : nodes / elapsedTime;
                println("Nodes per second: " + nodesPerSecond + " mil");
                total = nodes;
            }
        }
        return total;
    }

    private final static long perftSearchSpeed(long[] board, int depth, int maxDepth, int maxNum) {
        if(depth == 0) return 1;
        long nodes = 0L;
        long tempNodes = 0L;
        long[] moves = Gen.gen(board, false, false);
        int maxMoves = (int) moves[99];
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        int legalMoveNum = 0;
        String moveString = "";
        long currentTime = 0L;
        for(int move = 0; move < maxMoves; move ++) {
            long[] boardAfterMove = Board.makeMove(board, moves[move]);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            if(depth == maxDepth) {
                moveString = Move.string(moves[move]);
                print(" " + ((++ legalMoveNum <= 9) ? " " : "") + legalMoveNum + "/" + maxNum + " " + (moveString.length() == 4 ? " " : "") + moveString + ": ");
            }
            tempNodes = nodes;
            currentTime = System.currentTimeMillis();
            nodes += perftSearchSpeed(boardAfterMove, depth - 1, maxDepth, maxNum);
            if(depth == maxDepth) {
                println("" + (nodes - tempNodes) + "   Elapsed: " + (System.currentTimeMillis() - currentTime));
            }
        }
        return nodes;
    }

    private static final long perftPositionDebug(long[] board, int maxDepth, String moveString) {
        for(int i = 0; i < moveString.length(); i += 5) {
            int move = Move.stringToInt(board, moveString.substring(i, i + 5));
            println("Making move: " + Move.string(move) + " " + Move.verbose(move));
            board = Board.makeMove(board, move | (Board.getSquare(board, move & 0x3f) << 16));
            Board.drawText(board);
        }
        int maxNum = 0;
        long currentTime = 0L;
        long nodes = 0L;
        long total = 0L;
        for(int depth = 1; depth <= maxDepth; depth ++) {
            currentTime = System.currentTimeMillis();
            nodes = perftSearchSpeed(board, depth, maxDepth, maxNum);
            elapsedTime = System.currentTimeMillis() - currentTime;
            println("Positions for depth " + depth + "/" + maxDepth + " = " + nodes + " Elapsed: " + (elapsedTime));
            if(depth == 1) {
                maxNum = (int) nodes;
            }
            if(depth == maxDepth) {
                total = nodes;
            }
        }
        return total;
    }
/*
    private static final long perftPositionLegal(long[] board, int maxDepth) {
        int maxNum = 0;
        long currentTime = 0L;
        long nodes = 0L;
        long total = 0L;
        for(int depth = 1; depth <= maxDepth; depth ++) {
            currentTime = System.currentTimeMillis();
            nodes = perftSearchLegal(board, depth, maxDepth, maxNum);
            elapsedTime = System.currentTimeMillis() - currentTime;
            println("Positions for depth " + depth + "/" + maxDepth + " = " + nodes + " Elapsed: " + (elapsedTime));
            if(depth == 1) {
                maxNum = (int) nodes;
            }
            if(depth == maxDepth) {
                double nodesPerSecond = elapsedTime == 0 ? nodes : nodes / elapsedTime;
                println("Nodes per second: " + nodesPerSecond + " mil");
                total = nodes;
            }
        }
        return total;
    }

    private final static long perftSearchLegal(long[] board, int depth, int maxDepth, int maxNum) {
        if(depth == 0) return 1;
        long nodes = 0L;
        long tempNodes = 0L;
        int[] moves = Gen.gen(board, true, false);
        int maxMoves = moves[99];
        int legalMoveNum = 0;
        String moveString = "";
        long currentTime = 0L;
        for(int move = 0; move < maxMoves; move ++) {
            long[] boardAfterMove = Board.makeMove(board, moves[move]);
            if(depth == maxDepth) {
                moveString = Board.moveString(moves[move]);
                print(" " + ((++ legalMoveNum <= 9) ? " " : "") + legalMoveNum + "/" + maxNum + " " + (moveString.length() == 4 ? " " : "") + moveString + ": ");
            }
            tempNodes = nodes;
            currentTime = System.currentTimeMillis();
            nodes += perftSearchLegal(boardAfterMove, depth - 1, maxDepth, maxNum);
            if(depth == maxDepth) {
                println("" + (nodes - tempNodes) + "   Elapsed: " + (System.currentTimeMillis() - currentTime));
            }
        }
        return nodes;
    }
*/
    private static void println(String text) {
        print(text + "\n");
    }

    private static void print(String text) {
        System.out.print(text);
    }

}

