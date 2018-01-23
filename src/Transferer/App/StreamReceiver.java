package Transferer.App;

import java.io.*;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Viliam on 27.12.2013.
 */

/**
 * Toto je trieda, ktora ma nastarost prijat pakety a poskladat ich do suboru, dalej vie prijmat
 * stringy inty a podobne.
 * Toto nebudem komentovat, lebo toto ani nie je moj kod to som nasiel na nete.
 * Problem je ze vsetko co tu je uz je v Jave spravene (prijmanie paketov, prijmanie intov a
 * stringov), ale ked som ten projekt robil tak som to nevedel.
 * Cize ja tu je v podstate spravene len to co uz je v Jave, konkretne priamo FileInputStream
 * alebo este lepsie BufferedInputStream to vsetko uz vedia (aj ich tu vyuzivam).
 */
public class StreamReceiver {
    private InputStream inputStream;
    private final int PACKET_SIZE = 1024;

    public StreamReceiver(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public int receiveInt() throws IOException {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) inputStream.read();
        bytes[1] = (byte) inputStream.read();
        bytes[2] = (byte) inputStream.read();
        bytes[3] = (byte) inputStream.read();

        return StreamUtils.toInt(bytes);
    }

    public String receiveString() throws IOException {
        int stringLength = receiveInt();
        String string = new String();

        for (int i = 0; i < stringLength; i++) {
            string += (char) inputStream.read();
        }
        return string;
    }

    public ResultOfReceiving receiveFile(String destination, boolean append) throws
            IOException {

        int length = receiveInt();
        String fileName = receiveString();
        String filePath = destination + fileName;
        File file;
        file = new File(filePath);

        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file, append));

        receiveFile(bufferedOutputStream, length, PACKET_SIZE);
        bufferedOutputStream.close();
        return ResultOfReceiving.create(length, file);
    }

    private void receiveFile(OutputStream outputStream, int length, int bufferSize) throws
            IOException {

        byte[] buffer = new byte[bufferSize];

        int lengthRead = 0;
        int totalLengthRead = 0;

        while (totalLengthRead + bufferSize <= length) {
            lengthRead = inputStream.read(buffer);
            if (lengthRead == -1 && totalLengthRead < length) {
                System.err.println("Socket connection interrupted on remote side");
                return;
            }
            totalLengthRead += lengthRead;
            outputStream.write(buffer, 0, lengthRead);
        }

        if (totalLengthRead < length) {
            receiveFile(outputStream, length - totalLengthRead, bufferSize / 2);
        }
    }

    private void receiveBytes(InputStream inputStream, ByteArrayOutputStream byteArrayOutputStream, int length, int bufferSize)
            throws IOException {

        byte[] buffer = new byte[bufferSize];

        int lengthRead = 0;
        int totalLengthRead = 0;


        while (totalLengthRead + bufferSize <= length) {
            lengthRead = inputStream.read(buffer);

            totalLengthRead += lengthRead;

            byteArrayOutputStream.write(buffer, 0, lengthRead);

        }

        if (totalLengthRead < length) {

            receiveBytes(inputStream, byteArrayOutputStream, length - totalLengthRead, bufferSize / 2);
        }
    }

    public byte[] receiveBytes() throws IOException {
        int length = receiveInt();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(length);

        receiveBytes(inputStream, byteArrayOutputStream, length, PACKET_SIZE);

        return byteArrayOutputStream.toByteArray();


    }
}
