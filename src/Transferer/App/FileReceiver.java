package Transferer.App;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by Viliam on 28.1.2014.
 */

/**
 * toto je serverova cast aplikacie
 */
public class FileReceiver implements Runnable {
    private static final String HOST = "localhost";
    private static final int BASE_PORT = 43210;
    private static final String RECEIVE_DIR = "RecDir\\";
    private Socket initSocket;
    private CompletionService<ResultOfReceiving> receivingCompletionService;

    // tato metoda je v podstate inak zapisany konstruktor
    public static FileReceiver create() {

        return new FileReceiver();
    }

    private void FileReceiver() {
        System.out.println("FileReceiver: instance created");
    }

    // tato metoda sa vola hned pri spusteni aplikacie
    public static void listen() {
        try {
            System.out.println("FileReceiver: Listening ...");
            ServerSocket initListener = new ServerSocket(BASE_PORT);
            // server pocuva...
            while (true) {
                FileReceiver fileReceiver = new FileReceiver();
                // ak je nadviazane spojenie...
                fileReceiver.initSocket = initListener.accept();
                // ... spusti prijmanie suboru v novom vlakne
                new Thread(fileReceiver).start();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }


    @Override
    public void run() {  //... Forrest, run!
        try {
            //System.out.println("Receiving started...");

            // initInputStream ma za uloho prijat metadata ako nazov suboru, pocet vlakien cez ktore klient posielat atd. ...
            InputStream initInputStream = initSocket.getInputStream();

            // StreamReceiver riesi prijmanie intov, stringov atd...
            StreamReceiver initStreamReceiver = new StreamReceiver(initInputStream);

            // numberOfSockets znamena cez kolko socketov budeme prijmat subor, teda kolko vlakien potrebujeme spustit na prijmanie
            int numberOfSockets = initStreamReceiver.receiveInt();

            // prijmeme nazov suboeu
            String receivedFileName = initStreamReceiver.receiveString();
            System.out.println("FileReceiver: Number od threads and file name received");

            // v pripade, ze sa nejedna o prijmanie noveho suboru, ale o pokracovanie preruseneho stahovania si zistime kolko sme vlastne prijali bajtov v kazdom vlakne
            List<Integer> sizes = getSizesOfUncompletedResults(receivedFileName);

            // posleme klientovi tie velkosti nami prijatych dat aby klient vedel odkial ma zacat posielat
            sendUncompletedTransferInfo(numberOfSockets, sizes);

            // vytvorime si ExecutorService, kde numberOfSockets znamena pocet vlakien, ktore executorService vytvori
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfSockets);
            // CompletionService je trieda ktorej funkcionalita je vratit objekt triedy ResultOfReceiving ked skoncia vsetky vlakna, teda ked vsetky casti suboru su prijate
            receivingCompletionService = new ExecutorCompletionService<ResultOfReceiving>(executorService);

            // zacneme prijmat subor
            submitReceiving(numberOfSockets, receivingCompletionService);

            // po skonceni prijmania (ci uz dokoncenom, alebo prerusenom) si ulozime vysledky prijmania
            List<ResultOfReceiving> results = getFileParts(numberOfSockets);

            // FileSplitterMerger ma za ulohu pospajat prijate casti suboru (ktorych pocet je numberOfSockets) do celku
            FileSplitterMerger fileSplitterMerger = FileSplitterMerger.getInstance(); // jedna sa o singleton, teda vytvorewna moze byt len jedna instancia

            // ak prijmanie je kompletne, ...
            if (arePartsCompleted(results)) {
                // ... tak spoj casti suborov v premennej results do jedneho suboru
                fileSplitterMerger.mergeFile(results, receivedFileName);
                // zmaz prijate casti suboru
                fileSplitterMerger.clearRecDir();
                // ak prijmanie je nekompletne (prerusene uzivatelom, vypadok spojenia, atd.)...
            } else {
                // uloz informacie o nedokoncenom prijmani
                fileSplitterMerger.saveInfo(results);
            }
            // ak je po vsetkom tak znicime vlakna
            executorService.shutdown();

        } catch (java.lang.Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    private void sendUncompletedTransferInfo(int numberOfSockets, List<Integer> sizes) throws IOException {

        StreamSender initStreamSender = new StreamSender(initSocket.getOutputStream());
        if (sizes != null) {
            for (int i = 0; i < numberOfSockets; i++) {
                initStreamSender.sendInt(sizes.get(i).intValue());
            }
        } else {
            for (int i = 0; i < numberOfSockets; i++) {
                initStreamSender.sendInt(0);
            }
        }
        System.out.println("FileReceiver: Info of interrupted transfer sended");
    }

    // tu prijmeme informacie o nedokoncenom stahovani
    private List<Integer> getSizesOfUncompletedResults(String fileName) {
        System.out.println("FileReceiver: Getting sizes of uncompleted received file parts...");
        List<Integer> sizes = new ArrayList<Integer>();

        List<ResultOfReceiving> uncompletedResults = null;
        try {
            // skusime nacitat informacie o neskoncenych stahovaniach
            uncompletedResults = FileSplitterMerger.getInstance().loadInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ak mame nejake neskoncene stahovanie
        if (uncompletedResults != null &&
                // a subor ktory nam klient posiela je ten isty co bol posielany naposledy
                fileName.equals(uncompletedResults.get(0).getFile().getName().substring(0, fileName.length()))) {

            // tu si musime utriedit nazvy casti suborov, lebo inak je zle :)
            // ide o to, ze ak prijamny subor ma nazov subor.txt a prijmame ho cez napr. 3 vlakna tak tie casti maju nazov subor.txt0, subor.txt1 a subor.txt2
            // nie vzdy totiz tie casti su nacitane podla poradia, tak preto ich triedime
            Comparator<ResultOfReceiving> comparator = new ResultOfReceivingComparator();
            Collections.sort(uncompletedResults, comparator);
            for (ResultOfReceiving resultOfReceiving : uncompletedResults) {
                // ulozime si velkosti ciastocne prijatych casti
                sizes.add((int) resultOfReceiving.getFile().length());
            }
            System.out.println("FileReceiver: Incompleted file parts found");
            return sizes;
        }
        System.out.println("FileReceiver: No incompleted file parts found");
        return null;
    }

    // zistime ci prijate casti su uplne alebo doslo k preruseniu alebo chybe
    private boolean arePartsCompleted(List<ResultOfReceiving> resultsOfReceiving) {
        System.out.println("FileReceiver: Checking if all parts are completed");
        for (ResultOfReceiving result : resultsOfReceiving) {
            if (result.getExpectedLength() > (int) result.getFile().length()) {
                System.out.println("FileReceiver: Some file part(s) is incompleted");
                return false;
            }

        }
        System.out.println("FileReceiver: All file parts successfully received");
        return true;
    }

    // metoda na prijmanie suboru cez numberOfSockets vlakien
    private void submitReceiving(int numberOfSockets, CompletionService<ResultOfReceiving> receivingCompletionService) {
        for (int i = 0; i < numberOfSockets; i++) {

            // byteReceiver ma za ulohu prijmat bajty jednej casti suboru v jednom vlakne
            // kazda cast bude prijmana cez port o 1 vyssim nez predchadzajuca cast
            ByteReceiver byteReceiver = new ByteReceiver(BASE_PORT + 1 + i, RECEIVE_DIR, true);

            // toto vytvori nove vlakno a zacne prijmat bajty 1 casti suboru
            receivingCompletionService.submit(byteReceiver);
            //futureCounters.add(receivingExecutor.submit(new Transferer.App.ByteReceiver(BASE_PORT+1+i)));
        }
    }

    private List<ResultOfReceiving> getFileParts(int numberOfSockets) throws InterruptedException, ExecutionException {
        List<ResultOfReceiving> results = new ArrayList<ResultOfReceiving>(numberOfSockets);
        for (int i = 0; i < numberOfSockets; i++) {
            // vezmeme si vysledky prijmania
            ResultOfReceiving result = receivingCompletionService.take().get();
            results.add(result);

        }
        return results;
    }
}

