package minchessv0.board;

import static minchessv0.util.B.BB;
import static minchessv0.util.B.LEAP_ATTACKS;
import static minchessv0.util.B.KING_ATTACKS;
import static minchessv0.util.B.PAWN_ATTACKS_PLAYER1;

import minchessv0.gen.Gen;
import minchessv0.util.Fen;
import minchessv0.util.Magic;
import minchessv0.util.Piece;
import minchessv0.util.Value;
import minchessv0.util.Zobrist;

/**
 * This Java class is a static utility for handling chess boards. It works
 * with board arrays, which include bitboards (longs) to represent piece
 * positions and types, status bits for various game states, and a zobrist
 * key for board hashing. The array indexes use the first 3 bits for piece
 * type identification and the fourth bit to indicate color (white or black).
 * The system allows for quick piece and player information retrieval through
 * bitwise operations. Index positions 0, 7, 8, and 15 are reserved for special
 * purposes. Here's how the bitboards are organized:
 * Index Content
 * 0, 8	 White, Black pieces' occupancy
 * 1, 9	 White, Black king
 * 2,10	 White, Black queen
 * 3,11	 White, Black rook
 * 4,12	 White, Black bishop
 * 5,13	 White, Black knight
 * 6,14	 White, Black pawn
 * 7	 Status bits
 * 15	 Zobrist key
 *
 * @author Charles Clark
 */
public class Board {

    /*
     * This is the index in the board array for the board's various status bits as
     * follows:
     * 1) The player to move (bit 0) 0 = white, 1 = black
     * 2) Castling rights (bits 1-4) bit 1 = white king side, bit 2 = white queen
     * side, bit 3 = black king side, bit 4 = black queen side
     * 3) En passant square (bits 5-10) no en passant square = Value.INVALID, any
     * other valid enpassant square value directly corresponds to the square on the
     * board, e.g. a3 = 16
     * 5) Half move clock (bits 11-17) the number of half moves since the last
     * capture or pawn move, used for the fifty move rule
     * 6) Full move number (bits 18-27) the number of full moves, incremented after
     * black's move
     */
    public static final int STATUS = 7;

    /*
     * This is the maximum number of bitboards in the board array
     */
    public static final int MAX_BITBOARDS = 16;

    /*
     * This is the index in the board array for the board's Zobrist key
     */
    public static final int KEY = MAX_BITBOARDS - 1;

    /*
     * This is used when retrieving the player bit from STATUS
     */
    public static final int PLAYER_BIT = 1;

    /**
     * This method returns the player to move, 0 = white, 1 = black
     * 
     * @param board the board array
     * @return the player to move
     */
    public static int player(long[] board) {
        return (int) board[STATUS] & PLAYER_BIT;
    }

    public static final int WHITE_KINGSIDE_BITS = 0b1;
    public static final int BLACK_KINGSIDE_BITS = 0b100;

    /**
     * This method returns whether kingside castling is possible for a player
     * 
     * @param board  the board array
     * @param player the player to move
     * @return true if kingside castling is possible
     */
    public static boolean kingSide(long[] board, int player) {
        return (board[STATUS] & (player == Value.WHITE ? WHITE_KINGSIDE_BITS : BLACK_KINGSIDE_BITS)) != 0L;
    }

    public static final int WHITE_QUEENSIDE_BITS = 0b10;
    public static final int BLACK_QUEENSIDE_BITS = 0b1000;

    /**
     * This method returns whether queenside castling is possible for a player
     * 
     * @param board  the board array
     * @param player the player to move
     * @return true if queenside castling is possible
     */
    public static boolean queenSide(long[] board, int player) {
        return (board[STATUS] & (player == Value.WHITE ? WHITE_QUEENSIDE_BITS : BLACK_QUEENSIDE_BITS)) != 0L;
    }

    public static final long WHITE_ENPASSANT_SQUARES = 0x0000ff0000000000L;
    public static final long BLACK_ENPASSANT_SQUARES = 0x0000000000ff0000L;

    /**
     * This method returns whether an en passant square is valid for the position
     * 
     * @param square candidate en passant square
     * @param player player
     * @return true if square is valid for player to capture en passant
     */
    public static boolean isValidEnPassantSquareForPlayer(int square, int player) {
        return ((1L << square) & (player == 0 ? WHITE_ENPASSANT_SQUARES : BLACK_ENPASSANT_SQUARES)) != 0L;
    }

    /**
     * These are used when retrieving the EnPassant bits from STATUS
     */
    public static final int ESQUARE_SHIFT = 5;
    public static final int SQUARE_BITS = 0b111111;
    
    /**
     * This method returns whether the current En passant square is a valid value
     * 
     * @param board the board array
     * @return true if the current En passant square is a valid value
     */
    public static boolean hasValidEnPassantSquare(long[] board) {
        return ((1L << ((int) board[STATUS] >>> ESQUARE_SHIFT & SQUARE_BITS)) & (((int) board[STATUS] & PLAYER_BIT) == 0 ? WHITE_ENPASSANT_SQUARES : BLACK_ENPASSANT_SQUARES)) != 0L;
    }

    /**
     * This method returns a valid En passant square or Value.INVALID if not
     * 
     * @param board the board array
     * @return a valid En passant square or Value.INVALID if not
     */
    public static int enPassantSquare(long[] board) {
        int eSquare = (int) board[STATUS] >>> ESQUARE_SHIFT & SQUARE_BITS;
        return ((1L << eSquare) & (((int) board[STATUS] & PLAYER_BIT) == 0 ? WHITE_ENPASSANT_SQUARES : BLACK_ENPASSANT_SQUARES)) != 0L ? eSquare : Value.INVALID;
    }

    /**
     * These are used when retrieving the half move clock bits from STATUS
     */
    public static final int HALF_MOVE_CLOCK_SHIFT = 11;
    public static final int HALF_MOVE_CLOCK_BITS = 0b1111111;

    /**
     * This method returns the half move clock
     * 
     * @param board the board array
     * @return the half move clock
     */
    public static int halfMoveClock(long[] board) {
        return (int) board[STATUS] >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
    }

    /**
     * These are used when retrieving the full move number bits from STATUS
     */
    public static final int FULL_MOVE_NUMBER_SHIFT = 18;
    public static final int FULL_MOVE_NUMBER_BITS = 0b1111111111;

    /**
     * This method returns the full move number
     * 
     * @param board the board array
     * @return the full move number
     */
    public static int fullMoveNumber(long[] board) {
        return (int) board[STATUS] >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
    }

    /**
     * This method returns the boards Zobrist key, although this isn't necessary since you
     * can just access the value in the board array directly
     * 
     * @param board the board array
     * @return the Zobrist key
     */
    public static long key(long[] board) {
        return board[KEY];
    }

    public static final String FEN_STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * This is a factory method which creates a new board array from a FEN string
     * representing the starting position
     * 
     * @return a new board array representing the starting position
     */
    public static long[] startingPosition() {
        return fromFen(FEN_STARTING_POSITION);
    }

    /**
     * These are used when retrieving the castling bits from STATUS
     */
    public static final int CASTLING_SHIFT = 1;
    public static final int CASTLING_BITS = 0b1111;

    /**
     * This is a factory method which creates a new board array from a FEN string
     * 
     * @param fen the FEN string
     * @return a new board array representing the position in the FEN string
     */
    public static long[] fromFen(String fen) {
        /*
         * create a new empty board array
         */
        long[] board = new long[MAX_BITBOARDS];
        /*
         * get an array of ints representing 64 squares and their contents from the FEN
         * string
         */
        int[] pieces = Fen.getPieces(fen);
        /*
         * loop over the squares on the board
         */
        int piece;
        long squareBit;
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            /*
             * if the square is not empty, set the corresponding bit in the appropriate
             * piece bitboard and occupancy bitboard for that player
             */
            piece = pieces[square];
            if(piece != Value.NONE) {
                squareBit = 1L << square;
                board[piece] |= squareBit;
                board[piece & Value.BLACK_BIT] |= squareBit;
            }
        }
        /*
         * get whether it is white to move from the FEN string and set the player bit in
         * STATUS
         */
        boolean whiteToMove = Fen.getWhiteToMove(fen);
        board[STATUS] = whiteToMove ? Value.WHITE : Value.BLACK;
        /*
         * get the castling rights from the FEN string and set the castling bits in
         * STATUS
         */
        int castling = Fen.getCastling(fen);
        board[STATUS] ^= castling << CASTLING_SHIFT;
        /*
         * get the en passant square from the FEN string and store the en passant square
         * in STATUS. if there is no valid en passant square, set the en passant square
         * to Value.INVALID
         * an invalid en passant square will be stored as a value of 0 in the
         * appropriate STATUS bits
         */
        int eSquare = Fen.getEnPassantSquare(fen);
        boolean eSquareIsValid = (whiteToMove && (eSquare > 39 && eSquare < 48)) | (!whiteToMove && (eSquare > 15 && eSquare < 24));
        board[STATUS] ^= eSquareIsValid ? eSquare << ESQUARE_SHIFT : 0L;
        eSquare = eSquareIsValid ? eSquare : Value.INVALID;
        /*
         * get the half move clock and full move number from the FEN string and set their
         * bits in STATUS
         */
        board[STATUS] ^= Fen.getHalfMoveClock(fen) << HALF_MOVE_CLOCK_SHIFT;
        board[STATUS] ^= Fen.getFullMoveNumber(fen) << FULL_MOVE_NUMBER_SHIFT;
        /*
         * get the Zobrist key and set it in KEY
         */
        board[KEY] = Zobrist.getKey(pieces, whiteToMove, (castling & Value.KINGSIDE_BIT[Value.WHITE]) != 0,
            (castling & Value.QUEENSIDE_BIT[Value.WHITE]) != 0, (castling & Value.KINGSIDE_BIT[Value.BLACK]) != 0,
            (castling & Value.QUEENSIDE_BIT[Value.BLACK]) != 0, eSquare);
        /*
         * check FEN string is valid by testing whether non-moving player's king can be captured
         * if it can be captured, FEN string is invalid so return invalid board (long array of size 1)
         */
        long[] moveList = Gen.gen(board, true, false);
        for(int i = 0; i < moveList[Gen.MOVELIST_SIZE]; i ++) {
            if((moveList[i] >>> TARGET_PIECE_SHIFT & Piece.TYPE) == Piece.KING) return new long[1]; 
        }
        return board;
    }

    public static final int PLAYER_SHIFT = 3;
    public static final int START_PIECE_SHIFT = 16;
    public static final int PIECE_BITS = 0b1111;
    public static final int TARGET_SQUARE_SHIFT = 6;
    public static final int TARGET_PIECE_SHIFT = 20;
    public static final int PROMOTE_PIECE_SHIFT = 12;
    public static final int SQUARE_A1 = 0;
    public static final int SQUARE_A8 = 56;
    public static final int SQUARE_H1 = 7;
    public static final int SQUARE_H8 = 63;
    public static final int WHITE_CASTLING_BITS = WHITE_KINGSIDE_BITS | WHITE_QUEENSIDE_BITS;
    public static final int BLACK_CASTLING_BITS = BLACK_KINGSIDE_BITS | BLACK_QUEENSIDE_BITS;

    /**
     * This is a factory method which returns a new board array representing the
     * position after making a move
     * 
     * @param board the board array
     * @param move  the move to make
     * @return a new board array representing the position after making the move
     */
    public static long[] makeMove(long[] board, long move) {
        /*
         * create a new board array and create copies of the board's bitboards, castling
         * rights, en passant square, half move clock, full move number, and Zobrist key
         * as these may be modified by the move
         */
        long[] newBoard = new long[MAX_BITBOARDS];
        System.arraycopy(board, 0, newBoard, 0, board.length);
        int castling = (int) newBoard[STATUS] >>> CASTLING_SHIFT & CASTLING_BITS;
        int eSquare = enPassantSquare(newBoard);
        int originalESquare = eSquare;
        int halfMoveClock = (int) newBoard[STATUS] >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
        int fullMoveNumber = (int) newBoard[STATUS] >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
        long key = newBoard[KEY];
        /*
         * get piece information from the move
         */
        int startSquare = (int) move & SQUARE_BITS;
        int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        int startPieceType = startPiece & Piece.TYPE;
        int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        long targetSquareBit = 1L << targetSquare;
        /*
         * get the player to move from STATUS
         */
        int player = (int) newBoard[STATUS] & PLAYER_BIT;
        /*
         * reset the en passant square if it is set
         */
        if(eSquare != Value.INVALID) {
            key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
            eSquare = Value.INVALID;
        }
        /*
         * if the target square is not empty, this is a capture so reset the half move
         * clock and update the target piece bitboard and other player occupancy
         * bitboard for the target square, and update the zobrist key
         */
        if(targetPiece != Value.NONE) {
            halfMoveClock = 0;
            int other = 1 ^ player;
            newBoard[targetPiece] ^= targetSquareBit;
            newBoard[other << PLAYER_SHIFT] ^= targetSquareBit;
            key ^= Zobrist.PIECE[targetPiece & Piece.TYPE][other][targetSquare];
        }
        /*
         * perform the move based on the piece type
         */
        switch (startPieceType) {
            /*
             * Queens, bishops and knights have the same move logic, so they are grouped
             * together
             */
            case Piece.QUEEN:
            case Piece.BISHOP:
            case Piece.KNIGHT: {
                /*
                 * translate the target square into it's bitboard version and get a bitboard
                 * containing the bits of the start square and target square
                 */
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                /*
                 * update the piece bitboard and player occupancy bitboard for the start square
                 * and target square by flipping their bits, then update the zobrist key
                 */
                newBoard[startPiece] ^= pieceMoveBits;
                newBoard[player << PLAYER_SHIFT] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                    ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                break;
            }
            /*
             * Handle king moves
             */
            case Piece.KING: {
                /*
                 * get the player bit used in calculations later
                 */
                int playerBit = player << PLAYER_SHIFT;
                /*
                 * handle moving the piece as above
                 */
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                newBoard[startPiece] ^= pieceMoveBits;
                newBoard[playerBit] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.KING][player][startSquare]
                    ^  Zobrist.PIECE[Piece.KING][player][targetSquare];
                /*
                 * check if castling is possible and if it is, turn off the castling bits for
                 * that player and update the zobrist key
                 */
                boolean playerKingSideCastling  = (castling & (player == Value.WHITE ? WHITE_KINGSIDE_BITS : BLACK_KINGSIDE_BITS))  != 0;
                boolean playerQueenSideCastling = (castling & (player == Value.WHITE ? WHITE_QUEENSIDE_BITS : BLACK_QUEENSIDE_BITS)) != 0;
                if(playerKingSideCastling || playerQueenSideCastling) {
                    key ^= (playerKingSideCastling  ? Zobrist.KING_SIDE[player]  : 0)
                        ^  (playerQueenSideCastling ? Zobrist.QUEEN_SIDE[player] : 0);
                    castling &= ~(player == Value.WHITE ? WHITE_CASTLING_BITS : BLACK_CASTLING_BITS);
                }
                /*
                 * if the king moves 2 squares horizontally, this is a castling move, so update
                 * the rook bitboard and player occupancy bitboard for the rook's start and
                 * target squares, then update the zobrist key
                 */
                if(Math.abs(startSquare - targetSquare) == 2) {
                    if((targetSquare & Value.FILE) == Value.FILE_G) {
                        long rookMoveBits = (1L << (targetSquare + 1)) | (1L << (targetSquare - 1));
                        newBoard[Piece.ROOK | playerBit] ^= rookMoveBits;
                        newBoard[playerBit] ^= rookMoveBits;
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare - 1];
                    } else {
                        long rookMoveBits = (1L << (targetSquare - 2)) | (1L << (targetSquare + 1));
                        newBoard[Piece.ROOK | playerBit] ^= rookMoveBits;
                        newBoard[playerBit] ^= rookMoveBits;
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare - 2]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1];
                    }
                }
                break;
            }
            /*
             * Handle rook moves
             */
            case Piece.ROOK: {
                /*
                 * handle moving the piece as above
                 */
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                newBoard[startPiece] ^= pieceMoveBits;
                newBoard[player << 3] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.ROOK][player][startSquare]
                    ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare];
                /*
                 * if castling on the rook's side is available, reset it, then update the
                 * castling bits and the zobrist key
                 */
                if((castling & Value.KINGSIDE_BIT[player]) != Value.NONE) {
                    if(startSquare == (player == Value.WHITE ? SQUARE_H1 : SQUARE_H8)) {
                        castling ^= Value.KINGSIDE_BIT[player];
                        key ^= Zobrist.KING_SIDE[player];
                    }
                }
                if((castling & Value.QUEENSIDE_BIT[player]) != Value.NONE) {
                    if(startSquare == (player == Value.WHITE ? SQUARE_A1 : SQUARE_A8)) {
                        castling ^= Value.QUEENSIDE_BIT[player];
                        key ^= Zobrist.QUEEN_SIDE[player];
                    }
                }
                break;
            }
            /*
             * Handle pawn moves
             */
            case Piece.PAWN: {
                /*
                 * get the promotion piece from the move, if there is no promotion piece, this
                 * is equal to 0
                 */
                int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
                /*
                 * get the player bit for calculations later
                 */
                int playerBit = player << PLAYER_SHIFT;
                /*
                 * pawn moves reset the half move counter
                 */
                halfMoveClock = 0;
                /*
                 * if there is no promotion piece, perform a normal pawn move
                 */
                if(promotePiece == Value.NONE) {
                    long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                    newBoard[startPiece] ^= pieceMoveBits;
                    newBoard[playerBit] ^= pieceMoveBits;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                } else {
                    /*
                     * A promotion piece exists, so update the start piece bitboard for the start
                     * square, the promotion piece bitboard for the target square, and the player
                     * occupancy bitboard, then update the zobrist key
                     */
                    long startSquareBit = 1L << startSquare;
                    newBoard[startPiece] ^= startSquareBit;
                    newBoard[promotePiece] ^= targetSquareBit;
                    newBoard[playerBit] ^= startSquareBit | targetSquareBit;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[promotePiece & Piece.TYPE][player][targetSquare];
                }
                /*
                 * perform an en passant capture. the difference between this capture and a
                 * normal capture is that the captured pawn is on a different square to the
                 * target square
                 */
                if(targetSquare == originalESquare) {
                    int other = 1 ^ player;
                    int otherBit = other << PLAYER_SHIFT;
                    int captureSquare = targetSquare + (player == Value.WHITE ? -8 : 8);
                    long captureSquareBit = 1L << captureSquare;
                    newBoard[Piece.PAWN | otherBit] ^= captureSquareBit;
                    newBoard[otherBit] ^= captureSquareBit;
                    key ^= Zobrist.PIECE[Piece.PAWN][other][captureSquare];
                }
                /*
                 * if the pawn advances 2 squares, set a new en passant square and update the
                 * zobrist key
                 */
                if(Math.abs(startSquare - targetSquare) == 16) {
                    eSquare = startSquare + (player == Value.WHITE ? 8 : -8);
                    key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
                }
                break;
            }
            default:
                break;
        }
        /*
         * if this move captures a rook, reset the appropriate castling rights and
         * update the zobrist key
         */
        if((targetPiece & Piece.TYPE) == Piece.ROOK) {
            int other = 1 ^ player;
            if((castling & (other == Value.WHITE ? WHITE_KINGSIDE_BITS : BLACK_KINGSIDE_BITS)) != Value.NONE) {
                if(targetSquare == (other == Value.WHITE ? SQUARE_H1 : SQUARE_H8)) {
                    castling ^= (other == Value.WHITE ? WHITE_KINGSIDE_BITS : BLACK_KINGSIDE_BITS);
                    key ^= Zobrist.KING_SIDE[other];
                }
            }
            if((castling & (other == Value.WHITE ? WHITE_QUEENSIDE_BITS : BLACK_QUEENSIDE_BITS)) != Value.NONE) {
                if(targetSquare == (other == Value.WHITE ? SQUARE_A1 : SQUARE_A8)) {
                    castling ^= (other == Value.WHITE ? WHITE_QUEENSIDE_BITS : BLACK_QUEENSIDE_BITS);
                    key ^= Zobrist.QUEEN_SIDE[other];
                }
            }
        }
        /*
         * create the new boards STATUS bits and set its KEY, then return the new board
         * array
         */
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT)
                         | (eSquare != Value.INVALID ? (eSquare << ESQUARE_SHIFT) : 0)
                         | (halfMoveClock << HALF_MOVE_CLOCK_SHIFT)
                         | ((fullMoveNumber + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key;
        return newBoard;
    }

    public static final long ENPASSANT_RESET_BITS = ~(SQUARE_BITS << ESQUARE_SHIFT); 

    /**
     * This method makes a null move on the board, and returns the new board array.
     * This shouldn't be used twice in a row and not while the current player is in
     * check
     * 
     * @param board the board array
     * @return the new board array
     */
    public static void nullMove(long[] board) {
        /*
         * a null move is where a player makes a second move in a row without the
         * opponent making a move
         * to set this up, we are passed a board where normally a player would make the
         * next move but we flip the player bit and reset the en passant square since an
         * en passant move is not possible in a null move
         */
        board[STATUS] = (board[STATUS] ^ PLAYER_BIT) & ENPASSANT_RESET_BITS;
    }

    /**
     * This method returns the contents of a square on the board
     * 
     * @param board  the board array
     * @param square the square to get the contents of
     * @return the contents of the square, an empty square returns Value.NONE
     */
    public static int getSquare(long[] board, int square) {
        /*
         * get the bitboard representation of the square
         */
        long squareBit = 1L << square;
        /*
         * check if the white occupancy bitboard has the square set, if it does, check
         * which piece is on the square and return it
         */
        if((board[Value.WHITE_BIT] & squareBit) != 0L) {
            if((board[Piece.WHITE_PAWN] & squareBit) != 0L)   return Piece.WHITE_PAWN;
            if((board[Piece.WHITE_KNIGHT] & squareBit) != 0L) return Piece.WHITE_KNIGHT;
            if((board[Piece.WHITE_BISHOP] & squareBit) != 0L) return Piece.WHITE_BISHOP;
            if((board[Piece.WHITE_ROOK] & squareBit) != 0L)   return Piece.WHITE_ROOK;
            if((board[Piece.WHITE_QUEEN] & squareBit) != 0L)  return Piece.WHITE_QUEEN;
                                                              return Piece.WHITE_KING;
        }
        /*
         * check if the black occupancy bitboard has the square set, if it does, check
         * which piece is on the square and return it
         */
        if((board[Value.BLACK_BIT] & squareBit) == 0L)    return Value.NONE;
        if((board[Piece.BLACK_PAWN] & squareBit) != 0L)   return Piece.BLACK_PAWN;
        if((board[Piece.BLACK_KNIGHT] & squareBit) != 0L) return Piece.BLACK_KNIGHT;
        if((board[Piece.BLACK_BISHOP] & squareBit) != 0L) return Piece.BLACK_BISHOP;
        if((board[Piece.BLACK_ROOK] & squareBit) != 0L)   return Piece.BLACK_ROOK;
        if((board[Piece.BLACK_QUEEN] & squareBit) != 0L)  return Piece.BLACK_QUEEN;
                                                          return Piece.BLACK_KING;
    }

    /**
     * check if a square is attacked by a certain player
     * 
     * @param board  the board array
     * @param square the square to check
     * @param player the player to check is attacking the square
     * @return true if the square is attacked by the player, false otherwise
     */
    public static boolean isSquareAttackedByPlayer(long[] board, int square, int player) {
        int playerBit = player << 3;
        /*
         * check whether a player's knight is attacking this square, if so return true
         */
        if((BB[LEAP_ATTACKS][square] & board[Piece.KNIGHT | playerBit]) != 0L) return true;
        /*
         * check whether a player's king is attacking this square, if so return true
         */
        if((BB[KING_ATTACKS][square] & board[Piece.KING | playerBit]) != 0L) return true;
        /*
         * check whether a player's pawn is attacking this square, if so return true
         */
        if((BB[PAWN_ATTACKS_PLAYER1 - player][square] & board[Piece.PAWN | playerBit]) != 0L) return true;
        /*
         * get the full occupancy of the board for Magic bitboard calculations
         */
        long allOccupancy = board[Value.WHITE_BIT] | board[Value.BLACK_BIT];
        /*
         * check whether a player's bishop or queen is attacking this square on a
         * diagonal, if so return true
         */
        if((Magic.bishopMoves(square, allOccupancy) & (board[Piece.BISHOP | playerBit]
           | board[Piece.QUEEN | playerBit])) != 0L) return true;
        /*
         * check whether a player's rook or queen is attacking this square on a file or
         * rank, if so return true
         */
        if((Magic.rookMoves(square, allOccupancy) & (board[Piece.ROOK | playerBit]
           | board[Piece.QUEEN | playerBit])) != 0L) return true;
        /*
         * no piece is attacking this square, return false
         */
        return false;
    }

    /**
     * check if a player is in check
     * 
     * @param board  the board array
     * @param player the player to check
     * @return true if the player is in check, false otherwise
     */
    public static boolean isPlayerInCheck(long[] board, int player) {
        return isSquareAttackedByPlayer(board, Long.numberOfTrailingZeros(board[Piece.KING | (player << 3)]),
               1 ^ player);
    }

    /**
     * Output a text representation of the board to the console
     * 
     * @param board the board array
     */
    public static void drawText(long[] board) {
        System.out.println(boardString(board));
    }

    /**
     * Create a string representation of the board
     * 
     * @param board the board array
     * @return a string representation of the board
     */
    public static String boardString(long[] board) {
        String boardString = "";
        int square;
        int piece;
        for(int i = SQUARE_A1; i <= SQUARE_H8; i ++) {
            square = i ^ 0x38;
            piece = getSquare(board, square);
            boardString += (piece != Value.NONE ? Piece.SHORT_STRING[piece] : ".") + ((i & 7) == 7 ? "\n" : " ");
        }
        return boardString;
    }

    /**
     * convert a square index to a string representation
     * 
     * @param square the square index
     * @return a string representation of the square
     */
    public static String squareToString(int square) {
        return Value.FILE_STRING.charAt(square & Value.FILE) + Integer.toString((square >>> 3) + 1);
    }

    /**
     * create a FEN string representation from a board array
     * 
     * @param board the board array
     * @return the FEN string representation of the board array
     */
    public static String toFenString(long[] board) {
        /**
         * initialise a FEN string and loop over the board to get the piece placement
         */
        StringBuilder fen = new StringBuilder();
        int empty;
        int square;
        int piece;
        for(int rank = 7; rank >= 0; rank --) {
            empty = 0;
            for(int file = 0; file < 8; file ++) {
                square = rank << 3 | file;
                piece = getSquare(board, square);
                if(piece != Value.NONE) {
                    if(empty > 0) {
                        fen.append(empty);
                        empty = 0;
                    }
                    fen.append(Piece.SHORT_STRING[piece]);
                } else {
                    empty ++;
                }
            }
            if(empty > 0) {
                fen.append(empty);
            }
            if(rank > 0) {
                fen.append('/');
            }
        }
        /*
         * determine the player to move
         */
        fen.append(" " + (player(board) == Value.WHITE ? "w " : "b "));
        /*
         * get the castling rights
         */

        if(((int) board[STATUS] >>> CASTLING_SHIFT & CASTLING_BITS) == 0) {
            fen.append("- ");
        } else {
            fen.append((kingSide(board, Value.WHITE) ? "K" : "")
                    + (queenSide(board, Value.WHITE) ? "Q" : "")
                    + (kingSide(board, Value.BLACK) ? "k" : "")
                    + (queenSide(board, Value.BLACK) ? "q" : "")
                    + " ");
        }
        /*
         * get a valid en passant square
         */
        if (hasValidEnPassantSquare(board)) {
            fen.append(squareToString(enPassantSquare(board)) + " ");
        } else {
            fen.append("- ");
        }
        /*
         * add the half move clock and full move number to the FEN string
         */
        fen.append(Integer.toString(halfMoveClock(board)) + " " + Integer.toString(fullMoveNumber(board)));
        return fen.toString();
    }

    /**
     * Return the number of one type of piece on the board
     * 
     * @param board the board array
     * @param piece the piece int
     * @return the number of that piece on the board
     */
    public static int countPiece(long[] board, int piece) {
        return Long.bitCount(board[piece]);
    }

    /**
     * Return the number of all non-pawn pieces for a player
     * 
     * @param board the board array
     * @param player the player int
     * @return the number of all non-pawn pieces for a player
     */
    public static int countMaterialPieces(long[] board, int player) {
        int playerBit = player << 3;
        return Long.bitCount(board[Piece.QUEEN | playerBit]) + Long.bitCount(board[Piece.ROOK | playerBit]) + Long.bitCount(board[Piece.BISHOP | playerBit]) + Long.bitCount(board[Piece.KNIGHT | playerBit]);
    }

    /**
     * Return the material value of a player for all pieces and pawns
     * 
     * @param board The board Array
     * @param player The player
     * @return The player's material value
     */
    public static int materialValue(long[] board, int player) {
        int playerBit = player << PLAYER_SHIFT;
        return Long.bitCount(board[Piece.QUEEN | playerBit])  * Piece.VALUE[Piece.QUEEN]
        +      Long.bitCount(board[Piece.ROOK | playerBit])   * Piece.VALUE[Piece.ROOK]
        +      Long.bitCount(board[Piece.BISHOP | playerBit]) * Piece.VALUE[Piece.BISHOP]
        +      Long.bitCount(board[Piece.KNIGHT | playerBit]) * Piece.VALUE[Piece.KNIGHT]
        +      Long.bitCount(board[Piece.PAWN | playerBit])   * Piece.VALUE[Piece.PAWN];
    }

    /**
     * Return the material value of a player for all pieces only
     * 
     * @param board The board Array
     * @param player The player
     * @return The player's material value
     */
    public static int materialValuePieces(long[] board, int player) {
        int playerBit = player << PLAYER_SHIFT;
        return Long.bitCount(board[Piece.QUEEN | playerBit])  * Piece.VALUE[Piece.QUEEN]
        +      Long.bitCount(board[Piece.ROOK | playerBit])   * Piece.VALUE[Piece.ROOK]
        +      Long.bitCount(board[Piece.BISHOP | playerBit]) * Piece.VALUE[Piece.BISHOP]
        +      Long.bitCount(board[Piece.KNIGHT | playerBit]) * Piece.VALUE[Piece.KNIGHT];
    }

    /*
     * This is a utility class and should not be instantiated
     */
    private Board() {}


}
