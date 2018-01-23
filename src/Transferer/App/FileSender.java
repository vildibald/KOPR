package Transferer.App;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Viliam on 28.1.2014.
 */
public class FileSender {
    private static final String HOST = "localhost";
    private static final int BASE_PORT = 43210;
    private static final String BUFFER_DIR = "BufferDir\\";
    private File file;
    private int numberOfSockets;
    private ExecutorService sendingExecutor;
    private Socket initSocket;


    private FileSender(File file, int numberOfSockets){
        this.file = file;
        this.numberOfSockets=numberOfSockets;
    }

    public static FileSender create(File file, int numberOfSockets){
        return new FileSender(file,numberOfSockets);
    }

    public void interrupt(){
        if(sendingExecutor!=null){
            sendingExecutor.shutdownNow();
        }
    }

    public void send() {
        //int numberOfSockets = 4;
        List<File> partsOfFile;
        //ExecutorService executor;
        //Socket initSocket;
        OutputStream initOutputStream;
        //File sendedFile;

        try {
            // vytvorime si Executor, ktory na vstup dostane pocet vlakien cez ktore budeme posielat subor
            sendingExecutor = Executors.newFixedThreadPool(numberOfSockets);
            initSocket = new Socket(HOST, BASE_PORT);
            // tymto budeme posielat inty a stringy ako velkosti suboru a nazov suboru
            StreamSender initStreamSender = new StreamSender(initSocket.getOutputStream());
            //sendedFile = new File(filePath);


            // posleme serveru cez kolko vlakien budeme posielat aby vytvoril prilsusny pocet soketov
            initStreamSender.sendInt(numberOfSockets);
            initStreamSender.sendString(file.getName());
            System.out.println("FileSender: Number of threads and file name sended");

            // prijmeme od serveru cisla, ktore znamenaju ci ma nejake nedokoncene stahovanie
            // ak predchadzajuce posielanie nebolo prerusene tak dostaneme same 0
            List<Integer> sizes = receiveUncompletedTransferInfo();

            // rozdelime subor na numberOfSockets casti
            partsOfFile = FileSplitterMerger.getInstance().splitFile(file, BUFFER_DIR, numberOfSockets);

            System.out.println("FileSender: Beginning file transfer...");

            // kazdu cast posleme samostatnym vlaknom
            executeSending(partsOfFile, sizes);

            //FileSplitterMerger.getInstance().clearBufferDir();

        } catch (Exception e) {

        }
    }

    private List<Integer> receiveUncompletedTransferInfo() throws IOException{

        System.out.println("FileSender: Receiving info of interrupted transfer");

        StreamReceiver interuptedTransferStreamReceiver = new StreamReceiver(initSocket.getInputStream());
        List<Integer> sizes = new ArrayList<Integer>(numberOfSockets);
        for(int i=0;i<numberOfSockets;i++){
            int size = interuptedTransferStreamReceiver.receiveInt();
            sizes.add(size);
        }
        System.out.println("FileSender: Info of interrupted transfer received");
        return sizes;
    }

    private void executeSending(List<File> partsOfFile, List<Integer> sizes) {
        for(int i = 0; i < numberOfSockets; i++){

            // vytvorime ByteSendery, kde kazdy dostane jednu cast suboru a pocet bajtov preruseneho predchadzajuceho stahovania
            sendingExecutor.execute(new ByteSender(partsOfFile.get(i), BASE_PORT + 1 + i, sizes.get(i)));
        }
    }
}
