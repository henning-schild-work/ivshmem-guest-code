/* This class contains constants and "macros" for the FTP apps.
 * Memory is laid out as follows:
 * First chunk is for synchronization - 3 ints for each of 8 clients is only xxx bytes but we have memory to spare.
 * Next xxx chunks are 32 blocks of n chunks each.
 *   - The first chunk contains locks and full/empty counts.
 *   - The other chunks are used for data transfer.
 * The rest of the memory is unused.
 */

public abstract class FTP {
    /* Memory device */
    MemAccess mem;

    /* Chunks per block */
    protected final long MSIZE;
    protected final int NBLOCKS;
    protected final int NCHUNKS;
    protected final int CHUNK_SZ;
    protected final int BLOCK_SZ;
    
    /* Offsets for the synchronization memory */
    protected final int SLOCK = 0;
    protected final int BLK = 4;
    protected final int CLIENT = 8;

    /* Offsets for the block memory */
    protected final int LOCK = 0;
    protected final int FLOCK = 4;
    protected final int FULL = 8;
    protected final int ELOCK = 12;
    protected final int EMPTY = 16;
    protected final int SIZE = 20;
    protected final int FNAME = 28;

    public FTP(String devname, long msize, int nblocks, int nchunks) throws Exception {
        mem = new MemAccess(devname);

        MSIZE = msize;
        NBLOCKS = nblocks;
        NCHUNKS = nchunks;

        /* Convert mem size to MB */
        msize *= 1024*1024;
        /* Reserve space for sync data */
        msize -= 256;
        /* Calculate sizes */
        BLOCK_SZ = (int)(msize / nblocks);
        CHUNK_SZ = BLOCK_SZ / (nchunks + 1);

        System.out.println("[FTP] Initialized.");
        System.out.println("\tMemory: " + String.valueOf(MSIZE) + "MB");
        System.out.println("\tBlocks: " + String.valueOf(NBLOCKS) + " x " + String.valueOf(BLOCK_SZ) + "B");
        System.out.println("\tChunks: " + String.valueOf(NCHUNKS) + " x " + String.valueOf(CHUNK_SZ) + "B per block");
    }

    protected int NEXT(int i) {
        return (i + 1) % NCHUNKS;
    }

    /* The base address of block blk */
    protected int BASE(int blk) {
        return CHUNK_SZ + BLOCK_SZ * blk;
    }

    /* Offset based on the block and chunk numbers */
    protected int OFFSET(int blk, int i) {
        return CHUNK_SZ + BLOCK_SZ * blk + CHUNK_SZ * (i + 1);
    }

    /* The syncronization block for machine i */
    protected int SYNC(int i) {
        return 12*(i-1);
    }
}
