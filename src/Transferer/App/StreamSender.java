package Transferer.App;

import java.io.*;

/**
 * Created by Viliam on 27.12.2013.
 */

/**
 * Toto je trieda, ktora ma nastarost rozdelit subor na pakety a poslat ich, dalej vie posielat stringy inty a podobne.
 * Problem je ze vsetko co tu je uz je v Jave spravene (delenie na pakety, posielanie intov a stringov), ale ked som ten projekt robil tak som to nevedel.
 * Cize ja tu je v podstate spravene len to co uz je v Jave, konkretne priamo FileOutputStream alebo este lepsie BufferedOutputStream to vsetko uz vedia (aj ich tu vyuzivam).
 */
public class StreamSender {

    private final int PACKET_SIZE = 1024;
    private OutputStream outputStream;

    public StreamSender(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void sendInt(int integer) throws IOException {
        byte[] byteArray4 = StreamUtils.toBytes(integer);
        outputStream.write(byteArray4);
    }

    public void sendString(String string) throws IOException {
        int stringLength = string.length();
        sendInt(stringLength);
        for (int i = 0; i < stringLength; i++) {
            outputStream.write((byte) string.charAt(i));
        }
        outputStream.flush();
    }

    public void sendFile(String filePath) throws IOException {
        sendFile(filePath, 0);
    }

    public void sendFile(String filePath, int fromPacket) throws IOException {
        File file = new File(filePath);
        sendFile(file, fromPacket);
    }

    public void sendFile(File file) throws IOException {
        sendFile(file, 0);
    }

    public void sendFile(File file, int fromByte) throws IOException {
        sendInt((int) file.length() - fromByte);
        sendString(file.getName());
        byte bytes[] = new byte[PACKET_SIZE];
        InputStream fileInputStream = new FileInputStream(file);
        int numRead = 0;

        if (fromByte > 0) {
            fileInputStream.skip(fromByte);
        }
        while ((numRead = fileInputStream.read(bytes)) > 0) {

            outputStream.write(bytes, 0, numRead);
            if (Thread.currentThread().interrupted()) {
                System.out.println("Stopping transfer for file: " + file.getName());
                outputStream.flush();
                outputStream.close();
                return;
            }

        }
        outputStream.flush();
        outputStream.close();
    }

    public void sendBytes(byte[] bytes) throws IOException {
        sendInt(bytes.length);

        InputStream inputStream = new ByteArrayInputStream(bytes);
        int numRead = 0;
        byte[] buffer = new byte[PACKET_SIZE];

        while ((numRead = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, numRead);
        }
        outputStream.flush();

    }
}
