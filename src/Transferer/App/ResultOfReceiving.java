package Transferer.App;

import java.io.File;

/**
 * Created by Viliam on 1.3.2014.
 */

/**
 * Referencie na prijate subory su ukladane do objektov tejto triedy
 *
 */
public class ResultOfReceiving {

    private int expectedFileLength;
    private File file;

    public ResultOfReceiving(int expectedFileLength, File file) {
        this.expectedFileLength = expectedFileLength;
        this.file = file;
    }

    public static ResultOfReceiving create(int expectedFileLength,File file){
        return new ResultOfReceiving(expectedFileLength,file);
    }

    public int getExpectedLength() {
        return expectedFileLength;
    }

    public File getFile() {
        return file;
    }

}
