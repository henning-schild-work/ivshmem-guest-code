// MemAccess.java:
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
 
public class MemAccess {
    static {
        System.loadLibrary("MemAccess");
    }
    
    public static void main(String[] args) {
        new MemAccess().readGeneratedMem();
    }
    
    void readGeneratedMem() {
        // get a reference to the object that holds a pointer to the c++ buffer
        ByteBuffer buffer = getGeneratedMem();
        buffer.order(ByteOrder.nativeOrder()); 
        int nbr = 16;
        // walk through the c++ buffer to display the values
        for (int i = 0; i < nbr; i++)
            System.out.println(buffer.getInt());
    }
    
    native ByteBuffer getGeneratedMem();
}
 

