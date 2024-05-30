package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.Score;
import minchessv0.gen.Gen;
import minchessv0.move.Move;
import minchessv0.sort.Sort;
import minchessv0.util.Piece;

public class SearchParallelTest2 {
    
    public SearchParallelTest2(long[] board, int maxDepth, int maxSearchTime, boolean verbose) {
        this.board = new long[board.length];
        System.arraycopy(board, 0, this.board, 0, board.length);
        this.playerToMove = (int) this.board[Board.STATUS] & Board.PLAYER_BIT;
        this.maxDepth = maxDepth;
        this.verbose = verbose;
    }

    public void run() {
        init();
        doSearch();
    }

    public long bestMove() {
        return this.bestMove;
    }

    public Score bestMoveScore() {
        return this.bestMoveScore;
    }

    private static final int INFINITY = 999999;

    private long[] board;
    private int playerToMove;
    private long bestMove;
    private Score bestMoveScore;
    private long[] rootMoveList;
    private int rootMoveListLength;
    private Score currentBestScore;
    private int bestScoreFoundAtDepth;
    private int maxDepth;
    private boolean verbose;

    private void init() {
        this.rootMoveList = Gen.gen(this.board, true, false);
        this.rootMoveListLength = (int) this.rootMoveList[Gen.MOVELIST_SIZE];
        long move;
        long[] boardAfterMove;
        Score score;
        int eval;
        for(int moveIndex = 0; moveIndex < this.rootMoveListLength; moveIndex ++) {
            move = this.rootMoveList[moveIndex];
            boardAfterMove = Board.makeMove(this.board, move);
            score = new Eval(boardAfterMove).score().negate();
            eval = score.eval();
            this.rootMoveList[moveIndex] = (move & 0xffffffffL) | ((long) eval << 32);
        }
        Sort.sort(this.rootMoveList);
    }

    private void doSearch() {
        this.bestMove = 0L;
        this.currentBestScore = new Score(this.playerToMove, -INFINITY);
        this.bestScoreFoundAtDepth = 0;
        long[] boardAfterMove;
        long move;
        Score score;
        int eval;
        int bestEval;
        int moveEval;
        int staticEval = new Eval(this.board).eval();
        System.out.println("----------------------------------------\nEvaluation: " + staticEval * (this.playerToMove == 0 ? 1 : -1));
        System.out.print("----------------------------------------\nDepth searched: 1/");
        for(int depth = this.maxDepth < 2 ? this.maxDepth : 2; depth <= this.maxDepth; depth += (this.maxDepth - depth == 1) ? 1 : 2) {
            System.out.print(depth + ((depth == this.maxDepth) ? "(Max)\n" : "/"));
            bestEval = -INFINITY;
            Sort.sort(this.rootMoveList);
            if(this.verbose) System.out.println(Move.moveListString(this.rootMoveList));
            for(int moveIndex = 0; moveIndex < this.rootMoveListLength; moveIndex ++) {
                move = this.rootMoveList[moveIndex];
                moveEval = (int) (move >> 32);
                if (depth == this.maxDepth) {
					System.out.print("  " + (moveIndex + 1) + "/" + this.rootMoveListLength + " " + ((moveIndex > 8) ? "" : " ") + Move.string(move) + ": baseEval " + moveEval + " ");
				}
                boardAfterMove = Board.makeMove(this.board, move);
                score = search(boardAfterMove, depth, new Score(this.playerToMove, -INFINITY), new Score(this.playerToMove, INFINITY)).negate();
                eval = score.eval();
                if(eval > moveEval) this.rootMoveList[moveIndex] = (move & 0xffffffffL) | ((long) eval << 32);
                if(depth == this.maxDepth) {
					String evalString = " ";
					if(Math.abs(eval) > INFINITY - 100) {
						int mateIn = (INFINITY - Math.abs(eval) + 1) / 2;
						if(mateIn == 1) {
							if(eval < 0) {
								evalString += this.playerToMove == 0 ? "0-1" : "1-0";
							} else {
								evalString += this.playerToMove == 0 ? "1-0" : "0-1";
							}
						} else {
							if(eval < 0) {
								evalString += "-";
							} else {
								evalString += " ";
							}
							evalString += "M" + Integer.toString(mateIn);
						}
					} else {
						if(eval >= 0) {
							evalString += " ";
						}
						evalString += Integer.toString(eval);
						if(eval > staticEval) {
							evalString += "+ ";
						} else {
							evalString += "  ";
						}
					}
					System.out.print("new eval " + evalString);
				}
                if(eval > bestEval) {
                    if(depth == this.maxDepth) {
						System.out.println("(Best)");
                        /*
						b.draw();
						b.drawBest(m);
                        */
					}
                    this.bestMove = move;
                    bestEval = eval;
                    this.currentBestScore = new Score(score);
                    this.bestScoreFoundAtDepth = depth;
                    if(this.verbose) System.out.println(score.string());
                } else {
                    if(depth == this.maxDepth) System.out.println();
                }
                if(depth == this.maxDepth && !verbose) {
                    Board.drawText(score.board());
                    System.out.println(score.stringCriteriaTotal() + score.stringInfo());
                }
            }
        }
    }

    private Score search(long[] board, int depth, Score alpha, Score beta) {
        if(this.verbose) System.out.println("search depth " + depth + stringInfo("alpha", alpha) + stringInfo("beta", beta));
        if(this.verbose) try {Thread.sleep(1000);} catch(InterruptedException e) { e.printStackTrace(); }
        if(depth < 1) {
            if(this.verbose) System.out.println("redirecting to quiesce");
            return quiesce(board, alpha, beta);
        }
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        long[] moveList = Gen.gen(board, false, false);
        int moveListLength = (int) moveList[Gen.MOVELIST_SIZE];
        long[] boardAfterMove;
        long move;
        int eval;
        int bestEval = -INFINITY;
        Score score;
        Sort.sortNoEval(board, moveList);
        for(int moveIndex = 0; moveIndex < moveListLength; moveIndex ++) {
            move = moveList[moveIndex];
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            score = search(boardAfterMove, depth - 1, new Score(beta).negate(), new Score(alpha).negate()).negate();
            if(this.verbose) System.out.println("after search move " + Move.string(move) + ":" + stringInfo("score", score));
            eval = score.eval();
            if(eval > bestEval) {
                bestEval = eval;
                if(eval >= beta.eval()) {
                    if(this.verbose) System.out.println("search returning " + stringInfo("beta", beta));
                    return new Score(beta);
                }
                if(eval >= alpha.eval()) alpha = new Score(score);
            }
        }
        if(this.verbose) System.out.println("search returning " + stringInfo("alpha", alpha));
        return new Score(alpha);
    }

    private Score quiesce(long[] board, Score alpha, Score beta) {
        Score standPat = new Eval(board).score();
        if(this.verbose) System.out.println("   " + stringInfo("standPat", standPat));
        int standPatEval = standPat.eval();
        if(standPatEval >= beta.eval()) {
            if(this.verbose) System.out.println("    quiesce returning 1" + stringInfo("beta", beta));
            return new Score(beta);
        }
        if(standPatEval + Piece.VALUE[Piece.QUEEN] < alpha.eval()) return new Score(alpha);
        if(standPatEval > alpha.eval()) {
            if(this.verbose) System.out.println("    old" + stringInfo("alpha", alpha));
            alpha = new Score(standPat);
            if(this.verbose) System.out.println("    new" + stringInfo("alpha", alpha));
        }
        long[] moveList = Gen.gen(board, false, true);
        int moveListLength = (int) moveList[Gen.MOVELIST_SIZE];
        long move;
        long[] boardAfterMove;
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        int other = 1 ^ player;
        int eval;
        Score score;
        boolean otherMaterialPiecesMoreThanOne = Board.countMaterialPieces(board, other) > 1;
        for(int moveIndex = 0; moveIndex < moveListLength; moveIndex ++) {
            move = moveList[moveIndex];
            if(otherMaterialPiecesMoreThanOne) {
                if(Piece.VALUE[(int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE] > Piece.VALUE[(int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE]
                && Eval.see(board, (int) move & Board.SQUARE_BITS, (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS) > 0) continue;
            }
            boardAfterMove = Board.makeMove(board, move);
            if(Board.isPlayerInCheck(boardAfterMove, player)) continue;
            score = quiesce(boardAfterMove, new Score(beta).negate(), new Score(alpha).negate()).negate();
            eval = score.eval();
            if(eval >= beta.eval()) {
                if(this.verbose) System.out.println("    quiesce returning 2" + stringInfo("beta", beta));
                return new Score(beta);
            }
            if(eval > alpha.eval()) alpha = new Score(score);
        }
        if(this.verbose) System.out.println("    quiesce returning" + stringInfo("alpha", alpha));
        return new Score(alpha);
    }

    private String stringInfo(String scoreName, Score score) {
        return " " + scoreName + " [" + score.stringInfo() + "]";
    }

}
