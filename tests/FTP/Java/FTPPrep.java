public class FTPPrep extends FTP {
    public static void main(String args[]) throws Exception {
        MemAccess mem = new MemAccess(args[0]);
        for(int i = 1; i < 32; i++) {
            mem.initLock(SYNC(i) + SLOCK);
            mem.initLock(BASE(i - 1) + LOCK);
        }
    }
}
