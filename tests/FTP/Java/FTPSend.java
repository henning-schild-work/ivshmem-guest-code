import java.io.FileInputStream;

public class FTPSend extends FTP {
    public static void main(String args[]) throws Exception {
        int receiver;
        MemAccess mem;
        String sendfile;
        String devname;
        int total, full, empty;
        byte bytes[] = new byte[CHUNK_SZ];
        int idx, sent;

        devname = args[0];
        sendfile = args[1];
        receiver = Integer.parseInt(args[2]);

        mem = new MemAccess(devname);
        FileInputStream file = new FileInputStream(sendfile);
        total = (int)file.getChannel().size();

        mem.initLock(FLOCK);
        mem.writeInt(FULL, 0);
        mem.initLock(ELOCK);
        mem.writeInt(EMPTY, 15);

        mem.writeInt(OFFSET(0), total);
        mem.waitEventIrq(receiver);
        mem.waitEvent(receiver);

        sent = 0;
        for(idx = 0; sent < total; idx = NEXT(idx)) {
            do {
                Thread.sleep(50);
                empty = mem.readInt(EMPTY);
            } while(EMPTY == 0); 
            while(mem.spinLock(ELOCK) != 0);
            empty = empty - 1;
            mem.writeInt(empty, FULL);
            mem.spinUnlock(ELOCK);

            file.read(bytes);
            mem.writeBytes(bytes, OFFSET(idx), CHUNK_SZ);
            sent += CHUNK_SZ;

            while(mem.spinLock(FLOCK) != 0);
            full = mem.readInt(FULL);
            full = full + 1;
            mem.spinUnlock(FLOCK);
        }
        
        file.close();
        mem.closeDevice();
    }
}
