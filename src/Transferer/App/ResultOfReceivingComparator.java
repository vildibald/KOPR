package Transferer.App;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Comparator;

/**
 * Created by Viliam on 11.3.2014.
 */
public class ResultOfReceivingComparator implements Comparator<ResultOfReceiving> {
    @Override
    public int compare(ResultOfReceiving o1, ResultOfReceiving o2) {
        return o1.getFile().getName().compareTo(o2.getFile().getName());
    }

    @Override
    public boolean equals(Object obj) {
        throw new NotImplementedException();
    }


}
