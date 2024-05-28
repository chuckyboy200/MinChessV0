package minchessv0.eval;

public class Score {
    
    public Score() {
        this.value = new int[2][Criteria.MAX_CRITERIA];
        this.eval = new int[2];
        this.negated = false;
        this.hasEval = false;
    }

    public Score(Score score) {
        this();
        System.arraycopy(score.value[0], 0, this.value[0], 0, score.value[0].length);
        System.arraycopy(score.value[1], 0, this.value[1], 0, score.value[1].length);
        this.eval[0] = score.eval[0];
        this.eval[1] = score.eval[1];
        this.totalEval = score.totalEval;
        this.alphaBeta = score.alphaBeta;
        this.negated = score.negated;
        this.playerToMove = score.playerToMove;
        this.hasEval = score.hasEval;
    }

    public Score(int defaultValue) {
        this();
        this.alphaBeta = defaultValue;
    }

    public void addCriteria(int player, int criteriaIndex, int value) {
        this.value[player][criteriaIndex] += value;
        this.eval[player] += value;
        this.totalEval += player == this.playerToMove ? value : -value;
        this.hasEval = true;
    }

    public int eval() {
        return this.hasEval ? (this.negated ? this.eval[1 ^ this.playerToMove] - this.eval[this.playerToMove] : this.eval[this.playerToMove] - this.eval[1 ^ this.playerToMove]) : 0;
    }

    public int eval(int player) {
        return this.hasEval ? (this.negated ? -this.eval[player] : this.eval[player]) : 0;
    }

    public String string(int player) {
        String string = "";
        for(int i = 0; i < Criteria.MAX_CRITERIA; i ++) {
            if(this.value[player][i] > 0) string += Criteria.CRITERIA_NAME[i] + " " + this.value[player][i] + "\n";
        }
        return string;
    }

    public String string() {
        return string(0) + string(1);
    }

    public String stringAll() {
        return string() + stringInfo();
    }

    public String stringInfo() {
        return "alphaBeta " + this.alphaBeta + " negated " + this.negated + " eval " + eval();
    }

    public Score negate() {
        this.negated = !this.negated;
        return this;
    }

    public int alphaBeta() {
        return this.negated ? -this.alphaBeta : this.alphaBeta;
    }

    public int player() {
        return this.playerToMove;
    }

    public void setPlayer(int player) {
        this.playerToMove = player;
    }

    private int[][] value;
    private int[] eval;
    private int totalEval;
    private int alphaBeta;
    private boolean negated;
    private int playerToMove;
    private boolean hasEval;

}
