package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.hardware.SensorEvent;

/**
 * Created by Kohei on 15/05/11.
 */
public class RawObject
{
    public int type;
    public long timestamp;
    public float[] values;

    RawObject(int sensorType, long timestamp, float[] values) {
        this.type = sensorType;
        this.timestamp = timestamp;
        this.values = values;
    }

    public int getType() {
        return type;
    }
}
