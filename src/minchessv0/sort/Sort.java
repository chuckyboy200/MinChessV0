package minchessv0.sort;

import minchessv0.board.Board;
import minchessv0.gen.Gen;
import minchessv0.util.Piece;

public class Sort {
    
    public static void sort(long[] array) {
        quickSort(array, 0, (int) array[Gen.MOVELIST_SIZE] - 1);
    }

    public static void sortNoEval(long[] board, long[] array) {
        if(array.length == 0) return;
        long move;
        long sortScore;
        int startType;
        int targetType;
        int promoteType;
        int targetSquare;
        int other = (int) ((8 ^ array[0]) & 8) >>> 3;
        int valueDifference;
        int moveListSize = (int) array[Gen.MOVELIST_SIZE];
        for(int i = 0; i < moveListSize; i ++) {
            move = array[i];
            sortScore = 0L;
            startType = (int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE;
            targetType = (int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE;
            promoteType = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Piece.TYPE;
            targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
            if(promoteType != Piece.EMPTY) {
                sortScore += PROMOTE_SCORE + Piece.VALUE[promoteType] + (targetType != Piece.EMPTY ? CAPTURE : 0);
            } else {
                if(targetType != Piece.EMPTY) {
                    valueDifference = Piece.VALUE[targetType] - Piece.VALUE[startType];
                    sortScore += valueDifference + (valueDifference > 50 ? HIGH + valueDifference : valueDifference > -50 ? LOW + valueDifference : !Board.isSquareAttackedByPlayer(board, targetSquare, other) ? LESS : 0);
                }
            }
            array[i] = (move & 0xffffffffL) | (sortScore << 32);
        }
        quickSort(array, 0, moveListSize - 1);
        for(int i = 0; i < moveListSize; i ++) {
            array[i] &= 0xffffffffL;
        }
    }

    private static final int PROMOTE_SCORE = 100000;
    private static final int HIGH = 80000;
    private static final int LOW = 60000;
    private static final int CAPTURE = 20000;
    private static final int LESS = 10000;

    private Sort() {}

    private static void quickSort(long[] array, int begin, int end) {
        if(begin < end) {
            int partitionIndex = partition(array, begin, end);
            quickSort(array, begin, partitionIndex - 1);
            quickSort(array, partitionIndex + 1, end);
        }
    }

    private static int partition(long[] array, int begin, int end) {
        long pivot = array[end];
        int i = begin - 1;
        long temp;
        for(int j = begin; j < end; j ++) {
            if(array[j] > pivot) {
                temp = array[++ i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        temp = array[++ i];
        array[i] = array[end];
        array[end] = temp;
        return i;
    }

}
