package ubilabmapmatchinglibrary.mapmatching;

public class Point {
	
	private double x;
	private double y;
	
	public Point() {
		x = 0;
		y = 0;
	}
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Point(Point p) {
		this.x = p.x;
		this.y = p.y;
	}
	
	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public void set(Point p) {
        if(p != null) {
            this.x = p.x;
            this.y = p.y;
        } else {
            this.x = 0;
            this.y = 0;
        }
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
}
