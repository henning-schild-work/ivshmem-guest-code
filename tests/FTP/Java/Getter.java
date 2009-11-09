import java.net.URL;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Getter {
    public static void main(String args[]) throws Exception {
        byte[] buf = new byte[1024*1024];
        int rd;

        FileOutputStream out = new FileOutputStream("gotten.file");

        System.setProperty("java.protocol.handler.pkgs", "org.ualberta.shm");
        URL u = new URL(args[0]);
        InputStream is = u.openStream();

        while(is.available() > 0) {
            rd = is.read(buf);
            out.write(buf, 0, rd);
        }

        out.close();
        is.close();
    }
}
