package minchessv0.eval;

import java.util.Arrays;

import minchessv0.board.Board;
import minchessv0.util.B;
import minchessv0.util.Magic;
import minchessv0.util.Piece;
import minchessv0.util.Value;

public class Eval {
    
    public static int eval(long[] board) {
        int eval = 0;
        long pieceBitboard;
        long bitboard;
        boolean perspective;
        int square;
        int thisPlayer = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        int thisPlayerBit = thisPlayer << 3;
        int playerBit;
        long allOccupancy = board[0] | board[8];
        long tacticalOccupancy;
        int kingSquare = Long.numberOfTrailingZeros(board[Piece.KING | thisPlayerBit]);
        int otherKingSquare = Long.numberOfTrailingZeros(board[Piece.KING | (1 ^ thisPlayerBit)]);
        int phase = Math.min(0, 24 - (Board.countPiece(board, Piece.QUEEN) + Board.countPiece(board, Piece.QUEEN | Value.BLACK_BIT)) * 4
                       - (Board.countPiece(board, Piece.ROOK) + Board.countPiece(board, Piece.ROOK | Value.BLACK_BIT)) * 2
                       - (Board.countPiece(board, Piece.BISHOP) + Board.countPiece(board, Piece.BISHOP | Value.BLACK_BIT))
                       - (Board.countPiece(board, Piece.KNIGHT) + Board.countPiece(board, Piece.KNIGHT | Value.BLACK_BIT)));
        for(int type = Piece.KING; type <= Piece.PAWN; type ++) {
            for(int player = 0; player < 2; player ++) {
                perspective = player == thisPlayer;
                playerBit = player << 3;
                tacticalOccupancy = ~board[playerBit];
                pieceBitboard = board[type | playerBit];
                for(bitboard = pieceBitboard; bitboard != 0L; bitboard &= bitboard - 1) {
                    square = Long.numberOfTrailingZeros(bitboard);
                    eval += perspective ? Psqt.BONUS[type][player][square][0] + Piece.VALUE[type] : -Psqt.BONUS[type][player][square][0] - Piece.VALUE[type];
                    switch(type) {
                        case Piece.KING: {
                            eval += perspective ? kingEval(square, player, board[Piece.ROOK | playerBit]) : -kingEval(square, player, board[Piece.ROOK | playerBit]);
                            break;
                        }
                        case Piece.QUEEN: {
                            eval += perspective ? queenEval(square, player, allOccupancy, tacticalOccupancy, otherKingSquare, pieceBitboard, board[Piece.BISHOP | playerBit], board[Piece.KNIGHT | playerBit]) : -queenEval(square, player, allOccupancy, tacticalOccupancy, kingSquare, pieceBitboard, board[Piece.BISHOP | playerBit], board[Piece.KNIGHT | playerBit]);
                            break;
                        }
                        case Piece.ROOK: {
                            eval += perspective ? rookEval(square, player, allOccupancy, tacticalOccupancy, otherKingSquare, pieceBitboard, board[Piece.PAWN | playerBit], board[Piece.PAWN | (8 ^ playerBit)]) : -rookEval(square, player, allOccupancy, tacticalOccupancy, otherKingSquare, pieceBitboard, board[Piece.PAWN | playerBit], board[Piece.PAWN | (8 ^ playerBit)]);
                            break;
                        }
                        case Piece.BISHOP: {
                            eval += perspective ? bishopEval(square, player, allOccupancy, tacticalOccupancy, otherKingSquare, pieceBitboard) : -bishopEval(square, player, allOccupancy, tacticalOccupancy, otherKingSquare, pieceBitboard);
                            break;
                        }
                        case Piece.KNIGHT: {
                            eval += perspective ? knightEval(square, player, tacticalOccupancy) : -knightEval(square, player, tacticalOccupancy);
                            break;
                        }
                        case Piece.PAWN: {
                            eval += perspective ? pawnEval(square, player, kingSquare, otherKingSquare, board[Piece.PAWN | playerBit], board[Piece.PAWN | (8 ^ playerBit)], board[Piece.KING | playerBit], Board.countMaterialPieces(board, 1 ^ player), player == thisPlayer, Board.materialValuePieces(board, 1 ^ player)) : -pawnEval(square, player, kingSquare, otherKingSquare, board[Piece.PAWN | playerBit], board[Piece.PAWN | (8 ^ playerBit)], board[Piece.KING | playerBit], Board.countMaterialPieces(board, 1 ^ player), player == thisPlayer, Board.materialValuePieces(board, 1 ^ player));
                            break;
                        }
                    }
                }
            }
        }
        return eval;
    }

    public static int see(long[] board, int startSquare, int targetSquare) {
        long[] seeBoard = Arrays.copyOf(board, board.length);
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

    private Eval() {}

    private static int kingEval2(long[] board) {
        int eval = 0;
        for(int player = Value.WHITE; player <= Value.BLACK; player ++) {
            int playerBit = player << 3;
            long bitboard = board[Piece.KING | playerBit];
            int square = Long.numberOfTrailingZeros(bitboard);
            int rank = square >>> 3;
            int file = square & 7;
        }
        return eval;
    }
    

    private static int kingEval(int square, int player, long rookBitboard) {
        int eval = 0;
        // king blocks rook
        int kingFile = square & 7;
        int kingRank = square >>> 3;
        // king on back rank evals
        // rook protects and king blocks rook
        // pawn shield and storm
        if(kingRank == (player == 0 ? 0 : 7)) {
            switch(kingFile) {
                case 0: {
                    if(((player == 0 ? 0x000000000000000eL : 0x0e00000000000000L) & rookBitboard) != 0L) eval += 30;
                    break;
                }
                case 1: {
                    if(((player == 0 ? 0x000000000000000cL : 0x0c00000000000000L) & rookBitboard) != 0L) eval += 30;
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval -= 30;
                    break;
                }
                case 2: {
                    if(((player == 0 ? 0x0000000000000008L : 0x0800000000000000L) & rookBitboard) != 0L) eval += 30;
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval -= 30;
                    break;
                }
                case 3: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval -= 30;
                    break;
                }
                case 4: {
                    break;
                }
                case 5: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) eval -= 30;
                    break;
                }
                case 6: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) eval -= 30;
                    if(((player == 0 ? 0x0000000000000020L : 0x2000000000000000L) & rookBitboard) != 0L) eval += 30;
                    break;
                }
                case 7: {
                    if((B.BB[B.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) eval -= 30;
                    if(((player == 0 ? 0x0000000000000060L : 0x6000000000000000L) & rookBitboard) != 0L) eval += 30;
                    break;
                }
                default: break;
            }
        }
        // king distance to other king when less than queen value material for both players and this king is stronger
        return eval;
    }

    private static int queenEval(int square, int player, long allOccupancy, long tacticalOccupancy, int otherKingSquare, long bitboard, long bishopBitboard, long knightBitboard) {
        int eval = 0;
        // mobility
        long queenAttacks = Magic.queenMoves(square, allOccupancy) & tacticalOccupancy;
        eval += Long.bitCount(queenAttacks) * 3;
        // early development
        if((bitboard & B.BB[B.QUEEN_START_POSITION_PLAYER0 + player][0]) == 0L && (bishopBitboard & B.BB[B.BISHOP_START_POSITION_PLAYER0 + player][0]) != 0L && (knightBitboard & B.BB[B.KNIGHT_START_POSITION_PLAYER0 + player][0]) != 0L) eval -= 30;
        // king safety
        eval += Long.bitCount(queenAttacks & B.BB[B.KING_RING_PLAYER1 - player][0]) * 5;
        // king distance
        int queenRank = square >>> 3;
        int queenFile = square & 7;
        int otherKingRank = otherKingSquare >>> 3;
        int otherKingFile = otherKingSquare & 7;
        eval += 16 / (Math.abs(queenRank - otherKingRank) + Math.abs(queenFile - otherKingFile)) * 5;
        return eval;
    }

    private static int rookEval(int square, int player, long allOccupancy, long tacticalOccupancy, int otherKingSquare, long bitboard, long pawnBitboard, long otherPawnBitboard) {
        int eval = 0;
        // mobility
        long rookAttacks = Magic.rookMoves(square, allOccupancy) & tacticalOccupancy;
        eval += Long.bitCount(rookAttacks) * 2;
        // connected rooks
        if((rookAttacks & bitboard) != 0L) eval += 30;
        // early development
        
        // king safety
        eval += Long.bitCount(rookAttacks & B.BB[B.KING_RING_PLAYER1 - player][0]) * 3;
        // king distance
        int rookRank = square >> 3;
        int rookFile = square & 7;
        int otherKingRank = otherKingSquare >>> 3;
        int otherKingFile = otherKingSquare & 7;
        eval += 16 / (Math.abs(rookRank - otherKingRank) + Math.abs(rookFile - otherKingFile)) * 3;
        // rook on open file
        if((B.BB[B.FILE][rookFile] & pawnBitboard) == 0L) eval += 15;
        if((B.BB[B.FILE][rookFile] & otherPawnBitboard) == 0L) eval += 15;
        // rook on relative 7th rank
        if(rookRank == (player == 0 ? 6 : 1)) eval += 30;
        return eval;
    }

    private static int bishopEval(int square, int player, long allOccupancy, long tacticalOccupancy, int otherKingSquare, long bitboard) {
        int eval = 0;
        // mobility;
        long bishopAttacks = Magic.bishopMoves(square, allOccupancy) & tacticalOccupancy;
        eval += bishopAttacks;
        // bishop pair
        if(Long.bitCount(bitboard) > 1) eval += 20;
        // bishop outpost
        // king safety
        eval += Long.bitCount(bishopAttacks & B.BB[B.KING_RING_PLAYER1 - player][0]) * 2;
        return eval;
    }

    private static int knightEval(int square, int player, long tacticalOccupancy) {
        int eval = 0;
        // mobility;
        long knightAttacks = B.BB[B.LEAP_ATTACKS][square] & tacticalOccupancy;
        eval += knightAttacks * 3;
        // knight outpost
        // king safety
        eval += Long.bitCount(knightAttacks & B.BB[B.KING_RING_PLAYER1 - player][0]) * 2;
        return eval;
    }
    
    private static int pawnEval(int square, int player, int kingSquare, int otherKingSquare, long bitboard, long otherPawnBitboard, long kingBitboard, int otherPieceCount, boolean playerToMove, int otherPieceMaterialValue) {
        int eval = 0;
        int other = 1 ^ player;
        int pawnRank = square >>> 3;
        int pawnFile = square & 7;
        // doubled pawns
        // pawn protects bishop or knight - same as outpost but may be faster to calculate
        // passed pawn
        if(((otherPawnBitboard & B.BB[B.FORWARD_RANKS_PLAYER0 + other][pawnRank]) & ((pawnFile > 0 ? B.BB[B.FILE][pawnFile - 1] : 0L) | (pawnFile < 7 ? B.BB[B.FILE][pawnFile + 1] : 0L))) == 0L) {
            // original eval subtracted normal pawn psqt bonus and then added back the passed pawn psqt bonus
			eval += Long.bitCount((B.BB[B.PAWN_ATTACKS_PLAYER0 + other][square] | B.BB[B.PAWN_ATTACKS_PLAYER0 + other][square + Piece.PAWN_ADVANCE[player] / 2]) & bitboard) * 14;
			int otherKingRank = otherKingSquare >>> 3;
            int otherKingFile = otherKingSquare & 7;
            if(otherPieceCount == 0 && (bitboard & B.BB[B.FORWARD_RANKS_PLAYER0 + player][pawnRank] & B.BB[B.FILE][pawnFile]) == 0L) {
				int pawnPromoteDist = Math.abs((player == 0 ? 7 : 0) - pawnRank) + (pawnRank == (player == 0 ? 1 : 6) ? 1 : 0);
				int otherKingDistFromPromote = Math.max(Math.abs((player == 0 ? 7 : 0) - otherKingRank), Math.abs(pawnFile - otherKingFile));
				int pawnTurnToMove = playerToMove ? 1 : 0;
				int kingTurnToMove = 1 ^ pawnTurnToMove;
				int ownKingInFront = (kingBitboard & B.BB[B.FORWARD_RANKS_PLAYER0 + player][pawnRank] & B.BB[B.FILE][pawnFile]) != 0L ? 1 : 0;
				int pawnDist = pawnPromoteDist - pawnTurnToMove + ownKingInFront;
				int kingDist = otherKingDistFromPromote - kingTurnToMove;
				//Gui.println("pawnDist " + pawnDist + " kingDist " + kingDist);
				if(kingDist > pawnDist) {
					eval += Piece.VALUE[Piece.BISHOP];
				}
			}
			if(otherPieceMaterialValue < Piece.VALUE[Piece.ROOK] + Piece.VALUE[Piece.KNIGHT]) {
                int kingRank = kingSquare >>> 3;
                int kingFile = kingSquare & 7;
				int kingDist = 8 - Math.max(Math.abs(kingRank - pawnRank), Math.abs(kingFile - pawnFile));
				int otherKingDist = Math.max(Math.abs(otherKingRank - pawnRank), Math.abs(otherKingFile - pawnFile));
				eval += (kingDist * kingDist + otherKingDist * otherKingDist) * (player == 0 ? pawnRank : 7 - pawnRank);
			}
		}
        return eval;
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
