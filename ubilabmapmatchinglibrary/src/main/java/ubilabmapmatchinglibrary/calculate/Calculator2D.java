package ubilabmapmatchinglibrary.calculate;

import android.graphics.PointF;

import com.google.android.gms.maps.model.LatLng;

//位置情報や2次元座標を用いた計算を行うクラス
public class Calculator2D {
	
	/**
	 * 2点間の距離を求める関数
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
	    double R = 6371.137; // km
	    double dX = Math.toRadians(lng2 - lng1);
	    double y1 = Math.toRadians(lat1);
	    double y2 = Math.toRadians(lat2);
	    double distance = R * Math.acos(Math.sin(y1) * Math.sin(y2) + Math.cos(y1) * Math.cos(y2) * Math.cos(dX));
	    return distance * 1000; //m単位
	}

	/**
	 * 2点間の距離を求める関数
	 * @param point1
     * @param point2
	 * @return
	 */
	public static double calculateDistance(LatLng point1, LatLng point2) {
	    double R = 6371.137; // km
	    double dX = Math.toRadians(point2.longitude - point1.longitude);
	    double y1 = Math.toRadians(point1.latitude);
	    double y2 = Math.toRadians(point2.latitude);
	    double distance = R * Math.acos(Math.sin(y1) * Math.sin(y2) + Math.cos(y1) * Math.cos(y2) * Math.cos(dX));
	    return distance * 1000; //m単位
	}

	/**
	 * 2点間を結ぶ方角を求める関数
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static double calculateDirection(double lat1, double lng1, double lat2, double lng2) {
	    double dX = Math.toRadians(lng2 - lng1);
	    double y1 = Math.toRadians(lat1);
	    double y2 = Math.toRadians(lat2);
	    double direction = 90 - Math.toDegrees(Math.atan2(Math.sin(dX), Math.cos(y1) * Math.tan(y2) - Math.sin(y1) * Math.cos(dX)));

	    if(direction < -270) {
	    	direction += 360;
	    }
	    
	    return direction;
	}
	/**
	 * 2点間を結ぶ方角を求める関数
	 * @param point1
	 * @param point2
	 * @return
	 */
	public static double calculateDirection(LatLng point1, LatLng point2) {
	    double dX = Math.toRadians(point2.longitude - point1.longitude);
	    double y1 = Math.toRadians(point1.latitude);
	    double y2 = Math.toRadians(point2.latitude);
	    double direction = 90 - Math.toDegrees(Math.atan2(Math.sin(dX), Math.cos(y1) * Math.tan(y2) - Math.sin(y1) * Math.cos(dX)));

	    if(direction < -270) {
	    	direction += 360;
	    }
	    
	    return direction;
	}
	
	/**
	 * 角ABCを求める関数
	 * @param aLat
	 * @param aLng
	 * @param bLat
	 * @param bLng
	 * @param cLat
	 * @param cLng
	 * @return
	 */
	public static double calculateInteriorAngle(double aLat, double aLng, double bLat, double bLng, double cLat, double cLng) {
	    double a1 = aLng - bLng;
	    double a2 = aLat - bLat;
	    double b1 = cLng - bLng;
	    double b2 = cLat - bLat;
	    double cos = (a1 * b1 + a2 * b2) / (Math.sqrt(a1 * a1 + a2 * a2) * Math.sqrt(b1 * b1 + b2 * b2));
	    double angle = Math.toDegrees((Math.acos(cos)));
	    return angle;
	}
	
	/**
	 * 二つの線分(ABとCD)が交差しているか判定する
	 * @param aX AのX座標
	 * @param aY AのY座標
	 * @param bX BのX座標
	 * @param bY BのY座標
	 * @param cX CのX座標
	 * @param cY CのY座標
	 * @param dX DのX座標
	 * @param dY DのY座標
	 * @return 交差していたらtrue,していなかったらfalse
	 */
	public static boolean isCrossed2Line(double aX, double aY, double bX, double bY, double cX, double cY, double dX, double dY) {		
		double ta = ((cX - dX) * (aY - cY)) + ((cY - dY) * (cX - aX));
		double tb = ((cX - dX) * (bY - cY)) + ((cY - dY) * (cX - bX));
		double tc = ((aX - bX) * (cY - aY)) + ((aY - bY) * (aX - cX));
		double td = ((aX - bX) * (dY - aY)) + ((aY - bY) * (aX - dX));
		
		if((ta * tb) < 0 && (tc * td) < 0) {
			return true;
		} else {
			return false;
		}
	}

    /**
     * 二つの線分(ABとCD)が交差しているか判定する
     * @param pointA
     * @param pointB
     * @param pointC
     * @param pointD
     * @return 交差していたらtrue,していなかったらfalse
     */
    public static boolean isCrossed2Line(LatLng pointA, LatLng pointB, LatLng pointC, LatLng pointD) {
        return isCrossed2Line(pointA.longitude, pointA.latitude, pointB.longitude, pointB.latitude, pointC.longitude, pointC.latitude, pointD.longitude, pointD.latitude);
    }
//
//	public static PointF getProjectedPoint(double lat, double lng, double[] lineConstantNumbers) {
//		double x1 = lng;
//		double y1 = lat;
//		
//		double intermediateExpression = (lineConstantNumbers[0] * x1 + lineConstantNumbers[1] * y1 + lineConstantNumbers[2]) / (lineConstantNumbers[0] * lineConstantNumbers[0] + lineConstantNumbers[1] * lineConstantNumbers[1]);
//
//		double projectedLat = y1 - intermediateExpression * lineConstantNumbers[1];
//		double projectedLng = x1 - intermediateExpression * lineConstantNumbers[0];
//		return new PointF((float)projectedLng, (float)projectedLat);
//	}
	
	public static LatLng getProjectedPoint(LatLng point, double[] lineConstantNumbers) {
		double x1 = point.longitude;
		double y1 = point.latitude;
		
		double intermediateExpression = (lineConstantNumbers[0] * x1 + lineConstantNumbers[1] * y1 + lineConstantNumbers[2]) / (lineConstantNumbers[0] * lineConstantNumbers[0] + lineConstantNumbers[1] * lineConstantNumbers[1]);
		
		double projectedLat = y1 - intermediateExpression * lineConstantNumbers[1];
		double projectedLng = x1 - intermediateExpression * lineConstantNumbers[0];
		
		return new LatLng(projectedLat, projectedLng);
	}
	
	/**
	 * 直線ABと直線CDの交点を求める
	 * @param pointA
	 * @param pointB
	 * @param pointC
	 * @param pointD
	 * @return
	 */
	public static PointF calculateCrossPoint(PointF pointA, PointF pointB, PointF pointC, PointF pointD) {
		double f1 = pointB.x - pointA.x;
        double g1 = pointB.y - pointA.y;
        double f2 = pointD.x - pointC.x;
        double g2 = pointD.y - pointC.y;

        double det = f2 * g1 - f1 * g2;
        if (det == 0) {
            return null;
        }

        double dx = pointC.x - pointA.x;
        double dy = pointC.y - pointA.y;
        double t1 = (f2 * dy - g2 * dx) / det;

        double x = pointA.x + f1 * t1;
        double y = pointA.y + g1 * t1;
		
        PointF crossPoint = new PointF((float)x, (float)y);
        return crossPoint;

	}
	
	/**
	 * 直線ABと直線CDの交点を求める
	 * @param pointA
	 * @param pointB
	 * @param pointC
	 * @param pointD
	 * @return
	 */
	public static LatLng calculateCrossLocation(LatLng pointA, LatLng pointB, LatLng pointC, LatLng pointD) {		
		double f1 = pointB.longitude - pointA.longitude;
        double g1 = pointB.latitude - pointA.latitude;
        double f2 = pointD.longitude - pointC.longitude;
        double g2 = pointD.latitude - pointC.latitude;

        double det = f2 * g1 - f1 * g2;
        if (det == 0) {
            return null;
        }

        double dx = pointC.longitude - pointA.longitude;
        double dy = pointC.latitude - pointA.latitude;
        double t1 = (f2 * dy - g2 * dx) / det;

        double lng = pointA.longitude + f1 * t1;
        double lat = pointA.latitude + g1 * t1;
		
        LatLng crossPoint = new LatLng(lat, lng);
        return crossPoint;

	}

    /**
     * 方角が1周した場合の処理を行う
     * @param direction
     * @return
     */
	public static double loopDirection(double direction) {
		if(direction > 180) {
			direction -= 360;
		} else if (direction < -180){
			direction += 360;
		}
		return direction;
	}

    /**
     * PointをLatLngに変換する
     * @param p
     * @return
     */
	public static LatLng point2LatLng(PointF p) {
		return new LatLng(p.y, p.x);
	}
}