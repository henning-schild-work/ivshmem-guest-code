import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Random;
 
public class MemAccess {
    static {
        System.loadLibrary("MemAccess");
    }
    
    native int openDevice(String devname);
    native int writeBytes(byte[] bytes, int offset, int cnt);
    native int readBytes(byte[] bytes, int offset, int cnt);
    native int writeInt(int towrite, int offset);
    native int readInt(int offset);
    native int InitLock(int offset);
    native int SpinLock(int offset);
    native int SpinUnlock(int offset);
    native int waitEvent(int client);
    native int waitEventIrq(int client);
}
