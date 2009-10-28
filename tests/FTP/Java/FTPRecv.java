import java.io.FileOutputStream;

public class FTPRecv extends FTP {
    public static void main(String args[]) throws Exception {
        int sender;
        MemAccess mem;
        String recvfile;
        String devname;
        int total, full, empty;
        byte bytes[] = new byte[CHUNK_SZ];
        int idx, recvd;

        devname = args[0];
        recvfile = args[1];
        sender = Integer.parseInt(args[2]);

        mem = new MemAccess(devname);
        FileOutputStream file = new FileOutputStream(recvfile);

        mem.waitEvent(sender);
        total = mem.readInt(0);
        mem.waitEventIrq(sender);

        recvd = 0;
        for(idx = 0; recvd < total; idx = NEXT(idx)) {
            do {
                Thread.sleep(50);
                full = mem.readInt(FULL);
            } while(full == 0); 
            while(mem.spinLock(FLOCK) != 0);
            full = full - 1;
            mem.writeInt(full, FULL);
            mem.spinUnlock(FLOCK);

            mem.readBytes(bytes, OFFSET(idx), CHUNK_SZ);
            file.write(bytes, 0, CHUNK_SZ);
            recvd += CHUNK_SZ;

            while(mem.spinLock(ELOCK) != 0);
            empty = mem.readInt(EMPTY);
            empty = empty + 1;
            mem.spinUnlock(ELOCK);
        }
        
        file.getChannel().truncate(total);
        file.close();

        mem.closeDevice();
    }
}
