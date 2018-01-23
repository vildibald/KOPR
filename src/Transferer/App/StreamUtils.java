package Transferer.App;

/**
 * Created by Viliam on 27.12.2013.
 */
public class StreamUtils {

    public static byte[] toBytes(int integer) {
        byte bytes[] = new byte[4];
        for (int i=0; i < 4; i++) {

            int  bInt = (integer >> (i*8)) & 255;
            byte b = (byte) ( bInt );

            bytes[i] = b;
        }
        return bytes;
    }

    public static int toInt(byte[] bytes) {
        int integer = 0;
        for (int i=0; i<4; i++) {
            int b = (int) bytes[i];
            if (i<3 && b<0) {
                b=256+b;
            }
            integer += b << (i*8);
        }
        return integer;
    }
}
