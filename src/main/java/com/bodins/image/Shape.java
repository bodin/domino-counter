package com.bodins.image;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

public class Shape {
    private static Scalar RED = new Scalar(0, 0, 255);
    private static Scalar GREEN = new Scalar(0, 255, 0);

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

    public void draw(Mat drawTo){
        //if(!this.isImportant()) return;

        if (this.isCircle()) {
            Imgproc.drawContours(drawTo, Arrays.asList(this.contour), 0, GREEN, 2);
        } else {
            Imgproc.drawContours(drawTo, Arrays.asList(this.contour), 0, RED, 2);
        }
    }
    public boolean isImportant(){
        return this.area > 100;
    }

    public boolean isCircle(){
        return this.area / this.areaEncolsingCircle > .65;
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
