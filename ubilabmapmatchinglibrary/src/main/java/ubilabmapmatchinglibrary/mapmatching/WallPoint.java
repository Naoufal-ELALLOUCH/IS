package ubilabmapmatchinglibrary.mapmatching;

import com.google.android.gms.maps.model.LatLng;

/**
 * WallPoint(交差点の4隅の頂点などの通路の出口を構成する点)を表すクラス
 */
public class WallPoint {
    int id;
    int nodeId;
    int groupNumber;
    int pointOrder;
    double lat;
    double lng;
    int grid;

    public WallPoint(int id, int nodeId, int groupNumber, int pointOrder, double lat, double lng, int grid) {
        this.id = id;
        this.nodeId = nodeId;
        this.groupNumber = groupNumber;
        this.pointOrder = pointOrder;
        this.lat = lat;
        this.lng = lng;
        this.grid = grid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(int groupNumber) {
        this.groupNumber = groupNumber;
    }

    public int getPointOrder() {
        return pointOrder;
    }

    public void setPointOrder(int pointOrder) {
        this.pointOrder = pointOrder;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public LatLng getLatng() {
        return new LatLng(lat, lng);
    }

    public void setLatLng(LatLng point) {
        this.lat = point.latitude;
        this.lng = point.longitude;
    }

    public int getGrid() {
        return grid;
    }

    public void setGrid(int grid) {
        this.grid = grid;
    }

}

