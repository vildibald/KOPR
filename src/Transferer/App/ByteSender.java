package Transferer.App; /**
 * Created by Viliam on 27.12.2013.
 */

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Objekty tejto triedy su vytvarane objektami triedy FileSender ktora pre kazde vlakno vytvori prave 1 ByteSender
 * Ten ma za ulohu poslat 1 cast suboru ktory bol predtym rozdeleny na casti objektom triedy FileSplitterMerger.
 */
public class ByteSender implements Runnable{
    private final String HOST = "localhost";
    private int port;
    // subor (presnejsie cast rozdeleneho suboru) ktory budeme posielat vo vlakne
    private File file;
    private int fromByte;


    public ByteSender(File file, int port, int fromByte) {
        this.file = file;
        this.port = port;
        this.fromByte = fromByte;
    }

    @Override
    public void run() {
        Socket socket;
        OutputStream outputStream;
        StreamSender streamSender;

        try {
            // vytvorime soket
            socket = new Socket(HOST, port);
            outputStream = socket.getOutputStream();
            // cez to budeme posielat pakety
            streamSender = new StreamSender(outputStream);
            // posleme cast suboru
            streamSender.sendFile(file, fromByte);
            //socket.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}