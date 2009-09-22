// MemAccess.java:
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
 
public class MemAccess {
    static {
        System.loadLibrary("MemAccess");
    }
    
    public static void main(String[] args) throws Exception {
        new MemAccess().readGeneratedMem();
    }
    
    void readGeneratedMem() throws Exception {
        // get a reference to the object that holds a pointer to the c++ buffer
        ByteBuffer buffer = getGeneratedMem();
        buffer.order(ByteOrder.nativeOrder()); 
        long nbr = 16;
        // walk through the c++ buffer to display the values
//        for (int i = 0; i < nbr; i++)
//            System.out.println(buffer.getLong());
    
	closeGeneratedMem();

        MessageDigest md = MessageDigest.getInstance("SHA-1");

        md.update(buffer);
        byte[] bytes = md.digest();
        System.out.println(HexConversions.bytesToHex(bytes).toLowerCase());

	buffer.position(0);
	md.reset();
        md.update(buffer);
        bytes = md.digest();

        System.out.println(HexConversions.bytesToHex(bytes).toLowerCase());

    }
    
    native ByteBuffer getGeneratedMem();
    native void closeGeneratedMem();
 
	private static final String toHex(int s) {
		if (s < 10) {
		   return new StringBuffer().
                                append((char)('0' + s)).
                                toString();
		} else {
		   return new StringBuffer().
                                append((char)('A' + (s - 10))).
                                toString();
		}
	}

}
