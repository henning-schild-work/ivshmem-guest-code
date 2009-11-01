import java.io.FileOutputStream;

public class FTPRecv extends FTP {
    public static void main(String args[]) throws Exception {
        int sender;
        MemAccess mem;
        String recvfile;
        String devname;
        int full, empty;
        byte bytes[] = new byte[CHUNK_SZ];
        int idx;
        long total, recvd;

        devname = args[0];
        recvfile = args[1];
        sender = Integer.parseInt(args[2]);

        System.out.println("[RECV] Opening device " + devname);
        mem = new MemAccess(devname);
        FileOutputStream file = new FileOutputStream(recvfile);

        System.out.println("[RECV] Waiting for size from sender.");
        mem.waitEvent(sender);
        total = mem.readLong(OFFSET(0));
        System.out.println("[RECV] Got size from sender: " + String.valueOf(total));
        System.out.println("[RECV] TIME = " + System.currentTimeMillis());
        mem.waitEventIrq(sender);

        recvd = 0;
        for(idx = 0; recvd < total; idx = NEXT(idx)) {
            System.out.println("[RECV] Waiting for full slot.");
            do {
                Thread.sleep(50);
                full = mem.readInt(FULL);
            } while(full == 0); 
            while(mem.spinLock(FLOCK) != 0);
            full = full - 1;
            mem.writeInt(full, FULL);
            mem.spinUnlock(FLOCK);
            System.out.println("[RECV] Decremented full to " + String.valueOf(full));

            mem.readBytes(bytes, OFFSET(idx), CHUNK_SZ);
            file.write(bytes, 0, CHUNK_SZ);
            recvd += CHUNK_SZ;
            System.out.println("[RECV] Read bytes in slot " + String.valueOf(idx) + " recvd =  " + String.valueOf(recvd));

            while(mem.spinLock(ELOCK) != 0);
            empty = mem.readInt(EMPTY);
            empty = empty + 1;
            mem.writeInt(empty, EMPTY);
            mem.spinUnlock(ELOCK);
            System.out.println("[RECV] Incremented empty to " + String.valueOf(empty));
        }
       
        System.out.println("[RECV] Done, closing file and device.");
        file.getChannel().truncate(total);
        System.out.println("[RECV] TIME = " + System.currentTimeMillis());
        file.close();

        mem.closeDevice();
    }
}
