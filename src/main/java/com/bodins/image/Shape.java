package com.bodins.image;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class Shape {

    private MatOfPoint contour;
    private double area;
    private double areaEncolsingCircle;

    public Shape(MatOfPoint contour) {
        this.contour = contour;
        this.area = Imgproc.contourArea(contour);

        //if(areaReal <= 100) continue;

        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());

        double peri = Imgproc.arcLength(contour2f, true);
        MatOfPoint2f approx2f = new MatOfPoint2f();
        Imgproc.approxPolyDP(contour2f, approx2f, 0.04*peri, true);

        Point p = new Point();
        float[] rads = new float[1];
        Imgproc.minEnclosingCircle(approx2f, p, rads);

        this.areaEncolsingCircle = rads[0] * rads[0] * Math.PI;
    }
    public boolean isImportant(){
        return this.area > 100;
    }

    public boolean isCircle(){
        return this.area / this.areaEncolsingCircle > .55;
    }

    public MatOfPoint getContour() {
        return contour;
    }

    public double getArea() {
        return area;
    }

    public double getAreaEncolsingCircle() {
        return areaEncolsingCircle;
    }
}
