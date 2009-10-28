public abstract class FTP {
    protected static final int CHUNK_SZ = 16*1024*1024;
    protected static final int FLOCK = 0;
    protected static final int FULL = 4;
    protected static final int ELOCK = 8;
    protected static final int EMPTY = 12;
    
    protected static int NEXT(int i) {
        return (i + 1) % 15;
    }

    protected static int OFFSET(int i) {
        return (i + 1) * CHUNK_SZ;
    }
}
