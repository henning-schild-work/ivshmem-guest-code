public abstract class FTP {
    /* 14 512K chunks per block */
    protected static final int CHUNK_SZ = 512*1024;
    protected static final int NCHUNKS = 14;
    
    /* Maximum filename length is 4096 */
    protected static final int MAX_PATH = 4096;

    /* Offsets for the synchronization memory */
    protected static final int SLOCK = 0;
    protected static final int BLK = 4;
    protected static final int CLIENT = 8;

    /* Offsets for the block memory */
    protected static final int LOCK = 0;
    protected static final int FLOCK = 4;
    protected static final int FULL = 8;
    protected static final int ELOCK = 12;
    protected static final int EMPTY = 16;
    protected static final int SIZE = 20;
    protected static final int FNAME = 28;

    protected static int NEXT(int i) {
        return (i + 1) % NCHUNKS;
    }

    /* The base address of block blk */
    protected static int BASE(int blk) {
        return 384 + (blk * (NCHUNKS + 1) * CHUNK_SZ);
    }

    /* Offset based on the block and chunk numbers */
    protected static int OFFSET(int blk, int i) {
        return 384 + (blk * (NCHUNKS + 1) * CHUNK_SZ) + (i + 1) * CHUNK_SZ;
    }

    /* The syncronization block for machine i */
    protected static int SYNC(int i) {
        return 12*i;
    }
}
