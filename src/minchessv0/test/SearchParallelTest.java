package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.Score;
import minchessv0.gen.Gen;
import minchessv0.move.Move;
import minchessv0.search.Search;
import minchessv0.sort.Sort;
import minchessv0.util.Piece;
import minchessv0.util.Value;

public class SearchParallelTest implements Search, Runnable {
    
    public SearchParallelTest(long[] board, int maxDepth, long maxSearchTime) {
        this.board = new long[Board.MAX_BITBOARDS];
        System.arraycopy(board, 0, this.board, 0, Board.MAX_BITBOARDS);
        this.playerToMove = (int) board[Board.STATUS] & Board.PLAYER_BIT;
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
    private int playerToMove;
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
    private Score currentBestScore;
    private int bestScoreFoundAtDepth;
    private long currentDepthNodes;
    private long nextTimeToSendInfo;

    private void init() {
        this.rootMoveList = Gen.gen(this.board, true, false);
        this.rootMoveListLength = (int) this.rootMoveList[Gen.MOVELIST_SIZE];
        long[] boardAfterMove;
        long move;
        Score score;
        int eval;
        for(int moveIndex = 0; moveIndex < this.rootMoveListLength; moveIndex ++) {
            move = this.rootMoveList[moveIndex];
            boardAfterMove = Board.makeMove(this.board, move);
            score = new Eval(boardAfterMove).score().negate();
            eval = score.eval();
            this.rootMoveList[moveIndex] = (move & 0xffffffffL) | ((long) eval << 32);
            System.out.println((int) (this.rootMoveList[moveIndex] >>> 32));
        }
        Sort.sort(this.rootMoveList);
    }

    private void doSearch() throws InterruptedException {
        this.searchHalted = false;
        this.timeReached = false;
        this.bestMove = 0L;
        this.startTime = System.currentTimeMillis();
        this.nextTimeToSendInfo = this.startTime + this.sendInfoDelay;
        this.currentBestScore = new Score(this.playerToMove, -INFINITY);
        this.bestScoreFoundAtDepth = 0;
        this.infoTimeElapsed = this.startTime;
        long[] boardAfterMove;
        long move;
        Score score;
        int eval;
        int bestEval;
        int[] tempPV = new int[MAX_PV_LENGTH];
        int moveEval;
        int player = (int) this.board[Board.STATUS] & Board.PLAYER_BIT;
        for(int depth = this.maxDepth < 2 ? this.maxDepth : 2; depth <= this.maxDepth; depth += (this.maxDepth - depth == 1) ? 1 : 2) {
            this.currentSearchDepth = depth;
            this.currentDepthNodes = this.nodesSearched;
            if(this.currentBestScore.eval() > (-INFINITY / 2)) sendInfo();
            bestEval = -INFINITY;
            Sort.sort(this.rootMoveList);
            for(int moveIndex = 0; moveIndex < this.rootMoveListLength; moveIndex ++) {
                move = this.rootMoveList[moveIndex];
                moveEval = (int) (move >> 32);
                boardAfterMove = Board.makeMove(this.board, move);
                tempPV[0] = 0;
                score = search(boardAfterMove, depth, new Score(1 ^ player, -INFINITY), new Score(1 ^ player, INFINITY), tempPV).negate();
                eval = score.eval();
                if(eval > moveEval) this.rootMoveList[moveIndex] = (move & 0xffffffffL) | ((long) eval << 32);
                if(eval > bestEval) {
                    this.bestMove = move;
                    bestEval = eval;
                    this.rootPV[0] = tempPV[0] + 1;
                    this.rootPV[1] = (int) move;
                    System.arraycopy(tempPV, 1, this.rootPV, 2, tempPV[0]);
                    this.currentBestScore = score;
                    this.bestScoreFoundAtDepth = depth;
                    System.out.println(score.string());
                    sendInfo();
                }
                if(this.searchHalted || this.timeReached) break;
            }
            if(this.searchHalted || this.timeReached) break;
        }
        sendInfo();
        this.searchRunning = false;
    }

    private Score search(long[] board, int depth, Score alpha, Score beta, int[] pv) {
        System.out.println("search depth " + depth + " alpha [" + alpha.stringInfo() + "] beta [" + beta.stringInfo() + "]");
        try {Thread.sleep(1000);} catch(InterruptedException e) { e.printStackTrace(); }
        if(this.searchHalted || this.timeReached) return alpha;
        if(this.nextTimeToSendInfo < System.currentTimeMillis()) {
            this.nextTimeToSendInfo = System.currentTimeMillis() + this.sendInfoDelay;
            sendInfo();
        }
        if(depth < 1) {
            System.out.println(" redirecting to quiesce\nalpha " + alpha.eval() + " beta " + beta.eval());
            return quiesce(board, alpha, beta);
        }
        this.nodesSearched ++;
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        long[] localMoveList = Gen.gen(board, false, false);
        int localMoveListLength = (int) localMoveList[Gen.MOVELIST_SIZE];
        long[] boardAfterMove;
        long move;
        int eval;
        int[] childPV = new int[MAX_PV_LENGTH];
        int bestEval = -INFINITY;
        Score score;
        for(int moveIndex = 0; moveIndex < localMoveListLength; moveIndex ++) {
            move = localMoveList[moveIndex];
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            childPV[0] = 0;
            System.out.println("move " + moveIndex + " " + Move.string(move));
            score = new Score(search(boardAfterMove, depth - 1, new Score(beta.negate()), new Score(alpha.negate()),  childPV).negate());
            System.out.println("after search alpha " + alpha.stringInfo() + " beta " + beta.stringInfo() + " score " + score.stringInfo());
            eval = score.eval();
            if(eval > bestEval) {
                bestEval = eval;
                pv[0] = childPV[0] + 1;
                pv[1] = (int) move;
                System.arraycopy(childPV, 1, pv, 2, childPV[0]);
                if(eval >= beta.eval()) {
                    System.out.println("search returning beta\n" + beta.stringInfo());
                    return beta;
                }
                if(eval > alpha.eval()) {
                    alpha = new Score(score);
                }
            }
        }
        if(System.currentTimeMillis() - this.startTime >= this.maxSearchTime) {
            this.timeReached = true;
        }
        System.out.println("search returning alpha\n" + alpha.stringInfo());
        return alpha;
    }

    private Score quiesce(long[] board, Score alpha, Score beta) {
        Score standPat = new Eval(board).score();
        System.out.println("standpat\n" + standPat.stringInfo() + " beta " + beta.eval() + " alpha " + alpha.eval());
        int standPatEval = standPat.eval();
        if(standPatEval >= beta.eval()) return beta;
        if(standPatEval + Piece.VALUE[Piece.QUEEN] < alpha.eval()) return alpha;
        if(standPatEval > alpha.eval()) {
            alpha = new Score(standPat);
            System.out.println("copying standPat to alpha");
            System.out.println("standPat " + standPat.stringInfo());
            System.out.println("alpha " + alpha.stringInfo());
        }
        long[] localMoveList = Gen.gen(board, false, true);
        int localMoveListLength = (int) localMoveList[Gen.MOVELIST_SIZE];
        long move;
        long[] boardAfterMove;
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        int other = 1 ^ player;
        int eval;
        Score score;
        boolean otherMaterialPiecesMoreThanOne = Board.countMaterialPieces(board, other) > 1;
        for(int moveIndex = 0; moveIndex < localMoveListLength; moveIndex ++) {
            move = localMoveList[moveIndex];
            if(otherMaterialPiecesMoreThanOne) {
                if(Piece.VALUE[(int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE] > Piece.VALUE[(int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE] && Eval.see(board, (int) move & Board.SQUARE_BITS, (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS) > 0) continue;
            }
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            score = new Score(quiesce(boardAfterMove, new Score(beta.negate()), new Score(alpha.negate())).negate());
            eval = score.eval();
            if(eval >= beta.eval()) {
                System.out.println("quiesce returning beta\n" + beta.stringInfo());
                return beta;
            }
            if(eval > alpha.eval()) alpha = new Score(score);
        }
        System.out.println("quiesce returning alpha\n" + alpha.stringInfo());
        return alpha;
    }

    private void sendInfo() {
        long currentTime = System.currentTimeMillis();
        //System.out.println("info depth " + this.currentSearchDepth);
        //System.out.println("info score cp " + this.currentBestScore.eval() + " depth " + this.bestScoreFoundAtDepth + " nodes " + (this.nodesSearched - this.currentDepthNodes) + " time " + (currentTime - this.infoTimeElapsed) + " pv " + pv());
        this.infoTimeElapsed = currentTime;
        int elapsedSeconds = (int) ((currentTime - this.startTime) / 1000);
        long nps = elapsedSeconds > 0 ? this.nodesSearched / elapsedSeconds : this.nodesSearched;
        //System.out.println("info nps " + nps);
    }

}
