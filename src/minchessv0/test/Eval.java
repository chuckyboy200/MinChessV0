package minchessv0.test;

import minchessv0.board.Board;
import minchessv0.eval.Criteria;
import minchessv0.eval.Psqt;
import minchessv0.eval.Score;
import minchessv0.util.B;
import minchessv0.util.Magic;
import minchessv0.util.Piece;
import minchessv0.util.Value;

public class Eval {

    public Eval() {
        this(Board.startingPosition());
    }

    public Eval(long[] board) {
        this.board = board;
        this.playerToMove = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        this.playerOccupancy[0] = board[0];
        this.playerOccupancy[1] = board[8];
        this.allOccupancy = this.playerOccupancy[0] | this.playerOccupancy[1];
        this.playerKingSquare[0] = Long.numberOfTrailingZeros(board[Piece.WHITE_KING]);
        this.playerKingRank[0] = this.playerKingSquare[0] >>> 3;
        this.playerKingFile[0] = this.playerKingSquare[0] & 7;
        this.playerKingSquare[1] = Long.numberOfTrailingZeros(board[Piece.BLACK_KING]);
        this.playerKingRank[1] = this.playerKingSquare[1] >>> 3;
        this.playerKingFile[1] = this.playerKingSquare[1] & 7;
        this.phase = Math.min((Long.bitCount(board[Piece.WHITE_QUEEN]) + Long.bitCount(board[Piece.BLACK_QUEEN])) * 4 +
                     (Long.bitCount(board[Piece.WHITE_ROOK])  + Long.bitCount(board[Piece.BLACK_ROOK]))  * 2 +
                     Long.bitCount(board[Piece.WHITE_BISHOP]) + Long.bitCount(board[Piece.BLACK_BISHOP]) +
                     Long.bitCount(board[Piece.WHITE_KNIGHT]) + Long.bitCount(board[Piece.BLACK_KNIGHT]), 24);
        this.hasEvaluated = false;
    }

    public int eval() {
        if(!this.hasEvaluated) evaluateScore();
        return this.score.eval(this.playerToMove) - this.score.eval(1 ^ this.playerToMove);
    }

    public Score score() {
        if(!this.hasEvaluated) evaluateScore();
        return this.score;
    }

    public void evaluateScore() {
        this.score = new Score();
        kingEval(Value.WHITE, Value.BLACK, board[Piece.WHITE_ROOK], board[Piece.WHITE_PAWN], board[Piece.BLACK_PAWN]);
        queenEval(Value.WHITE, Value.BLACK, board[Piece.WHITE_QUEEN], board[Piece.WHITE_BISHOP], board[Piece.WHITE_KNIGHT]);
        rookEval(Value.WHITE, Value.BLACK, board[Piece.WHITE_ROOK], board[Piece.WHITE_KING], board[Piece.WHITE_PAWN], board[Piece.BLACK_PAWN], board[Piece.BLACK_QUEEN]);
        bishopEval(Value.WHITE, Value.BLACK, board[Piece.WHITE_BISHOP], board[Piece.WHITE_PAWN], board[Piece.BLACK_PAWN]);
        knightEval(Value.WHITE, Value.BLACK, board[Piece.WHITE_KNIGHT], board[Piece.WHITE_PAWN], board[Piece.BLACK_PAWN]);
        pawnEval(Value.WHITE, Value.BLACK, board[Piece.WHITE_PAWN], board[Piece.BLACK_PAWN], board[Piece.WHITE_KING]);
        kingEval(Value.BLACK, Value.WHITE, board[Piece.BLACK_ROOK], board[Piece.BLACK_PAWN], board[Piece.WHITE_PAWN]);
        queenEval(Value.BLACK, Value.WHITE, board[Piece.BLACK_QUEEN], board[Piece.BLACK_BISHOP], board[Piece.BLACK_KNIGHT]);
        rookEval(Value.BLACK, Value.WHITE, board[Piece.BLACK_ROOK], board[Piece.BLACK_KING], board[Piece.BLACK_PAWN], board[Piece.WHITE_PAWN], board[Piece.WHITE_QUEEN]);
        bishopEval(Value.BLACK, Value.WHITE, board[Piece.BLACK_BISHOP], board[Piece.BLACK_PAWN], board[Piece.WHITE_PAWN]);
        knightEval(Value.BLACK, Value.WHITE, board[Piece.BLACK_KNIGHT], board[Piece.BLACK_PAWN], board[Piece.WHITE_PAWN]);
        pawnEval(Value.BLACK, Value.WHITE, board[Piece.BLACK_PAWN], board[Piece.WHITE_PAWN], board[Piece.BLACK_KING]);
        this.hasEvaluated = true;
    }

    public static int see(long[] board, int startSquare, int targetSquare) {
        long[] seeBoard = new long[board.length];
        System.arraycopy(board, 0, seeBoard, 0, board.length);
        int seeValue = 0;
        int startPlayer = (int) seeBoard[Board.STATUS] & Board.PLAYER_BIT;
        int currentPlayer = startPlayer;
        int targetPiece = Board.getSquare(seeBoard, targetSquare);
        long pieceMoveBits;
        int startPiece = Board.getSquare(seeBoard, startSquare);
        long targetSquareBit = 1L << targetSquare;
        while(true) {
            seeValue += Piece.VALUE[targetPiece & Piece.TYPE] * (currentPlayer == startPlayer ? 1 : -1);
            currentPlayer = 1 ^ currentPlayer;
            pieceMoveBits = (1L << startSquare) | targetSquareBit;
            seeBoard[startPiece] ^= pieceMoveBits;
            seeBoard[startPiece & 8] ^= pieceMoveBits;
            seeBoard[targetPiece] ^= targetSquareBit;
            seeBoard[targetPiece & 8] ^= targetSquareBit;
            //Gui.drawBoard(seeBoard);
            //Gui.println("seeValue = " + seeValue);
            //Gui.sleep(2000);
            startSquare = getNextAttackingPiece(seeBoard, targetSquare, currentPlayer);
            if(startSquare == Value.INVALID) break;
            targetPiece = startPiece;
            startPiece = Board.getSquare(seeBoard, startSquare);
        }
        return seeValue;
    }

    private static final int[][] PIECE_VALUE = new int[7][11];
    private static final int[][] MOBILITY_VALUE = new int[7][29];
    private static final int[][] KING_SAFETY_VALUE = new int[7][7];
    private static final int[][] KING_DISTANCE_VALUE = new int[7][15];
    private static final int[] PAWN_SHIELD_CLOSE_VALUE = new int[4];
    private static final int[] PAWN_SHIELD_FAR_VALUE = new int[4];
    private static final int[] PAWN_STORM_CLOSE_VALUE = new int[4];
    private static final int[] PAWN_STORM_FAR_VALUE = new int[4];
    private static final int[][] ROOK_PAWN_VALUE = new int[11][9];
    private static final int[][] BISHOP_PAWN_VALUE = new int[9][9];
    private static final int[][] KNIGHT_PAWN_VALUE = new int[11][9];

    static {
        for(int type = Piece.QUEEN; type <= Piece.PAWN; type ++) {
            for(int num = 1; num < 11; num ++) {
                PIECE_VALUE[type][num] = Piece.VALUE[type] * num;
            }
            for(int mobility = 1; mobility < 29; mobility ++) {
                MOBILITY_VALUE[type][mobility] = mobility * (type == 2 ? 3 : type == 3 ? 2 : 1);
            }
            for(int safety = 1; safety < 7; safety ++) {
                KING_SAFETY_VALUE[type][safety] = safety * (type == 2 ? 5 : type == 2 ? 3 : 2);
            }
            for(int distance = 0; distance < 15; distance ++) {
                KING_DISTANCE_VALUE[type][distance] = (distance - 7) * (type == 2 ? 3 : type == 3 ? 2 : 1);
            }
        }
        for(int num = 1; num < 4; num ++) {
            PAWN_SHIELD_CLOSE_VALUE[num] = num * Criteria.VALUE[Criteria.PAWN_SHIELD_CLOSE];
            PAWN_SHIELD_FAR_VALUE[num] = num * Criteria.VALUE[Criteria.PAWN_SHIELD_FAR];
            PAWN_STORM_CLOSE_VALUE[num] = num * Criteria.VALUE[Criteria.PAWN_STORM_CLOSE];
            PAWN_STORM_FAR_VALUE[num] = num * Criteria.VALUE[Criteria.PAWN_STORM_FAR];
        }
        for(int rooks = 1; rooks < 11; rooks ++) {
            for(int pawns = 0; pawns < 9; pawns ++) {
                ROOK_PAWN_VALUE[rooks][pawns] = (5 - pawns) * rooks * 3;
            }
        }
        for(int ownPawns = 0; ownPawns < 9; ownPawns ++) {
            for(int otherPawns = 0; otherPawns < 9; otherPawns ++) {
                BISHOP_PAWN_VALUE[ownPawns][otherPawns] = ownPawns * 3 - otherPawns * 7;
            }
        }
        for(int knights = 1; knights < 11; knights ++) {
            for(int pawns = 0; pawns < 9; pawns ++) {
                KNIGHT_PAWN_VALUE[knights][pawns] = (pawns - 5) * knights * 4;
            }
        }
    }

    private static final int ROOK_PROTECTS = Criteria.VALUE[Criteria.ROOK_PROTECTS];
    private static final int KING_BLOCKS_ROOK = Criteria.VALUE[Criteria.KING_BLOCKS_ROOK];
    private static final int QUEEN_EARLY_DEVELOPMENT = Criteria.VALUE[Criteria.QUEEN_EARLY_DEVELOPMENT];
    private static final int ROOK_EARLY_DEVELOPMENT = Criteria.VALUE[Criteria.ROOK_EARLY_DEVELOPMENT];
    private static final int ROOK_PAIR = Criteria.VALUE[Criteria.ROOK_PAIR];
    private static final int ROOK_OPEN_FILE = Criteria.VALUE[Criteria.ROOK_OPEN_FILE];
    private static final int ROOK_ON_QUEEN_FILE = Criteria.VALUE[Criteria.ROOK_ON_QUEEN_FILE];
    private static final int BISHOP_PAIR = Criteria.VALUE[Criteria.BISHOP_PAIR];
    private static final int BISHOP_OUTPOST = Criteria.VALUE[Criteria.BISHOP_OUTPOST];
    private static final int KNIGHT_PAIR = Criteria.VALUE[Criteria.KNIGHT_PAIR];
    private static final int KNIGHT_OUTPOST = Criteria.VALUE[Criteria.KNIGHT_OUTPOST];
    private static final int DOUBLED_PAWN = Criteria.VALUE[Criteria.DOUBLED_PAWN];
    private static final int WEAK_PAWN = Criteria.VALUE[Criteria.WEAK_PAWN];
    private static final int ISOLATED_PAWN = Criteria.VALUE[Criteria.ISOLATED_PAWN];
    private static final int PAWN_PROTECTS = Criteria.VALUE[Criteria.PAWN_PROTECTS];
    private static final int PAWN_STORM_OWN_KING_OPPOSITE = Criteria.VALUE[Criteria.PAWN_STORM_OWN_KING_OPPOSITE];
    private static final int PASSED_PAWN_PHALANX = Criteria.VALUE[Criteria.PASSED_PAWN_PHALANX];

    private long[] board;
    private int playerToMove;
    private long allOccupancy;
    private long[] playerOccupancy = new long[2];
    private int[] playerKingSquare = new int[2];
    private int[] playerKingRank = new int[2];
    private int[] playerKingFile = new int[2];
    private int phase;
    private Score score;
    private boolean hasEvaluated;

    private void kingEval(int player, int other, long rookBitboard, long pawnBitboard, long otherPawnBitboard) {
        int kingRank = this.playerKingRank[player];
        // king on back rank evals - king blocks rook, rook protects king, pawn shield, opponent pawn storm
        if(kingRank == (player == Value.WHITE ? 0 : 7)) {
            switch(this.playerKingFile[player]) {
                case 0: {
                    if(((player == 0 ? 0x000000000000000eL : 0x0e00000000000000L) & rookBitboard) != 0L) score.addCriteria(player, Criteria.ROOK_PROTECTS, ROOK_PROTECTS);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_CLOSE, PAWN_SHIELD_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_FAR, PAWN_SHIELD_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_CLOSE, -PAWN_STORM_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER1 - player][0] & otherPawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_FAR, -PAWN_STORM_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_QUEENSIDE_FAR_PLAYER1 - player][0] & otherPawnBitboard)]);
                    break;
                }
                case 1: {
                    if(((player == 0 ? 0x000000000000000cL : 0x0c00000000000000L) & rookBitboard) != 0L) score.addCriteria(player, Criteria.ROOK_PROTECTS, ROOK_PROTECTS);
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) score.addCriteria(player, Criteria.KING_BLOCKS_ROOK, -KING_BLOCKS_ROOK);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_CLOSE, PAWN_SHIELD_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_FAR, PAWN_SHIELD_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_CLOSE, -PAWN_STORM_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER1 - player][0] & otherPawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_FAR, -PAWN_STORM_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_QUEENSIDE_FAR_PLAYER1 - player][0] & otherPawnBitboard)]);
                    break;
                }
                case 2: {
                    if(((player == 0 ? 0x0000000000000008L : 0x0800000000000000L) & rookBitboard) != 0L) score.addCriteria(player, Criteria.ROOK_PROTECTS, ROOK_PROTECTS);
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) score.addCriteria(player, Criteria.KING_BLOCKS_ROOK, -KING_BLOCKS_ROOK);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_CLOSE, PAWN_SHIELD_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_FAR, PAWN_SHIELD_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_CLOSE, -PAWN_STORM_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER1 - player][0] & otherPawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_FAR, -PAWN_STORM_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_QUEENSIDE_FAR_PLAYER1 - player][0] & otherPawnBitboard)]);
                    break;
                }
                case 3: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) score.addCriteria(player, Criteria.KING_BLOCKS_ROOK, -KING_BLOCKS_ROOK);
                    break;
                }
                case 4: {
                    break;
                }
                case 5: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) score.addCriteria(player, Criteria.KING_BLOCKS_ROOK, -KING_BLOCKS_ROOK);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_CLOSE, PAWN_SHIELD_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_FAR, PAWN_SHIELD_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_CLOSE, -PAWN_STORM_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_KINGSIDE_CLOSE_PLAYER1 - player][0] & otherPawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_FAR, -PAWN_STORM_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_KINGSIDE_FAR_PLAYER1 - player][0] & otherPawnBitboard)]);
                    break;
                }
                case 6: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) score.addCriteria(player, Criteria.KING_BLOCKS_ROOK, -KING_BLOCKS_ROOK);
                    if(((player == 0 ? 0x0000000000000020L : 0x2000000000000000L) & rookBitboard) != 0L) score.addCriteria(player, Criteria.ROOK_PROTECTS, ROOK_PROTECTS);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_CLOSE, PAWN_SHIELD_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_FAR, PAWN_SHIELD_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_CLOSE, -PAWN_STORM_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_KINGSIDE_CLOSE_PLAYER1 - player][0] & otherPawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_FAR, -PAWN_STORM_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_KINGSIDE_FAR_PLAYER1 - player][0] & otherPawnBitboard)]);
                    break;
                }
                case 7: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) score.addCriteria(player, Criteria.KING_BLOCKS_ROOK, -KING_BLOCKS_ROOK);
                    if(((player == 0 ? 0x0000000000000060L : 0x6000000000000000L) & rookBitboard) != 0L) score.addCriteria(player, Criteria.ROOK_PROTECTS, ROOK_PROTECTS);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_CLOSE, PAWN_SHIELD_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_SHIELD_FAR, PAWN_SHIELD_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_CLOSE, -PAWN_STORM_CLOSE_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_KINGSIDE_CLOSE_PLAYER1 - player][0] & otherPawnBitboard)]);
                    score.addCriteria(player, Criteria.PAWN_STORM_FAR, -PAWN_STORM_FAR_VALUE[Long.bitCount(B.BB[B.PAWN_STORM_KINGSIDE_FAR_PLAYER1 - player][0] & otherPawnBitboard)]);
                    break;
                }
                default: break;
            }
        }
        // king endgame distance
        int playerPieceMaterial = Board.materialValuePieces(board, player);
        int otherPieceMaterial = Board.materialValuePieces(board, other);
        if(playerPieceMaterial <= Piece.VALUE[Piece.QUEEN] && otherPieceMaterial <= Piece.VALUE[Piece.QUEEN] && playerPieceMaterial > otherPieceMaterial) {
            int oFileDist = Math.abs(7 - (playerKingFile[other] << 1)) - 1;
            int oRankDist = Math.abs(7 - (playerKingRank[other] << 1)) - 1;
            int oDist = ((oFileDist * oFileDist * oFileDist) + (oRankDist * oRankDist * oRankDist)) / 5;
            int distBetweenKings = (9 - Math.max(Math.abs(playerKingFile[player] - playerKingFile[other]), Math.abs(playerKingRank[player] - playerKingRank[other])));
            score.addCriteria(player, Criteria.KING_ENDGAME_DISTANCE, ((oDist + distBetweenKings) * phase) / 24);
        }
    }

    private void queenEval(int player, int other, long bitboard, long bishopBitboard, long knightBitboard) {
        // material value
        score.addCriteria(player, Criteria.MATERIAL_QUEEN, PIECE_VALUE[Piece.QUEEN][Long.bitCount(bitboard)]);
        // early development
        if((bitboard & B.BB[B.QUEEN_START_POSITION_PLAYER0 + player][0]) == 0L &&
           (bishopBitboard & B.BB[B.BISHOP_START_POSITION_PLAYER0 + player][0]) != 0L &&
           (knightBitboard & B.BB[B.KNIGHT_START_POSITION_PLAYER0 + player][0]) != 0L) score.addCriteria(player, Criteria.QUEEN_EARLY_DEVELOPMENT, -QUEEN_EARLY_DEVELOPMENT);
        int square;
        long queenAttacks;
        for(; bitboard != 0L; bitboard &= bitboard - 1) {
            square = Long.numberOfTrailingZeros(bitboard);
            // piece square bonus
            score.addCriteria(player, Criteria.SQUARE_BONUS_QUEEN, Psqt.BONUS[Piece.QUEEN][player][square][phase]);
            // mobility
            queenAttacks = Magic.queenMoves(square, allOccupancy) & ~this.playerOccupancy[player];
            score.addCriteria(player, Criteria.MOBILITY_QUEEN, MOBILITY_VALUE[Piece.QUEEN][Long.bitCount(queenAttacks)]);
            // queen affects other king safety
            score.addCriteria(player, Criteria.QUEEN_AFFECTS_KING_SAFETY, KING_SAFETY_VALUE[Piece.QUEEN][Long.bitCount(queenAttacks & B.BB[B.KING_RING_PLAYER1 - player][0])]);
            // enemy king distance
            score.addCriteria(player, Criteria.QUEEN_ENEMY_KING_DISTANCE, KING_DISTANCE_VALUE[Piece.QUEEN][((Math.abs((square >>> 3) - playerKingRank[other]) + Math.abs((square & 7) - playerKingFile[other])))]);
        }
    }

    private void rookEval(int player, int other, long bitboard, long kingBitboard, long pawnBitboard, long otherPawnBitboard, long otherQueenBitboard) {
        // material value
        int numRooks = Long.bitCount(bitboard);
        score.addCriteria(player, Criteria.MATERIAL_ROOK, PIECE_VALUE[Piece.ROOK][numRooks]);
        // early development
        if(Long.bitCount(bitboard & B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0]) < 2 && (kingBitboard & B.BB[B.KING_START_POSITION_PLAYER0 + player][0]) != 0L) score.addCriteria(player, Criteria.ROOK_EARLY_DEVELOPMENT, -ROOK_EARLY_DEVELOPMENT);
        // rook pair
        score.addCriteria(player, Criteria.ROOK_PAIR, (numRooks > 1 ? -ROOK_PAIR : 0));
        // rooks and pawns
        int numPawns = Long.bitCount(pawnBitboard);
		score.addCriteria(player, Criteria.ROOK_PAWN, ROOK_PAWN_VALUE[numRooks][numPawns]);
        int square;
        long rookAttacks;
        int rookFile;
        for(; bitboard != 0L; bitboard &= bitboard - 1) {
            square = Long.numberOfTrailingZeros(bitboard);
            // piece square bonus
            score.addCriteria(player, Criteria.SQUARE_BONUS_ROOK, Psqt.BONUS[Piece.ROOK][player][square][phase]);
            // mobility
            rookAttacks = Magic.rookMoves(square, allOccupancy) & ~this.playerOccupancy[player];
            score.addCriteria(player, Criteria.MOBILITY_ROOK, MOBILITY_VALUE[Piece.ROOK][Long.bitCount(rookAttacks)]);
            // rook open file
            rookFile = square & 7;
            score.addCriteria(player, Criteria.ROOK_OPEN_FILE, ((pawnBitboard & B.BB[B.FILE][rookFile]) == 0L ? ROOK_OPEN_FILE : 0) + ((otherPawnBitboard & B.BB[B.FILE][rookFile]) == 0L ? ROOK_OPEN_FILE : 0));
            // rook on other queen file
            score.addCriteria(player, Criteria.ROOK_ON_QUEEN_FILE, (otherQueenBitboard & B.BB[B.FILE][rookFile]) != 0L ? ROOK_ON_QUEEN_FILE : 0);
            // other king safety
            score.addCriteria(player, Criteria.ROOK_AFFECTS_KING_SAFETY, KING_SAFETY_VALUE[Piece.ROOK][Long.bitCount(rookAttacks & B.BB[B.KING_RING_PLAYER1 - player][0])]);
            // other king distance
            score.addCriteria(player, Criteria.ROOK_ENEMY_KING_DISTANCE, KING_DISTANCE_VALUE[Piece.ROOK][((Math.abs((square >>> 3) - playerKingRank[other]) + Math.abs(rookFile - playerKingFile[other])))]);
        }
    }

    private void bishopEval(int player, int other, long bitboard, long pawnBitboard, long otherPawnBitboard) {
        // material value
        int numBishops = Long.bitCount(bitboard);
        score.addCriteria(player, Criteria.MATERIAL_BISHOP, PIECE_VALUE[Piece.BISHOP][numBishops]);
        // bishop pair
        score.addCriteria(player, Criteria.BISHOP_PAIR, (numBishops > 1 ? BISHOP_PAIR : 0));
        int square;
        long bishopAttacks;
        int bishopFile;
        int bishopRank;
        long squareColorBitboard;
        for(; bitboard != 0L; bitboard &= bitboard - 1) {
            square = Long.numberOfTrailingZeros(bitboard);
            // piece square bonus
            score.addCriteria(player, Criteria.SQUARE_BONUS_BISHOP, Psqt.BONUS[Piece.BISHOP][player][square][phase]);
            // mobility
            bishopAttacks = Magic.bishopMoves(square, allOccupancy) & ~this.playerOccupancy[player];
            score.addCriteria(player, Criteria.MOBILITY_BISHOP, MOBILITY_VALUE[Piece.BISHOP][Long.bitCount(bishopAttacks)]);
            // outpost
            bishopFile = square & 7;
            bishopRank = square >>> 3;
            if((B.BB[B.PAWN_ATTACKS_PLAYER1 - player][square] & pawnBitboard) != 0L) {
                if((B.BB[B.PASSED_PAWNS_FILES_PLAYER0 + player][bishopFile] & B.BB[B.FORWARD_RANKS_PLAYER0 + player][bishopRank] & otherPawnBitboard) == 0L) score.addCriteria(player, Criteria.BISHOP_OUTPOST, BISHOP_OUTPOST);
            }
            // bad bishop
            squareColorBitboard = (B.BB[B.SQUARE_COLOR_LIGHT][0] & (1L << square)) != 0L ? B.BB[B.SQUARE_COLOR_LIGHT][0] : B.BB[B.SQUARE_COLOR_DARK][0];
            score.addCriteria(player, Criteria.BAD_BISHOP, BISHOP_PAWN_VALUE[Long.bitCount(pawnBitboard & squareColorBitboard)][Long.bitCount(otherPawnBitboard & squareColorBitboard)]);
            // own king distance
            score.addCriteria(player, Criteria.BISHOP_PROTECTOR, -(Math.abs(bishopRank - playerKingRank[player]) + Math.abs(bishopFile - playerKingFile[player])));
            // other king safety
            score.addCriteria(player, Criteria.BISHOP_AFFECTS_KING_SAFETY, KING_SAFETY_VALUE[Piece.BISHOP][Long.bitCount(bishopAttacks & B.BB[B.KING_RING_PLAYER1 - player][0])]);
            // other king distance
            score.addCriteria(player, Criteria.BISHOP_ENEMY_KING_DISTANCE, KING_DISTANCE_VALUE[Piece.BISHOP][((Math.abs(bishopRank - playerKingRank[other]) + Math.abs(bishopFile - playerKingFile[other])))]);
        }
    }

    private void knightEval(int player, int other, long bitboard, long pawnBitboard, long otherPawnBitboard) {
        // material value
        int numKnights = Long.bitCount(bitboard);
        score.addCriteria(player, Criteria.MATERIAL_KNIGHT, PIECE_VALUE[Piece.KNIGHT][numKnights]);
        // knight pair
        score.addCriteria(player, Criteria.KNIGHT_PAIR, (numKnights > 1 ? -KNIGHT_PAIR : 0));
        // knight and pawns
        int numPawns = Long.bitCount(pawnBitboard);
        score.addCriteria(player, Criteria.KNIGHT_PAWN, KNIGHT_PAWN_VALUE[numKnights][numPawns]);
        int square;
        long knightAttacks;
        int knightFile;
        int knightRank;
        for(; bitboard != 0L; bitboard &= bitboard - 1) {
            square = Long.numberOfTrailingZeros(bitboard);
            // piece square bonus
            score.addCriteria(player, Criteria.SQUARE_BONUS_KNIGHT, Psqt.BONUS[Piece.KNIGHT][player][square][phase]);
            // mobility
            knightAttacks = B.BB[B.LEAP_ATTACKS][square] & ~this.playerOccupancy[player];
            score.addCriteria(player, Criteria.MOBILITY_KNIGHT, MOBILITY_VALUE[Piece.KNIGHT][Long.bitCount(knightAttacks)]);
            // outpost
            knightFile = square & 7;
            knightRank = square >>> 3;
            if((B.BB[B.PAWN_ATTACKS_PLAYER1 - player][square] & pawnBitboard) != 0L) {
                if((B.BB[B.PASSED_PAWNS_FILES_PLAYER0 + player][knightFile] & B.BB[B.FORWARD_RANKS_PLAYER0 + player][knightRank] & otherPawnBitboard) == 0L) score.addCriteria(player, Criteria.KNIGHT_OUTPOST, KNIGHT_OUTPOST);
            }
            // own king distance
            score.addCriteria(player, Criteria.KNIGHT_PROTECTOR, -(Math.abs(knightRank - playerKingRank[player]) + Math.abs(knightFile - playerKingFile[player])));
            // other king safety
            score.addCriteria(player, Criteria.KNIGHT_AFFECTS_KING_SAFETY, KING_SAFETY_VALUE[Piece.KNIGHT][Long.bitCount(knightAttacks & B.BB[B.KING_RING_PLAYER1 - player][0])]);
            // other king distance
            score.addCriteria(player, Criteria.KNIGHT_ENEMY_KING_DISTANCE, KING_DISTANCE_VALUE[Piece.KNIGHT][((Math.abs(knightRank - playerKingRank[other]) + Math.abs(knightFile - playerKingFile[other])))]);
        }
    }

    private void pawnEval(int player, int other, long bitboard, long otherPawnBitboard, long kingBitboard) {
        // material value
        int numPawns = Long.bitCount(bitboard);
        score.addCriteria(player, Criteria.MATERIAL_PAWN, PIECE_VALUE[Piece.PAWN][numPawns]);
        long originalBitboard = bitboard;
        int square;
        int pawnFile;
        long pawnFileBitboard;
        int pawnRank;
        long adjacentFilesBitboard;
        long adjacentFilePawns;
        long forwardRanksBitboard;
        long otherPassedPawnBlockers;
        for(; bitboard != 0L; bitboard &= bitboard - 1) {
            square = Long.numberOfTrailingZeros(bitboard);
            // piece square bonus
            score.addCriteria(player, Criteria.SQUARE_BONUS_PAWN, Psqt.BONUS[Piece.PAWN][player][square][phase]);
            // doubled pawns
            pawnFile = square & 7;
            pawnFileBitboard = B.BB[B.FILE][pawnFile];
            if(Long.bitCount(bitboard & pawnFileBitboard) > 1) score.addCriteria(player, Criteria.DOUBLED_PAWN, -DOUBLED_PAWN);
            // weak pawn
            pawnRank = square >>> 3;
            adjacentFilesBitboard = (pawnFile > 0 ? B.BB[B.FILE][pawnFile - 1] : 0L) | (pawnFile < 7 ? B.BB[B.FILE][pawnFile + 1] : 0L);
            adjacentFilePawns = originalBitboard & adjacentFilesBitboard;
            if((adjacentFilePawns & B.BB[B.FORWARD_RANKS_PLAYER1 - player][pawnRank]) == 0L) score.addCriteria(player, Criteria.WEAK_PAWN, -WEAK_PAWN);
            // isolated pawn
            if(adjacentFilePawns == 0L) score.addCriteria(player, Criteria.ISOLATED_PAWN, -ISOLATED_PAWN);
            // pawn protects
            if((B.BB[B.PAWN_ATTACKS_PLAYER0 + player][square] & this.playerOccupancy[player]) != 0L) score.addCriteria(player, Criteria.PAWN_PROTECTS, PAWN_PROTECTS);
            // pawn storm when own king on opposite side
            if(pawnFile < 3) {
                if(playerKingFile[player] > 4) score.addCriteria(player, Criteria.PAWN_STORM_OWN_KING_OPPOSITE, PAWN_STORM_OWN_KING_OPPOSITE);
            }
            if(pawnFile > 4) {
                if(playerKingFile[player] < 3) score.addCriteria(player, Criteria.PAWN_STORM_OWN_KING_OPPOSITE, PAWN_STORM_OWN_KING_OPPOSITE);
            }
            // passed pawn
            forwardRanksBitboard = B.BB[B.FORWARD_RANKS_PLAYER0 + player][pawnRank];
            otherPassedPawnBlockers = otherPawnBitboard & (pawnFileBitboard | adjacentFilesBitboard) & forwardRanksBitboard;
            if(otherPassedPawnBlockers == 0L) {
                // additional piece square bonus
                score.addCriteria(player, Criteria.PASSED_PAWN_SQUARE_BONUS, Psqt.BONUS[Piece.PAWN][player][square][phase]);
                // phalanx
                score.addCriteria(player, Criteria.PASSED_PAWN_PHALANX, (originalBitboard & adjacentFilesBitboard & B.BB[B.RANK][pawnRank]) > 0L ? PASSED_PAWN_PHALANX : 0);
                // other king stops pawn when other has no material
                int pawnPromoteDist = Math.abs((player == 0 ? 7 : 0) - pawnRank) + (pawnRank == (player == 0 ? 1 : 6) ? 1 : 0);
				int otherKingDistFromPromote = Math.max(Math.abs((player == 0 ? 7 : 0) - playerKingRank[other]), Math.abs(pawnFile - playerKingFile[other]));
				int pawnTurnToMove = player == this.playerToMove  ? 1 : 0;
				int kingTurnToMove = 1 ^ pawnTurnToMove;
				int ownKingInFront = (kingBitboard & B.BB[B.FORWARD_RANKS_PLAYER0 + player][pawnRank] & B.BB[B.FILE][pawnFile]) != 0L ? 1 : 0;
				int pawnDist = pawnPromoteDist - pawnTurnToMove + ownKingInFront;
				int kingDist = otherKingDistFromPromote - kingTurnToMove;
				if(kingDist > pawnDist) {
					score.addCriteria(player, Criteria.PASSED_PAWN_UNSTOPPABLE, Piece.VALUE[Piece.BISHOP]);
				}
                // other king distance when low material
                kingDist = 8 - Math.max(Math.abs(playerKingRank[player] - pawnRank), Math.abs(playerKingFile[player] - pawnFile));
				int otherKingDist = Math.max(Math.abs(playerKingRank[other] - pawnRank), Math.abs(playerKingFile[other] - pawnFile));
				score.addCriteria(player, Criteria.PASSED_PAWN_ENEMY_KING_DISTANCE, (kingDist * kingDist + otherKingDist * otherKingDist) * (player == 0 ? pawnRank : 7 - pawnRank));
            }
        }
    }

    private static int getNextAttackingPiece(long[] board, int square, int player) {
        int playerBit = player << 3;
        long bitboard = B.BB[B.PAWN_ATTACKS_PLAYER1 - player][square] & board[Piece.PAWN | playerBit];
        if (bitboard != 0L) return Long.numberOfTrailingZeros(bitboard);
        bitboard = B.BB[B.LEAP_ATTACKS][square] & board[Piece.KNIGHT | playerBit];
        if (bitboard != 0L) return Long.numberOfTrailingZeros(bitboard);
        long allOccupancy = board[Value.WHITE_BIT] | board[Value.BLACK_BIT];
        bitboard = Magic.bishopMoves(square, allOccupancy) & board[Piece.BISHOP | playerBit];
        if (bitboard != 0L) return Long.numberOfTrailingZeros(bitboard);
        bitboard = Magic.rookMoves(square, allOccupancy) & board[Piece.ROOK | playerBit];
        if (bitboard != 0L) return Long.numberOfTrailingZeros(bitboard);
        bitboard = Magic.queenMoves(square, allOccupancy) & board[Piece.QUEEN | playerBit];
        if (bitboard != 0L) return Long.numberOfTrailingZeros(bitboard);
        return Value.INVALID;
    }
    
}
