package ubilabmapmatchinglibrary.mapmatching;

import java.util.ArrayList;

/**
 * Created by shun on 2014/12/17.
 */
public class TrajectoryTransedDetector {
    protected ArrayList<TrajectoryTransformedListener> mTrajectoryTransedListeners = new ArrayList<TrajectoryTransformedListener>();

    public void addListener(TrajectoryTransformedListener trajectoryTransedListener) {
        mTrajectoryTransedListeners.add(trajectoryTransedListener);
    }

    public boolean isEmpty(){
        return mTrajectoryTransedListeners.isEmpty();
    }

}
