package com.bodins;

import com.bodins.image.Shape;
import com.bodins.tree.Node;
import com.bodins.tree.Path;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DominoCounter {


    private static Scalar BLUE = new Scalar(255, 0, 0);
    private static Scalar BLACK = new Scalar(255, 255, 255);
    private static Scalar WHITE = new Scalar(0, 0, 0);

    private static double[] BLACK_A = new double[]{0, 0, 0};
    private static double[] WHITE_A = new double[]{255, 255, 255};

    private static final int CONFIG_WIDTH_SIZE = 1024;
    private static final int CONFIG_BLUR_SIZE = 1;

    public AtomicInteger count = new AtomicInteger(1);
    public boolean audit = false;

    public static void main(String[] args){
        cleanOutput();
        DominoCounter dc = new DominoCounter(true);
        OpenCV.loadLocally();
        dc.identify("sample-normal");
        dc.identify("sample-crowded");
        dc.identify("sample-three");
        dc.identify("sample-one");
    }

    public DominoCounter() {
        this(false);
    }

    public DominoCounter(boolean audit) {
        this.audit = audit;
    }

    public Mat identify(Mat image) {
        return this.identify("empty", image);
    }

    public Mat identify(String name) {
        Mat image = loadImage(name);
        return this.identify(name, image);
    }

    public Mat identify(String name, Mat image) {
        count.set(1);

        Mat working = copy(image);
        saveImage(name, working, "original");

        //Mat original_resized = resize(name, image, CONFIG_WIDTH_SIZE);
        balance_white(working);

        blur(name, working, CONFIG_BLUR_SIZE);
        grayscale2(name, working);
        Node<Shape> node = findCountours(name, working);

        Mat drawTo = working;

        drawCountours(node, drawTo);
        saveImage(name, drawTo, "final");
        return drawTo;
    }

    private Mat copy(Mat image){
      Mat result = new Mat();
      image.copyTo(result);
      return result;
    }

    private void blur(String name, Mat image, int size){
        Imgproc.medianBlur(image, image, size);
        saveImage(name, image, "blur");
    }
    private void grayscale1(String name, Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 7, 3);
        //Imgproc.threshold(image, image, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        saveImage(name, image, "gray");
    }
    private void grayscale2(String name, Mat image){
        for (int a = 0; a < image.rows(); a++) {
            for(int b = 0; b < image.cols(); b++) {
                double [] ps = image.get(a,b);
                //assume BGR (need RGB)
                //https://stackoverflow.com/questions/9780632/how-do-i-determine-if-a-color-is-closer-to-white-or-black
                //color c = Y < 128 ? black : white
                double lume = 0.2126*ps[2] + 0.7152*ps[1] + 0.0722*ps[0];
                double average = (ps[0] + ps[1] + ps[2]) / 3 ;
                if(average < 220) {
                    image.put(a, b, BLACK_A);
                }else{
                    image.put(a, b, WHITE_A);
                }
            }
        }
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        saveImage(name, image, "gray");
    }
    private Node<Shape> toNode(List<MatOfPoint> points, Mat hierarchy){
        Node node = new Node(Path.of("."));
        for(int i = 0; i < points.size(); i++) {

            MatOfPoint contour = points.get(i);

            //[Next, Previous, First_Child, Parent]
            List<Integer> pieces = new ArrayList<>();
            pieces.add(i);

            int parent = (int) hierarchy.get(0, i)[3];
            while (parent >= 0) {
                pieces.add((int) parent);
                parent = (int) hierarchy.get(0, parent)[3];
            }
            Collections.reverse(pieces);
            Path path = Path.of(pieces
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()).toArray(new String[pieces.size()]));
            node.add(path, new Shape(contour));
        }
        return node;
    }

    private Node<Shape> findCountours(String name, Mat gray){
        Mat edge = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> points = new ArrayList<>();

        //Imgproc.Canny(gray, edge, 84, 255, 7);
        Imgproc.Canny(gray, edge, 10,100);
        saveImage(name, edge, "canny");

        Imgproc.findContours(edge, points, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        return toNode(points, hierarchy);
    }

    private Mat drawCountours(Node<Shape> node, Mat drawTo){
        if(drawTo.channels() <3) {
            Imgproc.cvtColor(drawTo, drawTo, Imgproc.COLOR_GRAY2BGR);
        }
        node.getChildren().stream().forEach(c -> draw(c, drawTo));
        return drawTo;
    }

    private void draw(Node<Shape> n, Mat drawTo){
        n.visit(s -> s.draw(drawTo));
    }

    private Mat resize(String name, Mat image, double width){
        Mat dest = new Mat();
        int scale_percent = (int)(100 * width / image.width());
        int new_width = image.width() * scale_percent / 100;
        int new_height = image.height() * scale_percent / 100;
        Size dim = new Size(new_width, new_height);

        Imgproc.resize(image, dest, dim);
        saveImage(name, dest, "resize");

        return dest;
    }
    private Mat loadImage(String imagePath) {
        Imgcodecs imageCodecs = new Imgcodecs();
        return imageCodecs.imread("src/main/resources/" + imagePath + ".jpg");
    }

    private void saveImage(String name, Mat imageMatrix, String suffix) {
        if(audit) {
            Imgcodecs imgcodecs = new Imgcodecs();
            imgcodecs.imwrite("build/images/" + name + "-" + count.getAndIncrement() + "-" + suffix + ".jpg", imageMatrix);
        }
    }
    private static void cleanOutput(){
        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{jpg,jpeg}");
            Files.createDirectories(Paths.get("build/images/"));
            Files.list(Paths.get("build/images/"))
                    .filter(Files::isRegularFile)
                    .filter(f -> !matcher.matches(f))
                    .forEach(f -> {
                        try {
                            System.out.printf("Deleting %s\n", f);
                            Files.delete(f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //https://gist.github.com/tomykaira/94472e9f4921ec2cf582
    // reference http://www.ipol.im/pub/art/2011/llmps-scb/
    void balance_white(Mat mat) {

        double discard_ratio = 0.05;
        int hists[][] = new int[3][256];

        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                double [] ptr = mat.get(y,x);
                for (int j = 0; j < 3; ++j) {
                    hists[j][(int)ptr[j]] += 1;
                }
            }
        }

        // cumulative hist
        int total = mat.cols()*mat.rows();
        int[] vmin = new int[3], vmax = new int[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 255; j++) {
                hists[i][j + 1] += hists[i][j];
            }

            vmin[i] = 0;
            vmax[i] = 255;
            while (hists[i][vmin[i]] < discard_ratio * total) {
                vmin[i] += 1;
            }
            while (hists[i][vmax[i]] > (1 - discard_ratio) * total){
                vmax[i] -= 1;
            }

            if (vmax[i] < 255 - 1) {
                vmax[i] += 1;
            }
        }

        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                double [] ptr = mat.get(y,x);
                for (int j = 0; j < 3; j++) {
                    int val = (int)ptr[j];
                    if (val < vmin[j])
                        val = vmin[j];
                    if (val > vmax[j])
                        val = vmax[j];
                    ptr[j] = ((val - vmin[j]) * 255.0 / (vmax[j] - vmin[j]));
                }
                mat.put(y,x,ptr);
            }
        }
/*
  double discard_ratio = 0.05;
  int hists[3][256];
  memset(hists, 0, 3*256*sizeof(int));

  for (int y = 0; y < mat.rows; ++y) {
    //row
    uchar* ptr = mat.ptr<uchar>(y);
    for (int x = 0; x < mat.cols; ++x) {
      for (int j = 0; j < 3; ++j) {
        hists[j][ptr[x * 3 + j]] += 1;
      }
    }
  }
        // cumulative hist
        int total = mat.cols*mat.rows;
        int vmin[3], vmax[3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 255; ++j) {
                hists[i][j + 1] += hists[i][j];
            }
            vmin[i] = 0;
            vmax[i] = 255;
            while (hists[i][vmin[i]] < discard_ratio * total)
                vmin[i] += 1;
            while (hists[i][vmax[i]] > (1 - discard_ratio) * total)
                vmax[i] -= 1;
            if (vmax[i] < 255 - 1)
                vmax[i] += 1;
        }


        for (int y = 0; y < mat.rows; ++y) {
            uchar* ptr = mat.ptr<uchar>(y);
            for (int x = 0; x < mat.cols; ++x) {
                for (int j = 0; j < 3; ++j) {
                    int val = ptr[x * 3 + j];
                    if (val < vmin[j])
                        val = vmin[j];
                    if (val > vmax[j])
                        val = vmax[j];
                    ptr[x * 3 + j] = static_cast<uchar>((val - vmin[j]) * 255.0 / (vmax[j] - vmin[j]));
                }
            }
        }
        */

    }
}
