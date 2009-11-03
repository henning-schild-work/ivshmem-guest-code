import java.io.FileInputStream;

public class FTPSend extends FTP {
    public static void main(String args[]) throws Exception {
        int receiver;
        int me;
        MemAccess mem;
        String devname;
        String sendfile;
        int full, empty;
        byte fname[] = new byte[MAX_PATH];
        byte bytes[] = new byte[CHUNK_SZ];
        int idx;
        long total, sent;
        boolean quit = false;

        devname = args[0];

        System.out.println("[SEND] Opening device " + devname);
        mem = new MemAccess(devname);

        /* What is my VM number? */
        me = mem.getPosition();

        /* For now, we will always use the same block */
        mem.initLock(SYNC(me) + LOCK);
        mem.writeInt(me, SYNC(me) + BLK);
       
        while(!quit) {

            /* Wait for a client */
            mem.waitEvent();
            /* Read the client's ID */
            receiver = mem.readInt(SYNC(me) + CLIENT);

            /* Initialize the block data */
            mem.spinLock(BASE(me) + LOCK);
            mem.initLock(BASE(me) + FLOCK);
            mem.writeInt(NCHUNKS, BASE(me) + FULL);
            mem.initLock(BASE(me) + ELOCK);
            mem.writeInt(0, BASE(me) + EMPTY);

            /* Already have the block number written, so irq the client immediately and wait for a filename */
            mem.waitEventIrq(receiver);
            mem.waitEvent();

            /* Read the filename */
            mem.readBytes(fname, BASE(me) + FNAME, MAX_PATH);
            sendfile = new String(fname);

            /* Open the file and get its size */
            FileInputStream file = new FileInputStream(sendfile);
            total = (long)file.getChannel().size();

            /* Send the size and wait for an ack */
            System.out.println("[SEND] Sending size to sender: " + String.valueOf(total));
            mem.writeLong(total, BASE(me) + SIZE);
            mem.waitEventIrq(receiver);
            System.out.println("[SEND] Waiting for ack");
            mem.waitEvent();

            /* Do the send! */
            sent = 0;
            for(idx = 0; sent < total; idx = NEXT(idx)) {
                System.out.println("[SEND] Waiting for empty slot");
                do {
                    Thread.sleep(50);
                    empty = mem.readInt(BASE(me) + EMPTY);
                } while(empty == 0);

                while(mem.spinLock(BASE(me) + ELOCK) != 0);
                empty = empty - 1;
                mem.writeInt(empty, BASE(me) + EMPTY);
                mem.spinUnlock(BASE(me) + ELOCK);
                System.out.println("[SEND] Decremented empty to " + String.valueOf(empty));

                file.read(bytes);
                mem.writeBytes(bytes, OFFSET(me, idx), CHUNK_SZ);
                sent += CHUNK_SZ;
                System.out.println("[SEND] Sent bytes in slot " + String.valueOf(idx) + " sent = " + String.valueOf(sent));

                while(mem.spinLock(BASE(me) + FLOCK) != 0);
                full = mem.readInt(BASE(me) + FULL);
                full = full + 1;
                mem.writeInt(full, BASE(me) + FULL);
                mem.spinUnlock(BASE(me) + FLOCK);
                System.out.println("[SEND] Incremented full to " + String.valueOf(full));
            }

            System.out.println("[SEND] Done, closing file.");
            file.close();
        }

        /* Unlock my memory */
        mem.spinUnlock(BASE(me) + LOCK);
        
        mem.closeDevice();
    }
}
