package ubilabmapmatchinglibrary.pedestrianspacenetwork;

import com.google.android.gms.maps.model.LatLng;

/**
 * Node(交差点の中央などの点)を表すクラス
 */
public class Node {
    private int id;
    private double lat;
    private double lng;
    private int level; //TOD:今後踊り場とか追加するならdoubleに
    /*
        0:交差点
        1:広場
        2:EV内
        3:EV前
        4:階段入口
        5:階段踊り場
     */
    private NodeType type;
    private String gridId;

    public Node(int id, double lat, double lng,int level, NodeType type, String gridId) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.level = level;
        this.type = type;
        this.gridId = gridId;
    }

    public Node(int id, double lat, double lng,int level, int type, String gridId) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.level = level;
        this.type = castToNodeType(type);
        this.gridId = gridId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public void setLatLng(LatLng point) {
        lat = point.latitude;
        lng = point.longitude;
    }

    public LatLng getLatLng() {
        return new LatLng(lat, lng);
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public void setType(int type) {
        this.type = castToNodeType(type);
    }

    public String getGridId() {
        return gridId;
    }

    public void setGridId(String gridId) {
        this.gridId = gridId;
    }


    /**
     * ノードのタイプ
     */
    public static enum NodeType {
        INTERSECTION, //0:交差点
        OPEN_SPACE, //1:広場
        EV_IN, //2:EV内
        EV_OUT, //3:EV前
        STAIR, //4:階段(入口)
        LANDING; // 5:階段踊り場
    }


    public static NodeType castToNodeType(int num) {
        NodeType[] enumArray = NodeType.values();

        // 取得出来たenum型分ループします。
        for(NodeType enumInt : enumArray) {
            // 引数intとenum型の文字列部分を比較します。
            if (num == enumInt.ordinal()){
                return enumInt;
            }
        }
        return null;
    }
}
