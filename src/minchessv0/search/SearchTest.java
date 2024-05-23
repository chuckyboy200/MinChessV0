package minchessv0.search;

import minchessv0.board.Board;
import minchessv0.eval.Eval2;
import minchessv0.game.Game;
import minchessv0.gen.Gen;
import minchessv0.move.Move;
import minchessv0.sort.Sort;
import minchessv0.util.Piece;

public class SearchTest implements Search, Runnable {

    public SearchTest(long[] board, int maxDepth, long maxSearchTime) {
        this.board = new long[Board.MAX_BITBOARDS];
        System.arraycopy(board, 0, this.board, 0, board.length);
        this.maxDepth = maxDepth;
        this.maxSearchTime = maxSearchTime;
        this.bestMove = 0L;
        this.rootPV = new int[MAX_PV_LENGTH];
        this.sendInfoDelay = 100000;
    }

    @Override
    public void run() {
        init();
        doSearch();
    }

    @Override
    public void requestHalt() {
        this.timeReached = true;
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
        return this.nodes;
    }

    private static final int INFINITY = 999999;
    private static final int MAX_PV_LENGTH = 20;

    private long[] board;
    private long[] rootMoveList;
    private int maxDepth;
    private long maxSearchTime;
    private long bestMove;
    private long startTime;
    private boolean timeReached;
    private long nodes;
    private long currentDepthNodes;
    private int[] rootPV;
    private int sendInfoDelay;
    private long nextTimeToSendInfo;
    private int currentSearchDepth;
    private int currentBestScore;
    private int bestScoreFoundAtDepth;
    private long infoTimeElapsed;
    
    private void init() {
        this.rootMoveList = Gen.gen(this.board, true, false);
    }

    private void doSearch() {
        this.startTime = System.currentTimeMillis();
        this.timeReached = false;
        this.bestMove = 0;
        this.currentBestScore = -INFINITY;
        this.bestScoreFoundAtDepth = 0;
        this.infoTimeElapsed = System.currentTimeMillis();
        long[] boardAfterMove;
        int eval;
        int moveEval;
        long move;
        for(int moveIndex = 0; moveIndex < this.rootMoveList[Gen.MOVELIST_SIZE]; moveIndex ++) {
            move = this.rootMoveList[moveIndex];
            boardAfterMove = Board.makeMove(this.board, move);
            eval = -new Eval2(boardAfterMove).eval();
            this.rootMoveList[moveIndex] = ((long) eval << 32) | (move & 0xffffffffL);
        }
        int[] tempPV = new int[MAX_PV_LENGTH];
        for(int depth = 2; depth <= maxDepth; depth ++) {
            this.currentDepthNodes = this.nodes;
            this.currentSearchDepth = depth;
            sendInfo();
            int bestEval = -INFINITY;
            //System.out.println("Searching to depth " + depth);
            Sort.sort(this.rootMoveList);
            for(int moveIndex = 0; moveIndex < this.rootMoveList[Gen.MOVELIST_SIZE]; moveIndex ++) {
                move = this.rootMoveList[moveIndex];
                moveEval = (int) (move >> 32);
                boardAfterMove = Board.makeMove(this.board, move);
                //System.out.print("made move " + Move.string(move) + " current eval " + moveEval);
                tempPV[0] = 0;
                this.nodes ++;
                eval = -search(boardAfterMove, depth, -INFINITY, INFINITY, tempPV);
                //System.out.println("eval from search = " + eval);
                if(eval > moveEval) {
                    this.rootMoveList[moveIndex] = ((long) eval << 32) | (move & 0xffffffffL);
                    //System.out.println(" adjust +" + (eval - moveEval));
                } else {
                    //System.out.println();
                }
                if(eval > bestEval) {
                    this.bestMove = move;
                    bestEval = eval;
                    rootPV[0] = tempPV[0] + 1;
                    rootPV[1] = (int) move;
                    //System.out.println("rootPV length " + rootPV[0]);
                    System.arraycopy(tempPV, 1, rootPV, 2, tempPV[0]);
                    //System.out.println("best = " + Move.string(move));
                    this.currentBestScore = bestEval;
                    this.bestScoreFoundAtDepth = depth;
                    sendInfo();
                }
                if(timeReached) break;
            }
            if(timeReached) break;
        }
        sendInfo();
        Game.INSTANCE.sendCommand("searchcomplete");
    }

    private int search(long[] board, int depth, int alpha, int beta, int[] pv) {
        if(this.timeReached) return alpha;
        if(this.nextTimeToSendInfo < System.currentTimeMillis()) {
            this.nextTimeToSendInfo = System.currentTimeMillis() + sendInfoDelay;
            sendInfo();
        }
        if(depth < 1) return quiesce(board, alpha, beta);
        long[] moveList = Gen.gen(board, false, false);
        long[] boardAfterMove;
        long move;
        int player = Board.player(board);
        int eval;
        int[] childPV = new int[MAX_PV_LENGTH];
        int maxEval = -INFINITY;
        for(int moveIndex = 0; moveIndex < moveList[Gen.MOVELIST_SIZE]; moveIndex ++) {
            move = moveList[moveIndex];
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            childPV[0] = 0;
            this.nodes ++;
            eval = -search(boardAfterMove, depth - 1, -beta, -alpha, childPV);
            if(eval > maxEval) {
                //System.out.println("PV change " + move);
                maxEval = eval;
                pv[0] = childPV[0] + 1;
                pv[1] = (int) move;
                System.arraycopy(childPV, 1, pv, 2, childPV[0]);
                if(eval > alpha) {
                    alpha = eval;
                    if(alpha >= beta) {
                        break;
                    }
                }
            }
        }
        if(System.currentTimeMillis() - startTime >= this.maxSearchTime) {
            this.timeReached = true;
        }
        return alpha;
    }

    private int quiesce(long[] board, int alpha, int beta) {
        int standPat = new Eval2(board).eval();
		if (standPat >= beta) {
			return beta;
		}
		if (standPat + Piece.VALUE[Piece.QUEEN] < alpha) {
			return alpha;
		}
		if (standPat > alpha) {
			alpha = standPat;
		}
		long[] moveList = Gen.gen(board, false, true);
        long move;
        long[] boardAfterMove;
        int player = Board.player(board);
        int other = 1 ^ player;
        int eval;
		for (int i = 0; i < moveList[Gen.MOVELIST_SIZE]; i ++) {
			move = moveList[i];
			if (Piece.VALUE[(int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE] > Piece.VALUE[(int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE] && Board.countMaterialPieces(board, other) > 1 && Eval2.see(board, (int) move & Board.SQUARE_BITS, (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS) < 0) continue;
			boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
			eval = -quiesce(boardAfterMove, -beta, -alpha);
			if (eval >= beta) return beta;
			if (eval > alpha) alpha = eval;
		}
		return alpha;
    }

    private void sendInfo() {
        long currentTime = System.currentTimeMillis();
        System.out.println("info depth " + currentSearchDepth);
        System.out.println("info score cp " + this.currentBestScore + " depth " + bestScoreFoundAtDepth + " nodes " + (this.nodes - this.currentDepthNodes) + " time " + (currentTime - this.infoTimeElapsed) + " pv " + pv());
        this.infoTimeElapsed = currentTime;
        int elapsedSeconds = (int) ((currentTime - this.startTime) / 1000);
        long nps = elapsedSeconds > 0 ? this.nodes / elapsedSeconds : this.nodes;
        System.out.println("info nps " + nps);
    }
    

}
