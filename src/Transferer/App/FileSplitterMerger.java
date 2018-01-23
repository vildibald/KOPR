package Transferer.App;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam on 27.1.2014.
 */

public class FileSplitterMerger {

    private static final String BUFFER_DIR = "BufferDir";
    private static final String REC_DIR = "RecDir";
    private static final String INFO_FILE = "InfoFile.json";
    private static volatile FileSplitterMerger instance = null;

    private FileSplitterMerger() {
    }

    public static FileSplitterMerger getInstance() {
        if (instance == null) {
            synchronized (FileSplitterMerger.class) {
                if (instance == null) {
                    instance = new FileSplitterMerger();
                }
            }
        }
        return instance;
    }

    /**
     * Tato metoda rozdeli subor file na numberOfParts suborov.
     * Funguje tak, ze cita bajty z file a uklada ich do priecinka destination do (novovytvoreneho) suboru file.
     * Ak precita 1/numberOfParts bajtov tak prestane zapisovat do file.0, ale vytvori subor file.1 a zacne ukladat do neho.
     * Ak precita 2/numberOfParts bajtov tak prestane zapisovat do file.1, ale vytvori subor file.2 a zacne ukladat do neho.
     * Ak precita 3/numberOfParts bajtov tak ...
     *
     * @param file
     * @param destination
     * @param numberOfParts
     * @return
     * @throws IOException
     */
    public List<File> splitFile(File file, String destination, int numberOfParts) throws IOException {
        System.out.println("Starting file spliting.");
        List<File> splitedFile = new ArrayList<File>(numberOfParts);
        String fileName = file.getName();
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        long fileSize = file.length();

        long sizeOfOnePart = fileSize / numberOfParts;
        long mod = fileSize % (long) numberOfParts;
        if (mod != 0) {
            sizeOfOnePart++;
        }
        for (int i = 0; i < numberOfParts; i++) {

            String filePartPath = destination + fileName + "." + i;

            FileOutputStream fos = new FileOutputStream(filePartPath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            OUT:
            for (int j = 0; j < sizeOfOnePart; j++) {
                int ch;
                ch = bis.read();
                if (ch == -1)
                    // skocime pred for cyklus
                    // viem, ze to je neskutocna prasacina, ale nechce sa mi s tym ***, ked to uz ide
                    break OUT;
                bos.write(ch);
            }

            File filePart = new File(filePartPath);
            splitedFile.add(filePart);
            bos.close();
        }

        bis.close();
        System.out.println("Spliting file " + fileName + " into " + numberOfParts + " parts completed");
        return splitedFile;
    }


    public File mergeFile(List<ResultOfReceiving> resultsOfReceiving, String destination) throws IOException {
        int numberOfParts = resultsOfReceiving.size();
        ArrayList<File> splitedFile = new ArrayList<File>(numberOfParts);

        for (int i = 0; i < numberOfParts; i++) {
            splitedFile.add(resultsOfReceiving.get(i).getFile());
        }

        return mergeFile(splitedFile, destination);
    }

    /**
     * Tato metoda spoji (uz prijate) subory ulozene v ArrayList-e splitedFile do jedneho suboru.
     * Funguje tak, ze cita bajty postupne zo vsetkych suborov v splitedFile a uklada ich do priecinka destination do (novovytvoreneho) suboru,
     * ktory bude mat rovnaky nazov ako je nazov 1. suboru v splitedFile bez poslednych 2 znakov
     *
     * @param splitedFile
     * @param destination
     * @return
     * @throws IOException
     */
    private File mergeFile(ArrayList<File> splitedFile, String destination) throws IOException {
        System.out.println("Starting file merging.");
        Collections.sort(splitedFile);
        File file = new File(destination);
        int numberOfParts = splitedFile.size();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        for (int i = 0; i < numberOfParts; i++) {
            File filePart = (File) splitedFile.get(i);
            FileInputStream fis = new FileInputStream(filePart);
            BufferedInputStream bis = new BufferedInputStream(fis);
            long sizeOfPart = filePart.length();
            OUT:
            for (long j = 0; j < sizeOfPart; j++) {
                int ch;
                ch = bis.read();
                if (ch == -1)
                    break OUT;
                bos.write(ch);
            }


            bis.close();
            fis.close();
        }
        bos.close();
        fos.close();
        System.out.println("File merging completed");
        return file;

    }

    /**
     * Metoda ktora zmaze rozdelene casti posielaneho suboru
     *
     * @throws IOException
     */
    public void clearBufferDir() throws IOException {
        File directory = new File(BUFFER_DIR);
        File[] files = directory.listFiles();
        for (File file : files) {
            // Delete each file
            if (!file.delete()) {
                System.out.println("Failed to delete " + file);
            }
        }
    }

    /**
     * Metoda ktora zmaze rozdelene casti prijmaneho suboru
     *
     * @throws IOException
     */
    public void clearRecDir() throws IOException {


        File directory = new File(REC_DIR);
        File[] files = directory.listFiles();
        for (File file : files) {
            // Delete each file
            if (!file.delete()) {
                System.out.println("Failed to delete " + file);
            }
        }
        PrintWriter pw = new PrintWriter(new File(INFO_FILE));
        pw.close();
    }

    /**
     * Ukladame informacie o prerusenom transfere do suboru typu JSON
     *
     * @param results
     * @throws IOException
     */
    public void saveInfo(List<ResultOfReceiving> results) throws IOException {
        System.out.println("Saving interrupted transfer info");
        File file = new File(INFO_FILE);
        Gson gson = new Gson();
        String jsonResults = gson.toJson(results);
        System.out.println(jsonResults);


        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(jsonResults);
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (IOException e) {
            System.err.println("Error while saving interrupted transfer status");
        }
    }

    public List<ResultOfReceiving> loadInfo() throws IOException {
        File file = new File(INFO_FILE);
        JsonReader jsonReader = new JsonReader(new BufferedReader(new FileReader(file)));
        Gson gson = new Gson();
        Type type = new TypeToken<List<ResultOfReceiving>>() {
        }.getType();
        return gson.fromJson(jsonReader, type);
    }

    /**
     * Nechce sa mi robit unit test.
     * @param args
     */
    public static void main(String[] args) {

        FileSplitterMerger fsm = FileSplitterMerger.getInstance();
        try {
            List<ResultOfReceiving> list = new ArrayList<ResultOfReceiving>(4);
            for (int i = 0; i < 4; i++) {
                list.add(new ResultOfReceiving(i, new File("blabla" + i)));
            }

            fsm.saveInfo(list);
            list = fsm.loadInfo();
            fsm.saveInfo(list);


        } catch (Exception e) {

        }
    }
}
