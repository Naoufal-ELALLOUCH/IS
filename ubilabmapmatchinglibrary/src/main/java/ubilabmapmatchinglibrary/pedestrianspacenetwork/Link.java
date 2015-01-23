package ubilabmapmatchinglibrary.pedestrianspacenetwork;

/**
 * Link(Node同士を結ぶ線)を表すクラス
 */
public class Link {
    private int id;
    private int node1Id;
    private int node2Id;
    private double distance;
    private double bearing;
    /*
     * リンクを表す線分の方程式の定数
     * (y = ax + b)
     */
    private double constantA;
    private double constantB;

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

//    private List<Long> grids;

    public Link(int id, int node1Id , int node2Id, double distance, double bearing, double constantA, double constantB, LinkType type) {
        this.id = id;
        this.node1Id = node1Id;
        this.node2Id = node2Id;
        this.distance = distance;
        this.bearing = bearing;
        this.constantA = constantA;
        this.constantB = constantB;
        this.type = type;
    }

    public Link(int id, int node1Id , int node2Id, double distance, double bearing, double constantA, double constantB, int type) {
        this.id = id;
        this.node1Id = node1Id;
        this.node2Id = node2Id;
        this.distance = distance;
        this.bearing = bearing;
        this.constantA = constantA;
        this.constantB = constantB;
        this.type = castToLinkType(type);
    }

    public void setLink(Link link){
        this.id = link.id;
        this.node1Id = link.node1Id;
        this.node2Id = link.node2Id;
        this.distance = link.distance;
        this.bearing = link.bearing;
        this.constantA = link.constantA;
        this.constantB = link.constantB;
        this.type = link.type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNode1Id() {
        return node1Id;
    }

    public void setNode1Id(int node1Id) {
        this.node1Id = node1Id;
    }

    public int getNode2Id() {
        return node2Id;
    }

    public void setNode2Id(int node2Id) {
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

    public double getConstantA() {
        return constantA;
    }

    public void setConstantA(double constant) {
        this.constantA = constant;
    }
    public double getConstantB() {
        return constantB;
    }

    public void setConstantB(double constant) {
        this.constantB = constant;
    }

    public double[] getconstants() {
        double[] constants = {constantA, constantB};
        return constants;
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
        SLOPE;//6:スロープ
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

