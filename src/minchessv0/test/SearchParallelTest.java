package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.Eval;
import minchessv0.game.Game;
import minchessv0.gen.Gen;
import minchessv0.move.Move;
import minchessv0.search.Search;
import minchessv0.sort.Sort;
import minchessv0.util.Piece;

public class SearchParallelTest implements Search, Runnable {
    
    public SearchParallelTest(long[] board, int maxDepth, long maxSearchTime) {
        this.board = new long[Board.MAX_BITBOARDS];
        System.arraycopy(board, 0, this.board, 0, Board.MAX_BITBOARDS);
        this.rootMoveList = Gen.gen(this.board, true, false);
        this.maxDepth = maxDepth;
        this.maxSearchTime = maxSearchTime;
        this.searchRunning = false;
        this.searchHalted = false;
        this.rootPV = new int[MAX_PV_LENGTH];
        this.sendInfoDelay = 100000;
    }

    @Override
    public void run() {
        init();
        this.searchRunning= true;
        while(this.searchRunning) {
            try {
                doSearch();
            } catch(InterruptedException e) {
                this.searchRunning = false;
            }
        }
    }

    @Override
    public void requestHalt() {
        this.searchHalted = true;
        this.searchRunning = false;
    }

    @Override
    public long bestMove() {
        return this.bestMove;
    }

    @Override
    public String pv() {
        String pv = "";
        for(int i = 1; i <= this.rootPV[0]; i ++) {
            pv += Move.string((long) this.rootPV[i]) + " ";
        }
        return pv;
    }

    @Override
    public long nodes() {
        return this.nodesSearched;
    }

    private static final int INFINITY = 999999;
    private static final int MAX_PV_LENGTH = 20;

    private long[] board;
    private long[] rootMoveList;
    private int rootMoveListLength;
    private int maxDepth;
    private long maxSearchTime;
    private long bestMove;
    private long startTime;
    private volatile boolean searchRunning;
    private boolean searchHalted;
    private boolean timeReached;
    private long nodesSearched;
    private int[] rootPV;
    private int sendInfoDelay;
    private int currentSearchDepth;
    private long infoTimeElapsed;
    private int currentBestScore;
    private int bestScoreFoundAtDepth;
    private long currentDepthNodes;
    private long nextTimeToSendInfo;

    private void init() {
        this.rootMoveList = Gen.gen(this.board, true, false);
        this.rootMoveListLength = (int) this.rootMoveList[Gen.MOVELIST_SIZE];
        long[] boardAfterMove;
        long move;
        for(int moveIndex = 0; moveIndex < this.rootMoveListLength; moveIndex ++) {
            move = this.rootMoveList[moveIndex];
            boardAfterMove = Board.makeMove(this.board, move);
            int eval = -new Eval(boardAfterMove).eval();
            this.rootMoveList[moveIndex] = (move & 0xffffffffL) | ((long) eval << 32);
            System.out.println((int) (this.rootMoveList[moveIndex] >>> 32));
        }
        System.out.println(Move.moveListString(this.rootMoveList));
        Sort.sort(this.rootMoveList);
        System.out.println(Move.moveListString(this.rootMoveList));
    }

    private void doSearch() throws InterruptedException {
        this.searchHalted = false;
        this.timeReached = false;
        this.bestMove = 0L;
        this.startTime = System.currentTimeMillis();
        this.nextTimeToSendInfo = this.startTime + this.sendInfoDelay;
        this.currentBestScore = -INFINITY;
        this.bestScoreFoundAtDepth = 0;
        this.infoTimeElapsed = this.startTime;
        long[] boardAfterMove;
        long move;
        int eval;
        int bestEval;
        int[] tempPV = new int[MAX_PV_LENGTH];
        int moveEval;
        for(int depth = this.maxDepth < 2 ? this.maxDepth : 2; depth <= this.maxDepth; depth += (this.maxDepth - depth == 1) ? 1 : 2) {
            this.currentSearchDepth = depth;
            this.currentDepthNodes = this.nodesSearched;
            if(this.currentBestScore > (-INFINITY / 2)) sendInfo();
            bestEval = -INFINITY;
            Sort.sort(this.rootMoveList);
            for(int moveIndex = 0; moveIndex < this.rootMoveListLength; moveIndex ++) {
                move = this.rootMoveList[moveIndex];
                moveEval = (int) (move >> 32);
                boardAfterMove = Board.makeMove(this.board, move);
                tempPV[0] = 0;
                eval = -search(boardAfterMove, depth, -INFINITY, INFINITY, tempPV);
                int staticEval = new Eval(boardAfterMove).eval();
                if(Math.abs(staticEval - eval) > 100) {
                    System.out.println("Variant eval at depth " + depth + " move " + Move.string(move));
                    System.out.println("board: static eval " + new Eval(board).eval());
                    Board.drawText(board);
                    System.out.println("boardAfterMove: static eval " + staticEval);
                    Board.drawText(boardAfterMove);
                    System.out.println("search eval " + eval);
                }
                if(eval > moveEval) this.rootMoveList[moveIndex] = (move & 0xffffffffL) | ((long) eval << 32);
                if(eval > bestEval) {
                    this.bestMove = move;
                    bestEval = eval;
                    this.rootPV[0] = tempPV[0] + 1;
                    this.rootPV[1] = (int) move;
                    System.arraycopy(tempPV, 1, this.rootPV, 2, tempPV[0]);
                    this.currentBestScore = bestEval;
                    this.bestScoreFoundAtDepth = depth;
                    sendInfo();
                }
                if(this.searchHalted || this.timeReached) break;
            }
            if(this.searchHalted || this.timeReached) break;
        }
        sendInfo();
        this.searchRunning = false;
        //Game.INSTANCE.sendCommand("searchcomplete");
    }

    private int search(long[] board, int depth, int alpha, int beta, int[] pv) {
        if(this.searchHalted || this.timeReached) return alpha;
        if(this.nextTimeToSendInfo < System.currentTimeMillis()) {
            this.nextTimeToSendInfo = System.currentTimeMillis() + this.sendInfoDelay;
            sendInfo();
        }
        if(depth < 1) return quiesce(board, alpha, beta);
        this.nodesSearched ++;
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        long[] localMoveList = Gen.gen(board, false, false);
        int localMoveListLength = (int) localMoveList[Gen.MOVELIST_SIZE];
        long[] boardAfterMove;
        long move;
        int eval;
        int[] childPV = new int[MAX_PV_LENGTH];
        int bestEval = -INFINITY;
        for(int moveIndex = 0; moveIndex < localMoveListLength; moveIndex ++) {
            move = localMoveList[moveIndex];
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            childPV[0] = 0;
            eval = -search(boardAfterMove, depth - 1, -beta, -alpha,  childPV);
            if(eval > bestEval) {
                bestEval = eval;
                pv[0] = childPV[0] + 1;
                pv[1] = (int) move;
                System.arraycopy(childPV, 1, pv, 2, childPV[0]);
                if(eval >= beta) return beta;
                if(eval > alpha) {
                    alpha = eval;
                }
            }
        }
        if(System.currentTimeMillis() - this.startTime >= this.maxSearchTime) {
            this.timeReached = true;
        }
        return alpha;
    }

    private int quiesce(long[] board, int alpha, int beta) {
        int standPat = new Eval(board).eval();
        if(standPat >= beta) return beta;
        if(standPat + Piece.VALUE[Piece.QUEEN] < alpha) return alpha;
        if(standPat > alpha) alpha = standPat;
        long[] localMoveList = Gen.gen(board, false, true);
        int localMoveListLength = (int) localMoveList[Gen.MOVELIST_SIZE];
        long move;
        long[] boardAfterMove;
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        int other = 1 ^ player;
        int eval;
        boolean otherMaterialPiecesMoreThanOne = Board.countMaterialPieces(board, other) > 1;
        for(int moveIndex = 0; moveIndex < localMoveListLength; moveIndex ++) {
            move = localMoveList[moveIndex];
            if(otherMaterialPiecesMoreThanOne) {
                if(Piece.VALUE[(int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE] > Piece.VALUE[(int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE] && Eval.see(board, (int) move & Board.SQUARE_BITS, (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS) > 0) continue;
            }
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            eval = -quiesce(boardAfterMove, -beta, -alpha);
            if(eval >= beta) return beta;
            if(eval > alpha) alpha = eval;
        }
        return alpha;
    }

    private void sendInfo() {
        long currentTime = System.currentTimeMillis();
        System.out.println("info depth " + this.currentSearchDepth);
        System.out.println("info score cp " + this.currentBestScore + " depth " + this.bestScoreFoundAtDepth + " nodes " + (this.nodesSearched - this.currentDepthNodes) + " time " + (currentTime - this.infoTimeElapsed) + " pv " + pv());
        this.infoTimeElapsed = currentTime;
        int elapsedSeconds = (int) ((currentTime - this.startTime) / 1000);
        long nps = elapsedSeconds > 0 ? this.nodesSearched / elapsedSeconds : this.nodesSearched;
        System.out.println("info nps " + nps);
    }

}
