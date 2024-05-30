package minchessv0.eval;

import minchessv0.board.Board;

public class Score {
    
    public Score(Score score) {
        this();
        System.arraycopy(score.boardAtTimeOfEval, 0, this.boardAtTimeOfEval, 0, Board.MAX_BITBOARDS);
        System.arraycopy(score.value[0], 0, this.value[0], 0, score.value[0].length);
        System.arraycopy(score.value[1], 0, this.value[1], 0, score.value[1].length);
        this.eval = score.eval;
        this.playerToMove = score.playerToMove;
        this.isNegated = score.isNegated;
        this.isEvaluated = score.isEvaluated;
    }

    public Score(int player, int defaultValue) {
        this();
        this.playerToMove = player;
        this.eval = defaultValue;
    }

    public Score negate() {
        this.isNegated = !this.isNegated;
        return this;
    }

    public void addCriteria(long[] board, int player, int index, int value) {
        if(!this.isEvaluated) {
            this.isEvaluated = true;
            this.eval = 0;
            System.arraycopy(board, 0, this.boardAtTimeOfEval, 0, board.length);
        }
        this.value[player][index] += value;
        this.eval += player == this.playerToMove ? value : -value;
    }

    public Score score() {
        return this;
    }

    public int eval() {
        return this.isNegated ? -this.eval : this.eval;
    }

    public String string(int player) {
        String string = "";
        for(int i = 0; i < Criteria.MAX_CRITERIA; i ++) {
            if(this.value[player][i] != 0) string += Criteria.CRITERIA_NAME[i] + " " + this.value[player][i] + "\n";
        }
        return string;
    }

    public boolean verify() {
        return verifyValue() == this.eval;
    }

    public int verifyValue() {
        boolean isMovingPlayer;
        int verifyValue = 0;
        int verifyEval = 0;
        for(int player = 0; player < 2; player ++) {
            isMovingPlayer = player == this.playerToMove;
            verifyEval = 0;
            for(int i = 0; i < Criteria.MAX_CRITERIA; i ++) {
                verifyEval += this.value[player][i];
            }
            verifyValue += isMovingPlayer ? verifyEval : -verifyEval;
        }
        return this.isNegated ? -verifyValue : verifyValue;
    }

    public String string() {
        return this.string(0) + this.string(1);
    }

    public String stringAll() {
        return string() + stringInfo();
    }

    public String stringInfo() {
        return "eval " + this.eval + (this.isNegated ? " negated" : "");
    }

    public String stringCompare(Score otherScore) {
        String string = "";
        int sum;
        boolean samePlayer = this.playerToMove == otherScore.playerToMove;
        for(int i = 0; i < Criteria.MAX_CRITERIA; i ++) {
            sum = samePlayer ? this.value[0][i] - this.value[1][i] : this.value[0][i] + this.value[1][i];
            if(sum != 0) string += Criteria.CRITERIA_NAME[i] + " " + (otherScore.playerToMove == this.playerToMove ? sum : -sum) + "\n";
        }
        return string;
    }

    public String stringCriteriaTotal() {
        String string = "";
        int sum;
        int other = 1 ^ this.playerToMove;
        for(int i = 0; i < Criteria.MAX_CRITERIA; i ++) {
            sum = (this.value[this.playerToMove][i] - this.value[other][i]) * (this.isNegated ? -1 : 1);
            if(sum != 0) string += Criteria.CRITERIA_NAME[i] + " " + sum + "\n";
        }
        return string;
    }

    public long[] board() {
        return this.boardAtTimeOfEval;
    }

    private long[] boardAtTimeOfEval;
    private int[][] value;
    private int eval;
    private int playerToMove;
    private boolean isNegated;
    private boolean isEvaluated;

    private Score() {
        this.boardAtTimeOfEval = new long[Board.MAX_BITBOARDS];
        this.value = new int[2][Criteria.MAX_CRITERIA];
    }


}
