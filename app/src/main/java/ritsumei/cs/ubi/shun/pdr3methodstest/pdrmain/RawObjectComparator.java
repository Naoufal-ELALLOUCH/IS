package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import java.util.Comparator;

/**
 * Created by Kohei on 15/05/11.
 */
public class RawObjectComparator implements Comparator<RawObject> {

    @Override
    public int compare(RawObject lhs, RawObject rhs) {
        long time1 = lhs.timestamp;
        long time2 = rhs.timestamp;

        if (time1 > time2) {
            return 1;
        } else if (time1 == time2) {
            return 0;
        } else {
            return -1;
        }
    }
}
