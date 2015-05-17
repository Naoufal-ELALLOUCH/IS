package ubilabmapmatchinglibrary.pedestrianspacenetwork;

/**
 * Link(Node同士を結ぶ線)を表すクラス
 */
public class Link {
    private String id;
    private String node1Id;
    private String node2Id;
    private double distance;
    private double bearing;
    /*
     *  0:通路
     *  1:広場
     *   2:EV内同士(階層移動有)
     *  3:EV内外(階層移動無)
     *  4:階段(階層移動有)
     *  5:階段前・踊り場同士(階層移動無)
     *  6:スロープ
     */
    private LinkType type;
    private double pressureDiff;

//    private List<Long> grids;

    public Link(String id, String node1Id , String node2Id, double distance, double bearing, LinkType type, double pressureDiff) {
        this.id = id;
        this.node1Id = node1Id;
        this.node2Id = node2Id;
        this.distance = distance;
        this.bearing = bearing;
        this.type = type;
        this.pressureDiff = pressureDiff;
    }

    public Link(String id, String node1Id , String node2Id, double distance, double bearing, int type, double pressureDiff) {
        this.id = id;
        this.node1Id = node1Id;
        this.node2Id = node2Id;
        this.distance = distance;
        this.bearing = bearing;
        this.type = castToLinkType(type);
        this.pressureDiff = pressureDiff;
    }

    public void setLink(Link link){
        this.id = link.id;
        this.node1Id = link.node1Id;
        this.node2Id = link.node2Id;
        this.distance = link.distance;
        this.bearing = link.bearing;
        this.type = link.type;
        this.pressureDiff = link.pressureDiff;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNode1Id() {
        return node1Id;
    }

    public void setNode1Id(String node1Id) {
        this.node1Id = node1Id;
    }

    public String getNode2Id() {
        return node2Id;
    }

    public void setNode2Id(String node2Id) {
        this.node2Id = node2Id;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getBearing(){
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public LinkType getType() {
        return type;
    }

    public void setType(LinkType type) {
        this.type = type;
    }

    public void setType(int type) {
        this.type = castToLinkType(type);
    }

    public double getPressureDiff() {
        return  this.pressureDiff;
    }
    public void setPressureDiff(double pressureDiff) {
        this.pressureDiff = pressureDiff;
    }
    /**
     * リンクのタイプ(経路の区別をするためのもの)
     */
    public static enum LinkType {
        PASSAGE, //0:通路
        OPEN_SPACE, //1:広場
        EV,//2:EV内同士(階層移動有)
        EV_DOOR,//3:EV内外(階層移動無)
        STAIR,//4:階段(階層移動有)
        LANDING,//5:階段前・踊り場同士(階層移動無)
        SLOPE//6:スロープ
    }

    public static LinkType castToLinkType(int num) {
        LinkType[] enumArray = LinkType.values();
        for(LinkType enumInt : enumArray) {
            // 引数intとenum型の文字列部分を比較します。
            if (num == enumInt.ordinal()){
                return enumInt;
            }
        }
        return null;
    }
}

