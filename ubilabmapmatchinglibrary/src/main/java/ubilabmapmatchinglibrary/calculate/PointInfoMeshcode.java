package ubilabmapmatchinglibrary.calculate;

public class PointInfoMeshcode {
    /**
     * 緯度経度からメッシュコードを返すクラス
     * 緯度経度はint型(Yahooマップス用?)
     * levelは区画指定.1辺15mの場合は9を指定する
     * @param lat
     * @param lon
     * @param level
     * @return
     */
    public static String calcMeshCode(int lat, int lon, int level){
        String meshcode = "";
		/* 1?? */
        int p =  lat / (60000 * 40);
        int u = (lon - 360000000)/3600000;

        meshcode = meshcode + p + u;

		/* 2?? */
        int a = lat %(60000 * 40);
        int q = a / (5 * 60000);

        int f = (lon - 360000000)%3600000;
        int v = (f / ((7 * 60000) + 30000 ));

        meshcode = meshcode + q + v;

		/*3??*/
        int b = a % (5 * 60000);
        int r = b / 30000;

        int g = (f % ((7 * 60000) + 30000 ));
        int w = g  / 45000;

        meshcode = meshcode + r + w;

        level -=3;
        int c = b %30000;
        int h = g % 45000;

        int times=1;
        while (level > 0){
            r = (c * times) / 15000;
            w = (h * times) / 22500;
            c = c % (15000 / times);
            h = h % (22500 / times);
            int m = ((r*2) +(w+1));

            //Log.d("Tag", "level:" + level + "c" + c + "h" + h + "r" + r + "w" + w + "m" + m);

            meshcode = meshcode + m;
            times *=2;
            --level;
        }
        return meshcode;
    }
    /* double型(度）の緯度経度をメッシュコードに変換 */
    public static String calcMeshCode(double lat, double lon, int level){
        int ilat = (int)(lat * 3600000.0);
        int ilon = (int)(lon * 3600000.0);
        return calcMeshCode(ilat, ilon, level);
    }
    public static String calcMeshCodeOld(double lat, double lon, int level){
        int ilat = (int)(lat * 3600000.0);
        int ilon = (int)(lon * 3600000.0);
        String meshcode = "";
		/* 1?? */
        int p =  (int)((lat * 60.0)/40.0);
        int u = (int)(lon - 100.0);

        meshcode = meshcode + p + u;

		/* 2?? */
        double a = (lat * 60.0) - ((double)p * 40.0);
        int q = (int)(a / 5.0);

        double f = lon - 100 - (double)u;
        int v = (int)((f *60.0)/ 7.5);

        meshcode = meshcode + q + v;

		/*3??*/
        double b = a- ((double)q * 5.0);
        int r = (int)((b *60.0)/30.0);

        double g = (f * 60.0) - ((double)v  * 7.5);
        int w = (int)((g * 60.0) / 45);

        meshcode = meshcode + r + w;

        level -=3;
        double c = (b * 60.0) - ((double)r * 30.0);
        double h = (g * 60.0) - ((double)w * 45.0);

        int times=1;
        double denomy=15.0;
        double denomx=22.5;
        while (level > 0){
            r = (int) (c / denomy);
            w = (int) (h / denomx);
            c = c  - ((double)b * denomy);
            h = h  - ((double)g * denomx);
            int m = ((r*2) +(w+1));

            ////Log.d ("Tag", "level:" + level +"c" + c+ "h" + h+ "r" + r + "w" + w + "m" + m);

            meshcode = meshcode + m;
            denomx /=2.0;
            denomy /=2.0;
            --level;
        }
        return meshcode;
    }
    /* メッシュコードが示す矩形を返す(lonw, lats, lone, latn) */
    public static int[]  calcRectFromMesh(String meshcode){
        int lat, lon;
        int steplat=0, steplon=0;

        ////Log.d("Tag", "meshcode:" + meshcode);
		/* 3次メッシュ以上は扱わない*/
        if (meshcode.length() < 8){
            return null;
        }

		/* 一次 */
        int start=0;
        int val = Integer.parseInt(meshcode.substring(start, start+2));
        lat = val * (60000 * 40);
        start+=2;
        ////Log.d("Tag", "meshcode:" + meshcode + ", start:" + start + ", meshcode.substring(start,2):" + meshcode.substring(start,2));
        val = Integer.parseInt(meshcode.substring(start, start+2));
        lon = (val * 3600000) + 360000000;
        start +=2;

		/* 二次 */
        val = Integer.parseInt(meshcode.substring(start, start+1));
        lat += val * (60000 * 5);
        ++start;
        val = Integer.parseInt(meshcode.substring(start, start+1));
        lon += val * (( 7 * 60000)  + 30000);
        ++start;

		/* 三次 */
        val = Integer.parseInt(meshcode.substring(start, start+1));
        lat += val * 30000;
        ++start;
        val = Integer.parseInt(meshcode.substring(start, start+1));
        lon += val * 45000;
        ++start;

		/*1/2以下*/
        int times=1;
        while (meshcode.length() > start){
            val = Integer.parseInt(meshcode.substring(start, start+1));
            steplat = 15000/times;
            lat += ((val/3) * 15000)/times;
            steplon = 22500/times;
            lon += (((val - 1)%2) * 22500)/times;
            ++start;
            times*=2;
        }
        int [] rect = new int [4];
        rect[0] = lon;
        rect[1] = lat;
        rect[2] = lon + steplon;
        rect[3] = lat + steplat;
        return rect;

    }

    /* メッシュコードの矩形(double)を返す*/
    public static double[]  calcDoubleRectFromMesh(String meshcode){

        int rect[] = calcRectFromMesh(meshcode);
        if (rect == null){
            return null;
        }
        double dRect []= new double[4];
        dRect[0] = (double)rect[0] / 3600000.0;
        dRect[1] = (double)rect[1] / 3600000.0;
        dRect[2] = (double)rect[2] / 3600000.0;
        dRect[3] = (double)rect[3] / 3600000.0;
        return dRect;

    }

    /* メッシュコードの中心緯度経度を返す*/
    public static int[]  calcPointFromMesh(String meshcode){
        int rect[] = calcRectFromMesh(meshcode);
        if (rect == null){
            return null;
        }
        //TODO: latlon で (lat,lon)を返しているが、(lon,lat)にすべきか？
        int pos[] = new int [2];
        pos[0] = rect[1] + ((rect[3] - rect[1]) /2);
        pos[1] = rect[0] + ((rect[2] - rect[0]) /2);
        return pos;
    }
    /* メッシュコードの中心緯度経度(double)を返す*/
    public static double[]  calcDoublePointFromMesh(String meshcode){

        int latlon[] = calcPointFromMesh(meshcode);
        if (latlon == null){
            return null;
        }
        double dLatlon []= new double[2];
        dLatlon[0] = (double)latlon[0] / 3600000.0;
        dLatlon[1] = (double)latlon[1] / 3600000.0;
        return dLatlon;

    }
}