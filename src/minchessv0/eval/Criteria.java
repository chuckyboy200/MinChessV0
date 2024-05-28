package minchessv0.eval;

import minchessv0.util.Piece;

public class Criteria {
    
    public static final int MATERIAL_QUEEN = 1;
    public static final int MATERIAL_ROOK = 2;
    public static final int MATERIAL_BISHOP = 3;
    public static final int MATERIAL_KNIGHT = 4;
    public static final int MATERIAL_PAWN = 5;
    public static final int SQUARE_BONUS_KING = 6;
    public static final int SQUARE_BONUS_QUEEN = 7;
    public static final int SQUARE_BONUS_ROOK = 8;
    public static final int SQUARE_BONUS_BISHOP = 9;
    public static final int SQUARE_BONUS_KNIGHT = 10;
    public static final int SQUARE_BONUS_PAWN = 11;
    public static final int PAWN_SHIELD_CLOSE = 12;
    public static final int PAWN_SHIELD_FAR = 13;
    public static final int PAWN_STORM_CLOSE = 14;
    public static final int PAWN_STORM_FAR = 15;
    public static final int ROOK_PROTECTS = 16;
    public static final int KING_BLOCKS_ROOK = 17;
    public static final int QUEEN_EARLY_DEVELOPMENT = 18;
    public static final int ROOK_EARLY_DEVELOPMENT = 19;
    public static final int ROOK_PAIR = 20;
    public static final int ROOK_OPEN_FILE = 21;
    public static final int ROOK_ON_QUEEN_FILE = 22;
    public static final int BISHOP_PAIR = 23;
    public static final int BISHOP_OUTPOST = 24;
    public static final int KNIGHT_PAIR = 25;
    public static final int KNIGHT_OUTPOST = 26;
    public static final int DOUBLED_PAWN = 27;
    public static final int WEAK_PAWN = 28;
    public static final int ISOLATED_PAWN = 29;
    public static final int PAWN_PROTECTS = 30;
    public static final int PAWN_STORM_OWN_KING_OPPOSITE = 31;
    public static final int PASSED_PAWN_PHALANX = 32;
    public static final int KING_ENDGAME_DISTANCE = 33;
    public static final int MOBILITY_QUEEN = 34;
    public static final int MOBILITY_ROOK = 35;
    public static final int MOBILITY_BISHOP = 36;
    public static final int MOBILITY_KNIGHT = 37;
    public static final int QUEEN_AFFECTS_KING_SAFETY = 38;
    public static final int ROOK_AFFECTS_KING_SAFETY = 39;
    public static final int BISHOP_AFFECTS_KING_SAFETY = 40;
    public static final int KNIGHT_AFFECTS_KING_SAFETY = 41;
    public static final int QUEEN_ENEMY_KING_DISTANCE = 42;
    public static final int ROOK_ENEMY_KING_DISTANCE = 43;
    public static final int BISHOP_ENEMY_KING_DISTANCE = 44;
    public static final int KNIGHT_ENEMY_KING_DISTANCE = 45;
    public static final int ROOK_PAWN = 46;
    public static final int BAD_BISHOP = 47;
    public static final int BISHOP_PROTECTOR = 48;
    public static final int KNIGHT_PAWN = 49;
    public static final int KNIGHT_PROTECTOR = 50;
    public static final int PASSED_PAWN_SQUARE_BONUS = 51;
    public static final int PASSED_PAWN_UNSTOPPABLE = 52;
    public static final int PASSED_PAWN_ENEMY_KING_DISTANCE = 53;

    public static final int MAX_CRITERIA = 54;
    public static final String[] CRITERIA_NAME = new String[MAX_CRITERIA];
    public static final int[] VALUE = new int[MAX_CRITERIA];

    static {
        String[] tempName = { "", // 0
            "m_queen", "m_rook", "m_bishop", "m_knight", "m_pawn", // 1 - 5
            "s_king", "s_queen", "s_rook", "s_bishop", "s_knight", "s_pawn", // 6 - 11
            "b_pawnShieldClose", "b_pawnShieldFar", "p_pawnStormClose", "p_pawnStormFar", // 12 - 15
            "b_rookProtects", "p_kingBlocksRook", // 16 - 17
            "p_queenEarlyDevelopment", // 18
            "p_rookEarlyDevelopment", "p_rookPair", "b_rookOpenFile", "b_rookOnQueenFile", // 19 - 22
            "b_bishopPair", "b_bishopOutpost", // 23 - 24
            "p_knightPair", "b_knightOutpost", // 25 - 26
            "p_doubledPawn", "p_weakPawn", "p_isolatedPawn", "b_pawnProtects", "b_pawnStormOwnKingOpposite", "b_passed_pawn_phalanx", // 27 - 32
            "kingEndgameDistance", // 33
            "mobilityQueen", "mobilityRook", "mobilityBishop", "mobilityKnight", // 34 - 37
            "queenAffectsKingSafety", "rookAffectsKingSafety", "bishopAffectsKingSafety", "knightAffectsKingSafety", // 38 - 41
            "queenEnemyKingDistance", "rookEnemyKingDistance", "bishopEnemyKingDistance", "knightEnemyKingDistance", // 42 - 45
            "rookPawn", // 46
            "badBishop", "bishopProtector", // 47 - 48
            "knightPawn", "knightProtector", // 49 - 50
            "passedPawnSquareBonus", "passedPawnUnstoppable", "passedPawnEnemyKingDistance" // 51 - 53
        };
        int[] tempValue = { 0, // all values positive, bonuses added or penalties subtracted in eval function
            Piece.VALUE[Piece.QUEEN], Piece.VALUE[Piece.ROOK], Piece.VALUE[Piece.BISHOP], Piece.VALUE[Piece.KNIGHT], Piece.VALUE[Piece.PAWN],
            0, 0, 0, 0, 0, 0,
            10, 5, 7, 3,
            30, 30,
            30,
            30, 14, 14, 11,
            39, 14,
            14, 21,
            11, 11, 11, 7, 28, 11,
            0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0,
            0, 0,
            0, 0,
            0, 0, 0
        };
        for(int i = 0; i < MAX_CRITERIA; i ++) {
            CRITERIA_NAME[i] = tempName[i];
            VALUE[i] = tempValue[i];
        }
    }

    private Criteria() {}

}
