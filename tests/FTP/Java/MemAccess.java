public class MemAccess {
    static {
        System.loadLibrary("MemAccess");
    }

    public MemAccess(String devname) throws Exception {
        if(openDevice(devname) != 0) {
            throw new Exception("Could not open device.");
        }
    }
    
    public native int openDevice(String devname);
    public native int closeDevice();
    public native int writeBytes(byte[] bytes, int offset, int cnt);
    public native int readBytes(byte[] bytes, int offset, int cnt);
    public native int writeInt(int towrite, int offset);
    public native int readInt(int offset);
    public native int writeLong(long towrite, int offset);
    public native long readLong(int offset);
    public native int writeString(String towrite, int offset);
    public native String readString(int offset);
    public native int initLock(int offset);
    public native int spinLock(int offset);
    public native int spinUnlock(int offset);
    public native int waitEvent();
    public native int waitEventIrq(int client);
    public native int getPosition();
}
