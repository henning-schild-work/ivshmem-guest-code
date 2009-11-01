import java.io.FileInputStream;

public class FTPSend extends FTP {
    public static void main(String args[]) throws Exception {
        int receiver;
        MemAccess mem;
        String sendfile;
        String devname;
        int full, empty;
        byte bytes[] = new byte[CHUNK_SZ];
        int idx;
        long total, sent;

        devname = args[0];
        sendfile = args[1];
        receiver = Integer.parseInt(args[2]);

        System.out.println("[SEND] Opening device " + devname);
        mem = new MemAccess(devname);
        FileInputStream file = new FileInputStream(sendfile);
        total = (long)file.getChannel().size();

        mem.initLock(FLOCK);
        mem.writeInt(0, FULL);
        mem.initLock(ELOCK);
        mem.writeInt(15, EMPTY);

        System.out.println("[SEND] Sending size to sender: " + String.valueOf(total));
        mem.writeLong(total, OFFSET(0));
        mem.waitEventIrq(receiver);
        System.out.println("[SEND] Waiting for ack");
        mem.waitEvent(receiver);

        sent = 0;
        for(idx = 0; sent < total; idx = NEXT(idx)) {
            System.out.println("[SEND] Waiting for empty slot");
            do {
                Thread.sleep(50);
                empty = mem.readInt(EMPTY);
            } while(empty == 0); 
            while(mem.spinLock(ELOCK) != 0);
            empty = empty - 1;
            mem.writeInt(empty, EMPTY);
            mem.spinUnlock(ELOCK);
            System.out.println("[SEND] Decremented empty to " + String.valueOf(empty));

            file.read(bytes);
            mem.writeBytes(bytes, OFFSET(idx), CHUNK_SZ);
            sent += CHUNK_SZ;
            System.out.println("[SEND] Sent bytes in slot " + String.valueOf(idx) + " sent = " + String.valueOf(sent));

            while(mem.spinLock(FLOCK) != 0);
            full = mem.readInt(FULL);
            full = full + 1;
            mem.writeInt(full, FULL);
            mem.spinUnlock(FLOCK);
            System.out.println("[SEND] Incremented full to " + String.valueOf(full));
        }
        
        System.out.println("[SEND] Done, closing file and device.");
        file.close();
        mem.closeDevice();
    }
}
