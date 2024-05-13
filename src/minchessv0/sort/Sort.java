package minchessv0.sort;

import minchessv0.gen.Gen;

public class Sort {
    
    public static void sort(long[] array) {
        quickSort(array, 0, (int) array[Gen.MOVELIST_SIZE] - 1);
    }

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
