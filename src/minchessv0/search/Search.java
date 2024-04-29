package minchessv0.search;

import minchessv0.board.Board;
import minchessv0.eval.Eval;
import minchessv0.gen.Gen;
import minchessv0.move.Move;

public class Search implements Runnable {

    public Search() {
        this(new long[1], 1);
    }

    public Search(long[] board,int depth) {
        this.root = new long[board.length];
        System.arraycopy(board, 0, this.root, 0, board.length);
        this.maxDepth = depth;
        this.haltRequested = false;
        this.isHalted = false;
    }

    @Override
    public void run() {
        init();
        beginSearch();
    }

    public void requestHalt() {
        this.haltRequested = true;
    }

    public long getBestMove() {
        return isHalted ? this.bestMove : -1;
    }

    private static final int INFINITY = 999000;
    private static final int MAX_PV_LENGTH = 100;

    private long[] root;
    private long[] rootMoves;
    private int maxDepth;
    private long bestMove;
    private boolean haltRequested;
    private boolean isHalted;
    private long nodeCount;
    private long[] rootPV;
    
    private void init() {
        this.rootMoves = Gen.gen(this.root, true, false);
    }

    private void beginSearch() {
        this.nodeCount = 0;
        long[] boardAfterMove;
        long move;
        int eval;
        int bestEval = -INFINITY;
        rootPV = new long[MAX_PV_LENGTH];
        long[] tempPV = new long[100];
        for(int i = 0; i < this.rootMoves[99]; i ++) {
            move = this.rootMoves[i];
            System.out.print("Move " + (i + 1) + ": " + Move.string(move) + " - ");
            boardAfterMove = Board.makeMove(root, move);
            tempPV[0] = 0;
            //eval = -search(boardAfterMove, this.maxDepth, -INFINITY, INFINITY, tempPV);
            eval = -straightSearch(boardAfterMove, maxDepth);
            System.out.println(eval);
            if(eval > bestEval) {
                bestEval = eval;
                bestMove = move;
                rootPV[0] = tempPV[0] + 1;
                rootPV[1] = move;
                for(int j = 0; j < tempPV[0]; j ++) {
                    rootPV[j + 2] = tempPV[j + 1];
                }
            }
        }
        System.out.println("PV: " + pvString(rootPV));
        System.out.println("Total nodes " + nodeCount);
    }

    private int search(long[] board, int depth, int alpha, int beta, int[] pv) {
        //System.out.println("depth " + depth);
        //Board.drawText(board);
        //try { Thread.sleep(2000); } catch(InterruptedException e) { e.printStackTrace(); }
        nodeCount ++;
        if(haltRequested) return alpha;
        if(depth == 0) return Eval.eval(board);
        long[] localPV = new long[MAX_PV_LENGTH];
        long[] moveList = Gen.gen(board, false, false);
        //if(moveList[99] == 0) return Board.isPlayerInCheck(board, Board.player(board)) ? -INFINITY : 0;
        int player = Board.player(board);
        long move;
        long[] boardAfterMove;
        int eval;
        int maxEval = -INFINITY;
        int[] childPV = new int[MAX_PV_LENGTH];
        for(int i = 0; i < moveList[99]; i ++) {
            move = moveList[i];
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            //System.out.println(((Board.player(board)) == 0 ? "White ": "Black ") + "move " + Board.moveString(move));
            childPV[0] = 0;
            eval = -search(boardAfterMove, depth - 1, -beta, -alpha, childPV);
            if(eval > maxEval) {
                localPV[0] = childPV[0] + 1;
                localPV[1] = move;
                for(int j = 0; j < childPV[0]; j ++) {
                    localPV[j + 2] = childPV[j + 1];
                }
                if(eval > alpha) {
                    alpha = eval;
                    if(eval >= beta) {
                        break;
                    }
                }
            }
        }
        System.arraycopy(localPV, 0, pv, 0, localPV.length);
        return alpha;
    }

    private int straightSearch(long[] board, int depth) {
        if(depth == 0) return Eval.eval(board);
        long[] moveList = Gen.gen(board, true, false);
        if(moveList[99] == 0) {
            return Board.isPlayerInCheck(board, Board.player(board)) ? -INFINITY : 0;
        }
        int bestEval = -INFINITY;
        long move;
        long[] boardAfterMove;
        int eval;
        for(int i = 0; i < moveList[99]; i ++) {
            move = moveList[i];
            boardAfterMove = Board.makeMove(board, move);
            eval = -straightSearch(boardAfterMove, depth - 1);
            if(eval > bestEval) bestEval = eval;
        }
        return bestEval;
    }
    private String pvString(long[] pv) {
        String pvString = "";
        for(int i = 1; i <= pv[0]; i ++) {
            pvString += Move.string(pv[i]) + " ";
        }
        return pvString;
    }

}
