package minchessv0.move;

import static minchessv0.board.Board.*;
import static minchessv0.util.Value.*;

import minchessv0.gen.Gen;

import static minchessv0.util.Piece.*;
import static minchessv0.util.B.BB;
import static minchessv0.gen.Gen.*;

import minchessv0.util.B;
import minchessv0.util.Magic;
import minchessv0.util.Piece;
import minchessv0.util.Value;

public class Move {
    
    /**
     * convert a move to a string representation in algebraic notation
     * 
     * @param move the move
     * @return a string representation of the move in algebraic notation
     */
    public static String string(long move) {
        /**
         * get the promotion piece from the move
         */
        int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
        return squareToString((int) move & SQUARE_BITS) + squareToString((int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS)
                + (promotePiece == NONE ? "" : SHORT_STRING[promotePiece].toUpperCase());
    }

    /**
     * convert a move to a string representation in standard notation
     * 
     * @param board the board array
     * @param move  the move
     * @return a string representation of the move in standard notation
     */
    public static String notation(long[] board, long move) {
        /*
         * get the start square and target square information from the move
         */
        int startSquare = (int) move & SQUARE_BITS;
        int startFile = startSquare & Value.FILE;
        int startRank = startSquare >>> 3;
        int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        int targetFile = targetSquare & Value.FILE;
        int targetRank = targetSquare >>> 3;
        int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        long pieceBitboard = board[startPiece];
        int startType = startPiece & TYPE;
        int player = startPiece >>> 3;
        int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
        /**
         * get the occupancy of all squares on the board for use in magic bitboard
         * calculations
         */
        long allOccupancy = board[WHITE_BIT] | board[BLACK_BIT];
        /**
         * set the notation string to an empty string
         */
        String notation = "";
        /*
         * handle the moving piece type
         */
        switch (startType) {
            case KING: {
                /*
                 * if this is a castling move, return the castling notation
                 */
                if (Math.abs(startSquare - targetSquare) == 2) {
                    return "O-O" + (targetFile == FILE_G ? "" : "-O");
                }
                notation = "K";
                break;
            }
            case QUEEN: {
                notation = "Q";
                /*
                 * if there is more than one queen that can move to the target square, add the
                 * file or rank of the start square to the notation
                 * use magic bitboard calculations to determine if there is more than one queen
                 * that can move to the target square
                 */
                long queensAttackTargetSquare = Magic.queenMoves(targetSquare, allOccupancy) & pieceBitboard;
                /*
                 * if there is more than one queen that can move to the target square, add the
                 * file or rank (or both) of the start square to the notation
                 */
                if (queensAttackTargetSquare > 1L) {
                    int queensOnFile = Long.bitCount(queensAttackTargetSquare & BB[B.FILE][targetFile]);
                    int queensOnRank = Long.bitCount(queensAttackTargetSquare & BB[B.RANK][targetRank]);
                    int queensOnDiagonals = Long
                            .bitCount(queensAttackTargetSquare & (B.BB[B.DIAGONAL_ATTACKS][targetSquare]));
                    /*
                     * if the additional queens are on the same rank as the start square, add the
                     * file of the start square to the notation
                     */
                    if (queensOnRank > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    /*
                     * if the additional queens are on the same file as the start square, add the
                     * rank of the start square to the notation
                     */
                    if (queensOnFile > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                    /*
                     * if the additional queens are on the same diagonal as the start square,
                     * and a rank or file has not already been added to the notation string (i.e.
                     * the notation string is only "Q" at this point),
                     * add the file of the start square to the notation
                     */
                    if (notation.length() == 1 && queensOnDiagonals > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                }
                break;
            }
            case ROOK: {
                notation = "R";
                /*
                 * follow a similar method to the queen notation above, but only for files and
                 * ranks
                 */
                long rooksAttackTargetSquare = Magic.rookMoves(targetSquare, allOccupancy) & pieceBitboard;
                if (rooksAttackTargetSquare > 1L) {
                    int rooksOnFile = Long.bitCount(rooksAttackTargetSquare & B.BB[B.FILE][targetFile]);
                    int rooksOnRank = Long.bitCount(rooksAttackTargetSquare & B.BB[B.RANK][targetRank]);
                    if (rooksOnRank > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (rooksOnFile > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                }
                break;
            }
            case BISHOP: {
                notation = "B";
                /*
                 * follow a similar method to the queen notation above, but only for diagonals
                 */
                long bishopsAttackTargetSquare = Magic.bishopMoves(targetSquare, allOccupancy) & pieceBitboard;
                if (bishopsAttackTargetSquare > 1L) {
                    int bishopsOnFile = Long.bitCount(bishopsAttackTargetSquare & B.BB[B.FILE][targetFile]);
                    int bishopsOnRank = Long.bitCount(bishopsAttackTargetSquare & B.BB[B.RANK][targetRank]);
                    int bishopsOnDiagonals = Long
                            .bitCount(bishopsAttackTargetSquare & (B.BB[B.DIAGONAL_ATTACKS][targetSquare]));
                    if (bishopsOnRank > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (bishopsOnFile > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                    if (notation.length() == 1 && bishopsOnDiagonals > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                }
                break;
            }
            case KNIGHT: {
                notation = "N";
                /*
                 * if there is more than one knight that can move to the target square, add the
                 * file or rank of the start square to the notation
                 */
                if (Long.bitCount(B.BB[B.LEAP_ATTACKS][targetSquare] & pieceBitboard) > 1) {
                    if (Long.bitCount(B.BB[B.RANK][startRank] & pieceBitboard) > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (Long.bitCount(B.BB[B.FILE][startFile] & pieceBitboard) > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                }
                break;
            }
            /*
             * if the piece is a pawn, we don't need to add any additional notation for the
             * start square
             */
            case PAWN:
            default: {
                notation = "";
                break;
            }
        }
        /*
         * if there is a piece on the target square or the move is an en passant move
         * (i.e. the start piece is a pawn and the target square is the en passant
         * square)
         * add an "x" to the notation
         */
        if (targetPiece != Value.NONE || targetSquare == enPassantSquare(board)) {
            if (startType == PAWN) {
                notation += Value.FILE_STRING.charAt(startFile);
            }
            notation += "x";
        }
        /*
         * add the target square to the notation
         */
        notation += squareToString(targetSquare);
        /*
         * if there is a promotion piece, add the promotion piece to the notation
         */
        if (promotePiece != Value.NONE) {
            notation += "=";
            switch (promotePiece & TYPE) {
                case QUEEN:
                    notation += "Q";
                    break;
                case ROOK:
                    notation += "R";
                    break;
                case BISHOP:
                    notation += "B";
                    break;
                case KNIGHT:
                    notation += "N";
                    break;
            }
        }
        /*
         * check to see whether the move checks the opposing king
         */
        long[] tempBoard = makeMove(board, move);
        if (isPlayerInCheck(tempBoard, 1 ^ player)) {
            /*
             * if the opposing king is in check, see if there are any legal moves for the
             * opposing player
             */
            long[] moves = gen(tempBoard, true, false);
            /*
             * if there are no legal moves, this move is a mating move, otherwise it is a
             * checking move
             */
            if (moves[MOVELIST_SIZE] == 0) {
                notation += "#";
            } else {
                notation += "+";
            }
        }
        return notation;
    }

    /**
     * Returns a string representation of a move in verbose notation for debugging
     * purposes
     * 
     * @param move the move to be converted to a string
     * @return a string representation of the move in verbose notation
     */
    public static String verbose(int move) {
        int startSquare = move & 0x3f;
        int targetSquare = (move >>> 6) & 0x3f;
        int promotePiece = (move >>> 12) & 0xf;
        int startPiece = (move >>> 16) & 0xf;
        int targetPiece = (move >>> 20) & 0xf;
        return Piece.SHORT_STRING[startPiece] + "[" + startSquare + "] " + Piece.SHORT_STRING[targetPiece] + "["
                + targetSquare + "] " + Piece.SHORT_STRING[promotePiece] + "[" + promotePiece + "]";
    }

    /**
     * This is a string used in the parseMove method
     */
    private static final String PIECE_STRING = " KQRBNPXXkqrbnp";

    /**
     * Convert an algebraic move string to a move integer
     * 
     * @param board      the board array
     * @param moveString the move string to be converted
     * @return the move integer
     */
    public static int stringToInt(long[] board, String moveString) {
        /*
         * check string is a valid move string
         */
        if(moveString.length() < 4) return Value.INVALID;
        int startFile = Value.FILE_STRING.indexOf(moveString.charAt(0));
        if(startFile == Value.INVALID) return Value.INVALID;
        int startRank = Character.getNumericValue(moveString.charAt(1)) - 1;
        if(startRank == Value.INVALID) return Value.INVALID;
        int targetFile = Value.FILE_STRING.indexOf(moveString.charAt(2));
        if(targetFile == Value.INVALID) return Value.INVALID;
        int targetRank = Character.getNumericValue(moveString.charAt(3)) - 1;
        if(targetRank == Value.INVALID) return Value.INVALID;
        /*
         * get the start square and target square from the move string
         */
        int startSquare = startRank << 3 | startFile;
        int targetSquare = targetRank << 3 | targetFile;
        int promotePiece = 0;
        /*
         * if the move string has a 5th character, it is a promotion move, so get the
         * promotion piece from the move string
         */
        if (moveString.length() > 4) {
            promotePiece = PIECE_STRING.indexOf(moveString.charAt(4));
            if(promotePiece == Value.INVALID) return Value.INVALID;
        }
        /*
         * create a move integer from the start square, target square, promotion piece,
         * start piece and target piece
         */
        return startSquare | (targetSquare << 6) | (promotePiece << 12) | (getSquare(board, startSquare) << 16)
                | (getSquare(board, targetSquare) << 20);
    }

    /**
     * check whether a move from one square to another exists in a list of moves
     * 
     * @param moves        the list of moves
     * @param startSquare  the start square of the move
     * @param targetSquare the target square of the move
     * @return the move
     */
    public static long isValid(long[] moves, int startSquare, int targetSquare) {
        int index = Value.INVALID;
        /*
         * loop over the moves in the list of moves and check if the start and target
         * squares match the start and target squares of the move
         */
        for (int move = 0; move < moves[Gen.MOVELIST_SIZE]; move ++) {
            if ((moves[move] & 0xfff) == convertStartTarget(startSquare, targetSquare)) {
                index = move;
                break;
            }
        }
        return moves[index];
    }

    /**
     * convert a start square and target square to a move integer
     * 
     * @param startSquare  the start square of the move
     * @param targetSquare the target square of the move
     * @return the move integer
     */
    public static int convertStartTarget(int startSquare, int targetSquare) {
        return startSquare | (targetSquare << 6);
    }

    public static String moveListString(long[] moveList) {
        String string = "";
        for(int i = 0; i < moveList[99]; i ++) {
            string += "Move " + (i + 1) + ": " + string(moveList[i]) + "\n";
        }
        return string;
    }

    private Move() {}

}
