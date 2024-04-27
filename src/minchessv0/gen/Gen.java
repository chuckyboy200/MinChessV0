package minchessv0.gen;

import minchessv0.board.Board;
import minchessv0.util.B;
import minchessv0.util.Magic;
import minchessv0.util.Piece;
import minchessv0.util.Value;

public class Gen {
    
    
    /**
     * This is the maximum number of moves (+1) that can be held in the moves array
     * The last element contains the number of actual moves in the array, since the array is a constant size
     */
    public static final int MAX_MOVELIST_SIZE = 100;

    /**
     * This is the index into the moves array which holds the value for the number of actual moves in the array
     */
    public static final int MOVELIST_SIZE = MAX_MOVELIST_SIZE - 1;

    /**
     * This is a factory method which generates all moves for a given board
     * 
     * @param board    the board array
     * @param legal    whether to generate only legal moves
     * @param tactical whether to generate only tactical moves
     * @return an array of moves, the array's max size is set at 100, the last
     *         element of the array is the length of the move list
     */
    public static long[] gen(long[] board, boolean legal, boolean tactical) {
        /*
         * get the player to move from STATUS, the playerBit (for index into the
         * appropriate bitboard, and the otherBit (for index into the other player's
         * bitboard)
         */
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        int playerBit = player << Board.PLAYER_SHIFT;
        int otherBit = 8 ^ playerBit;
        /*
         * get the occupancy bitboard for all squares on the board and the tactical
         * occupancy, depending on whether only tactical moves are required
         */
        long allOccupancy = board[playerBit] | board[otherBit];
        long tacticalOccupancy = tactical ? board[otherBit] : ~board[playerBit];
        /*
         * create an array of ints to store the moves, the array's max size is set at
         * 100, the last element of the array is the length of the move list
         */
        long[] moves = new long[MAX_MOVELIST_SIZE];
        /*
         * generate king moves, knight moves, pawn moves, and slider moves, and store
         * them in the moves array, moveListLength is updated to be the current number
         * of moves in the moves array
         */
        int moveListLength = 0;
        moveListLength = getKingMoves(board, moves, Piece.KING | playerBit, moveListLength, player, allOccupancy, tacticalOccupancy, tactical);
        moveListLength = getKnightMoves(board, moves, Piece.KNIGHT | playerBit, moveListLength, tacticalOccupancy);
        moveListLength = getPawnMoves(board, moves, Piece.PAWN | playerBit, moveListLength, player, allOccupancy, board[otherBit], tactical);
        moveListLength = getSliderMoves(board, moves, player, moveListLength, allOccupancy, tacticalOccupancy);
        /*
         * throw an error if there are more moves than can fit in the moves array
         */
        if (moveListLength > MAX_MOVELIST_SIZE - 2) throw new RuntimeException("Move list overflow");
        /*
         * set the last element in the moves array to the length of the move list
         */
        moves[MOVELIST_SIZE] = moveListLength;
        return legal ? purgeIllegalMoves(board, moves, player) : moves;
    }

    /**
     * This is a static utility class and should not be instantiated
     */
    private Gen() {}

    /**
     * Create a new movelist which contains only legal moves by iterating
     * over all pseudo-legal moves, making the move and checking whether
     * that players king is in check. If it is not in check, add the
     * move to the legal movelist
     * 
     * @param board  The board array
     * @param moves  The pseudo-legal moves array
     * @param player The player to move
     * @return
     */
    private static long[] purgeIllegalMoves(long[] board, long[] moves, int player) {
        /*
         * create a new legal movelist
         */
        long[] legalMoves = new long[MAX_MOVELIST_SIZE];
        /*
         * initialise the number of legal moves
         */
        int legalMoveCount = 0;
        /*
         * pre-declare variables used within loop
         */
        long[] boardAfterMove;
        long move;
        /*
         * iterate over all pseudo-legal moves
         */
        for(int i = 0; i < moves[99]; i ++) {
            move = moves[i];
            /*
             * create a new board after making the move, check if the
             * player is not in check, add the move if so
             */
            boardAfterMove = Board.makeMove(board, move);
            if(!Board.isPlayerInCheck(boardAfterMove, player)) legalMoves[legalMoveCount ++] = move;
        }
        /*
         * set the legal movelist size to the legal move count
         */
        legalMoves[MOVELIST_SIZE] = legalMoveCount;
        return legalMoves;
    }

    /**
     * During move generation, add a move to the moves array
     * 
     * @param board          the board array
     * @param moves          the moves array
     * @param startSquare    the start square of the move
     * @param targetSquare   the target square of the move
     * @param moveListLength the current number of moves in the moves array
     * @param piece          the piece being moved
     */
    private static void addMove(long[] board, long[] moves, int startSquare, int targetSquare, int moveListLength, int piece) {
        moves[moveListLength] = startSquare | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
    }

    /**
     * During move generation, add pawn promotion moves to the moves array
     * 
     * @param board          the board array
     * @param moves          the moves array
     * @param startSquare    the start square of the move
     * @param targetSquare   the target square of the move
     * @param playerBit      the player bit
     * @param moveListLength the current number of moves in the moves array
     * @param piece          the piece being moved
     */
    private static void addPromotionMoves(long[] board, long[] moves, int startSquare, int targetSquare, int playerBit, int moveListLength, int piece) {
        int moveInfo = startSquare | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
        moves[moveListLength++] = moveInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        moves[moveListLength++] = moveInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        moves[moveListLength++] = moveInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        moves[moveListLength++] = moveInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
    }

    /**
     * generate king moves
     * 
     * @param board             the board array
     * @param moves             the moves array
     * @param piece             the king piece
     * @param player            the player to move
     * @param allOccupancy      the occupancy of all squares on the board
     * @param tacticalOccupancy the occupancy of all squares on the board for
     *                          tactical moves
     * @param tactical          whether to generate only tactical moves
     * @return the number of moves in the moves array after king moves have been
     *         generated
     */
    private static int getKingMoves(long[] board, long[] moves, int piece, int player, int moveListLength, long allOccupancy, long tacticalOccupancy, boolean tactical) {
        /*
         * get the king square from the king bitboard
         */
        int square = Long.numberOfTrailingZeros(board[piece]);
        /*
         * get all squares that the king attacks. if tactical is true, only get the
         * squares that are occupied by the other player's pieces, otherwise get all
         * squares not occupied by the player's pieces
         */
        long attacks = B.BB[B.KING_ATTACKS][square] & tacticalOccupancy;
        /*
         * loop over each bit in the attacks bitboard
         * use the bitwise trick of n = n & (n - 1) to remove the lowest significant set bit
         * the loop ends when all bits have been removed
         */
        for(; attacks != 0L; attacks &= attacks - 1L) {
            /*
             * add a move to the moves array for the king moving from the king square to the
             * square of the bit in the attacks bitboard and increment moveListLength
             */
            addMove(board, moves, square, Long.numberOfTrailingZeros(attacks), moveListLength ++, piece);
        }
        /*
         * if tactical is true, don't handle castling and return early
         */
        if(tactical) return moveListLength;
        /*
         * get the castling rights from STATUS and check whether castling is possible on
         * kingside and queenside
         */
        int castling = (int) (board[Board.STATUS] >>> Board.CASTLING_SHIFT) & Board.CASTLING_BITS;
        boolean kingSide = (castling & (player == Value.WHITE ? 0b1 : 0b100)) != Value.NONE;
        boolean queenSide = (castling & (player == Value.WHITE ? 0b10 : 0b1000)) != Value.NONE;
        /*
         * if either side has castling rights, check whether the squares between the
         * king and rook are empty and whether the squares that the king moves through
         * are not attacked by the other player. if so, add a castling move to the moves
         * array and increment moveListLength
         */
        if(kingSide || queenSide) {
            int other = 1 ^ player;
            if(!Board.isSquareAttackedByPlayer(board, square, other)) {
                if(kingSide) {
                    if((allOccupancy & (player == Value.WHITE ? 0x0000000000000060L : 0x6000000000000000L)) == 0L && !Board.isSquareAttackedByPlayer(board, square + 1, other))
                        addMove(board, moves, square, square + 2, moveListLength ++, piece);
                }
                if(queenSide) {
                    if((allOccupancy & (player == Value.WHITE ? 0x000000000000000eL : 0x0e00000000000000L)) == 0L && !Board.isSquareAttackedByPlayer(board, square - 1, other))
                        addMove(board, moves, square, square - 2, moveListLength ++, piece);
                }
            }
        }
        return moveListLength;
    }

    /**
     * generate knight moves
     * 
     * @param board             the board array
     * @param moves             the moves array
     * @param piece             the knight piece
     * @param moveListLength    the current number of moves in the moves array
     * @param tacticalOccupancy the occupancy of all squares on the board for
     *                          tactical moves
     * @return the number of moves in the moves array after knight moves have been
     *         generated
     */
    private static int getKnightMoves(long[] board, long[] moves, int piece, int moveListLength, long tacticalOccupancy) {
        /*
         * get the knight bitboard and loop over each set bit in the bitboard, where
         * each set bit represents a square with a knight on it
         */
        long knightBitboard = board[piece];
        int square;
        long attacks;
        for(; knightBitboard != 0L; knightBitboard &= knightBitboard - 1) {
            /*
             * get the square of the knight
             */
            square = Long.numberOfTrailingZeros(knightBitboard);
            /*
             * get all squares that the knight attacks and loop over each set bit in the
             * bitboard, where each set bit represents a square that the knight attacks
             */
            attacks = B.BB[B.LEAP_ATTACKS][square] & tacticalOccupancy;
            for(; attacks != 0L; attacks &= attacks - 1) {
                /*
                 * add a move to the moves array for the knight moving from the knight square to
                 * the square of the bit in the attacks bitboard and increment moveListLength
                 */
                addMove(board, moves, square, Long.numberOfTrailingZeros(attacks), moveListLength ++, piece);
            }
        }
        return moveListLength;
    }

    /**
     * generate pawn moves
     * 
     * @param board          the board array
     * @param moves          the moves array
     * @param piece          the pawn piece
     * @param moveListLength the current number of moves in the moves array
     * @param player         the player to move
     * @param allOccupancy   the occupancy of all squares on the board
     * @param otherOccupancy the occupancy of all squares on the board for the other player
     * @param tactical       whether to generate only tactical moves
     * @return
     */
    private static int getPawnMoves(long[] board, long[] moves, int piece, int moveListLength, int player, long allOccupancy, long otherOccupancy, boolean tactical) {
        /*
         * get the pawn bitboard and loop over each set bit in the bitboard, where each
         * set bit represents a square with a pawn on it
         */
        long pawnBitboard = board[piece];
        /*
         * get the player bit for calculations later
         */
        int playerBit = player << Board.PLAYER_SHIFT;
        int square;
        long attacks;
        /*
         * get the en passant square
         */
        int eSquare = Board.enPassantSquare(board);
        /*
         * add the en passant square to the other occupancy bitboard if it is set, as
         * this is a square that the pawn can attack
         */
        otherOccupancy |= (eSquare != Value.INVALID ? (1L << eSquare) : 0L);
        int pawnAdvance1Index = B.PAWN_ADVANCE_1_PLAYER0 + player;
        int pawnAdvance2Index = B.PAWN_ADVANCE_2_PLAYER0 + player;
        int pawnAttacksIndex = B.PAWN_ATTACKS_PLAYER0 + player;
        for(; pawnBitboard != 0L; pawnBitboard &= pawnBitboard - 1) {
            /*
             * get the square of the pawn
             */
            square = Long.numberOfTrailingZeros(pawnBitboard);
            /*
             * initialize and set the attacks bitboard to 0
             */
            attacks = 0L;
            /*
             * if tactical is false, get possible single pawn pushes. get double pawn pushes
             * if relevant
             */
            if (!tactical) {
                /*
                 * get single pawn pushes
                 */
                attacks = B.BB[pawnAdvance1Index][square] & ~allOccupancy;
                if (attacks != 0L) {
                    /*
                     * if there is a single pawn push, get double pawn pushes if relevant
                     */
                    attacks |= B.BB[pawnAdvance2Index][square] & ~allOccupancy;
                }
            }
            /*
             * get pawn attacks
             */
            attacks |= B.BB[pawnAttacksIndex][square] & otherOccupancy;
            /*
             * loop over each set bit in the attacks bitboard, where each set bit represents
             * a square that the pawn attacks
             */
            int targetSquare;
            int targetRank;
            for(; attacks != 0L; attacks &= attacks - 1) {
                /*
                 * get the target square of the attack and the rank of the target square
                 */
                targetSquare = Long.numberOfTrailingZeros(attacks);
                targetRank = targetSquare >>> 3;
                /*
                 * if the target rank is a pawn promotion rank, add a pawn promotion move to the
                 * moves array, otherwise add a normal pawn move to the moves array
                 */
                if(targetRank == 0 || targetRank == 7) {
                    addPromotionMoves(board, moves, square, targetSquare, playerBit, moveListLength, piece);
                    moveListLength += 4;
                } else addMove(board, moves, square, targetSquare, moveListLength ++, piece);
            }
        }
        return moveListLength;
    }

    /**
     * generate slider moves for queens, rooks and bishops
     * 
     * @param board             the board array
     * @param moves             the moves array
     * @param player            the player to move
     * @param moveListLength    the current number of moves in the moves array
     * @param allOccupancy      the occupancy of all squares on the board
     * @param tacticalOccupancy the occupancy of all squares on the board for
     *                          tactical moves
     * @return the number of moves in the moves array after slider moves have been
     *         generated
     */
    private static int getSliderMoves(long[] board, long[] moves, int player, int moveListLength, long allOccupancy, long tacticalOccupancy) {
        /*
         * get the player bit for calculations later
         */
        int playerBit = player << Board.PLAYER_SHIFT;
        /*
         * get the bitboard of all sliders for the player
         */
        long sliderBitboard = board[Piece.QUEEN | playerBit] | board[Piece.ROOK | playerBit] | board[Piece.BISHOP | playerBit];
        /*
         * loop over each set bit in the bitboard, where each set bit represents a
         * square with a slider on it
         */
        int square;
        int piece;
        int pieceType;
        long attacks;
        for(; sliderBitboard != 0L; sliderBitboard &= sliderBitboard - 1) {
            square = Long.numberOfTrailingZeros(sliderBitboard);
            piece = Board.getSquare(board, square);
            pieceType = piece & Piece.TYPE;
            attacks = ((pieceType == Piece.ROOK ? 0L : Magic.bishopMoves(square, allOccupancy)) | (pieceType == Piece.BISHOP ? 0L : Magic.rookMoves(square, allOccupancy))) & tacticalOccupancy;
            for(; attacks != 0L; attacks &= attacks - 1) addMove(board, moves, square, Long.numberOfTrailingZeros(attacks), moveListLength ++, piece);
        }
        return moveListLength;
    }

}
