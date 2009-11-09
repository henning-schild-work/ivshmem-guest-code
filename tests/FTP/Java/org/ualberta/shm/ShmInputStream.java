package org.ualberta.shm;

import java.io.InputStream;
import java.io.IOException;

public class ShmInputStream extends InputStream {
    private MemAccess _mem;
    private String _fname;
    private long _total;
    private long _recvd;
    private int _idx;
    private byte[] _buf;
    private int _mark;
    private int _block;

    /* Chunks per block */
    /* TODO: Make this configurable */
    private final long MSIZE = 256 * 1024 * 1024;
    private final int NBLOCKS = 5;
    private final int NCHUNKS = 5;
    private final int CHUNK_SZ;
    private final int BLOCK_SZ;
    /* Offsets for the synchronization memory */
    private final int SLOCK = 0;
    private final int BLK = 4;
    private final int CLIENT = 8;
    /* Offsets for the block memory */
    private final int LOCK = 0;
    private final int FLOCK = 4;
    private final int FULL = 8;
    private final int ELOCK = 12;
    private final int EMPTY = 16;
    private final int SIZE = 20;
    private final int FNAME = 28;

    public ShmInputStream(MemAccess m, String fname, int sender) {
        super();

        _mem = m;
        _fname = fname;

        /* Calculate sizes */
        BLOCK_SZ = (int)((MSIZE - 256) / NBLOCKS);
        CHUNK_SZ = BLOCK_SZ / (NCHUNKS + 1);

        /* Our buffering */
        _buf = new byte[CHUNK_SZ];

        int me = _mem.getPosition();

        /* Initiate connection */
        while(_mem.spinLock(SYNC(sender) + SLOCK) != 0);
        _mem.writeInt(me, SYNC(sender) + CLIENT);
        _mem.waitEventIrq(sender);
        _mem.waitEvent();

        /* Figure out which block we're using */
        _block = _mem.readInt(SYNC(sender) + BLK);
        /* Request the file */
        _mem.writeString(_fname, BASE(_block) + FNAME);
        _mem.waitEventIrq(sender);
        _mem.waitEvent();

        /* Read the file size */
        _total = _mem.readLong(BASE(_block) + SIZE);
        _mem.waitEventIrq(sender);

        _recvd = 0;
        _idx = 0;
        _mark = 0;

        _mem.spinUnlock(SYNC(sender) + SLOCK);
    }

    public void close() throws IOException {
        _mem.closeDevice();
    }

    private void fillBuffer() throws Exception {
        int full;
        int empty;

        do {
            Thread.sleep(50);
            full = _mem.readInt(BASE(_block) + FULL);
        } while(full == 0);

        while(_mem.spinLock(BASE(_block) + FLOCK) != 0);
        full = full - 1;
        _mem.writeInt(full, BASE(_block) + FULL);
        _mem.spinUnlock(BASE(_block) + FLOCK);

        _mem.readBytes(_buf, OFFSET(_block, _idx), CHUNK_SZ);

        while(_mem.spinLock(BASE(_block) + ELOCK) != 0);
        empty = _mem.readInt(BASE(_block) + EMPTY);
        empty = empty + 1;
        _mem.writeInt(empty, BASE(_block) + EMPTY);
        _mem.spinUnlock(BASE(_block) + ELOCK);

        _mark = 0;
        _idx = NEXT(_idx);
    }

    public int read() throws IOException {
        if(_recvd >= _total) {
            return -1;
        }

        if(_mark == CHUNK_SZ) {
            try {
                fillBuffer();
            } catch(Exception e) {
                throw new IOException("Error filling buffer.");
            }
        }

        _recvd += 1;

        return _buf[_mark++];
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        int tocopy;

        if(len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }

        if(len == 0) {
            return 0;
        }

        if(_mark == CHUNK_SZ) {
            try {
                fillBuffer();
            } catch(Exception e) {
                throw new IOException("Error filling buffer.");
            }
        }

        if(_total - _recvd < len) {
            tocopy = (int)(_total - _recvd);
        } else if(CHUNK_SZ - _mark < len) {
            tocopy = CHUNK_SZ - _mark;
        } else {
            tocopy = len;
        }

        System.arraycopy(_buf, _mark, b, 0, tocopy);
        _recvd += tocopy;
        _mark += tocopy;

        return tocopy;
    }

    public long skip(long s) throws IOException {
        long ret;

        if(s <= 0) {
            return 0;
        }

        if(CHUNK_SZ - _mark < s) {
            ret = CHUNK_SZ - _mark;
        } else if(_total - _recvd < s) {
            ret = _total - _recvd;
        } else {
            ret = s;
        }

        _mark += ret;
        _recvd += ret;
        
        return ret;
    }

    public int available() throws IOException {
        /* TODO: calculate FULL * CHUNK_SZ */
        if(_total - _recvd < CHUNK_SZ) {
            return (int)(_total - _recvd);
        } else {
            return CHUNK_SZ;
        }
    }

    public boolean markSupported() {
        return false;
    }

    public void reset() throws IOException {
        throw new IOException("Mark/reset not supported.");
    }
    
    private int NEXT(int i) {
        return (i + 1) % NCHUNKS;
    }

    /* The base address of block blk */
    private int BASE(int blk) {
        return CHUNK_SZ + BLOCK_SZ * blk;
    }

    /* Offset based on the block and chunk numbers */
    private int OFFSET(int blk, int i) {
        return CHUNK_SZ + BLOCK_SZ * blk + CHUNK_SZ * (i + 1);
    }

    /* The syncronization block for machine i */
    private int SYNC(int i) {
        return 12*(i-1);
    }
}
