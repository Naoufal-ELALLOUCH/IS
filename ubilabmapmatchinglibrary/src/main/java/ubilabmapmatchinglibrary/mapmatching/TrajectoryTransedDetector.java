package ubilabmapmatchinglibrary.mapmatching;

import java.util.ArrayList;

/**
 * Created by shun on 2014/12/17.
 */
public class TrajectoryTransedDetector {
    protected ArrayList<TrajectoryTransedListener> mTrajectoryTransedListeners = new ArrayList<TrajectoryTransedListener>();

    public void addListener(TrajectoryTransedListener trajectoryTransedListener) {
        mTrajectoryTransedListeners.add(trajectoryTransedListener);
    }

    public boolean isEmpty(){
        return mTrajectoryTransedListeners.isEmpty();
    }

}
