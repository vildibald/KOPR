package Transferer.App;

import java.io.File;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by Viliam on 29.1.2014.
 */

/**
 * Objekty tejto triedy su vytvarane objektami triedy FileReceiver ktora pre kazde vlakno vytvori prave 1 ByteReceiver
 * Ten ma za ulohu prijat a ulozit 1 cast suboru ktore su nasledne poskladane objektom triedy
 * FileSplitterMerger.
 */
public class ByteReceiver implements Callable {

    private int port;
    private  boolean append;
    private String destination;

    public ByteReceiver(int port, String destination, boolean append) {
        this.port = port;
        this.destination = destination;
        this.append = append;
    }

    public int getPort() {
        return port;
    }

    @Override
    public ResultOfReceiving call() {
        try{

            // Vytvorime serverSocket a cakame na spojenie
            Socket socket;
            ResultOfReceiving receivedFilePart;
            ServerSocket listener =new ServerSocket(port);
            // ked je spojenie nadviazane...
            socket=listener.accept();
            InputStream inputStream = socket.getInputStream();
                // Toto bute prijmat a spracovavat pakety
            StreamReceiver streamReceiver = new StreamReceiver(inputStream);

            // ... tak zacneme prijmat pakety
            receivedFilePart = streamReceiver.receiveFile(destination, append);
           // done=true;
            socket.close();

            // len informacne veci
            // nie je to dolezite
            synchronized (this){

                int totalReceivedLength =(int) receivedFilePart.getFile().length();
               int totalExpectedLength = receivedFilePart.getExpectedLength();
                int receivedLengthInLastTransferAttempt = totalReceivedLength;
                if(totalReceivedLength > totalExpectedLength){
                    receivedLengthInLastTransferAttempt = Math.abs(-totalReceivedLength+totalExpectedLength);
                }

                System.out.println("\nThread for socket on port: "+port+" is done.");
                System.out.println("Number of bytes received from port: " + port + " is: " + totalReceivedLength);
                System.out.println("Expected number of bytes from port: " + port + " is: " + totalExpectedLength);
            }

            // ked sa vlakno skonci vratime prijatu cast suboru
            return receivedFilePart;
        }catch (Exception ex){

        }
        return null;
    }
}
