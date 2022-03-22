package test;

import java.lang.Math;

public class StatLib {

	// simple average
	public static float avg(float[] x) {
		int i;
		float tempSum = 0;
		for (i = 0; i < x.length; i++)
			tempSum += x[i];
		return (tempSum / x.length);
	}

	// returns the variance of X and Y
	public static float var(float[] x) {
		float sum = avg(x);
		float variance = 0;
		for (float v : x) variance += Math.pow(v - sum, 2);
		return variance / x.length;
	}

	// returns the covariance of X and Y
	public static float cov(float[] x, float[] y) {
		float sum = 0;
		for (int i = 0; i < x.length; i++)
			sum += ((x[i] - avg(x)) * (y[i] - avg(y)));
		return (sum / x.length);
	}

	public static float pearson(float[] x, float[] y) {
		return (float) (cov(x, y) * (1/Math.sqrt(var(x)) * (1/Math.sqrt(var(y)))));
	}

	// performs a linear regression and returns the line equation
	public static Line linear_reg(Point[] points) {
		float[] x = new float[points.length];
		float[] y = new float[points.length];

		for (int i = 0; i < points.length; i++) {
			x[i] += points[i].x;
			y[i] += points[i].y;
		}
		float a = (cov(x, y) * (1 / var(x)));
		float b = avg(y) - (a * avg(x));

		return new Line(a, b);
	}

	// returns the deviation between point p and the line equation of the points
	public static float dev(Point p, Point[] points) {
		Line line = linear_reg(points);
		return Math.abs(p.y - line.f(p.x));
	}

	// returns the deviation between point p and the line
	public static float dev(Point p, Line l) {
		return Math.abs(p.y - l.f(p.x));
	}
}
