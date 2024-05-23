package minchessv0.util;

public class TTable {

    public static class TTEntry {

        public TTEntry() {
            this.type = INVALID;
        }

        public TTEntry(long key, int eval, int depth, int type, long hashMove) {
            this.key = key;
            this.eval = eval;
            this.depth = depth;
            this.type = type;
            this.hashMove = hashMove;
        }

        public int eval() {
            return this.eval;
        }

        public int depth() {
            return this.depth;
        }

        public int type() {
            return this.type;
        }

        public long hashMove() {
            return this.hashMove;
        }

        private static final int INVALID = -1;
        private static final int EXACT = 0;
        private static final int ALPHA = 1;
        private static final int BETA = 2;

        private long key;
        private int eval;
        private int depth;
        private int type;
        private long hashMove;

    }

    public TTable() {
        this(DEFAULT_TABLE_SIZE_IN_MB);
    }

    public TTable(int sizeInMB) {
        long totalBytes = (long) sizeInMB * 1024 * 1024;
        int entries = (int) (totalBytes / ENTRY_SIZE_IN_BYTES);
        this.tableSize = calculateTableSize(entries);
        this.table = new TTEntry[this.tableSize];
    }

    public TTEntry probe(long key) {
        int index = (int) key & this.tableSize;
        TTEntry entry = this.table[index];
        return entry.key == key ? entry : new TTEntry();
    }

    public void save(long key, int eval, int depth, int type, long hashKey) {
        int index = (int) key & this.tableSize;
        TTEntry entry = this.table[index];
        if(entry == null || entry.depth <= depth) {
            this.table[index] = new TTEntry(key, eval, depth, type, hashKey);
        }
    }

    private static final int ENTRY_SIZE_IN_BYTES = 28;
    private static final int DEFAULT_TABLE_SIZE_IN_MB = 128;

    private int tableSize;
    private TTEntry[] table;

    private int calculateTableSize(int minEntries) {
        int n = 1;
        while((1 << n) - 1 < minEntries) n ++;
        return (1 << n) - 1;
    }  

}
