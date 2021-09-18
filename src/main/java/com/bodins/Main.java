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

public class Main {
    public static AtomicInteger count = new AtomicInteger(1);
    private static Scalar RED = new Scalar(0, 0, 255);
    private static Scalar GREEN = new Scalar(0, 255, 0);
    private static Scalar BLUE = new Scalar(255, 0, 0);
    private static Scalar BLACK = new Scalar(255, 255, 255);
    private static Scalar WHITE = new Scalar(0, 0, 0);

    private static double[] BLACK_A = new double[]{0, 0, 0};
    private static double[] WHITE_A = new double[]{255, 255, 255};

    private static final int CONFIG_WIDTH_SIZE = 1024;
    private static final int CONFIG_BLUR_SIZE = 3;

    public static void main(String[] args){
        cleanOutput();
        OpenCV.loadLocally();
        run("sample-normal");
        run("sample-crowded");
        run("sample-three");
        run("sample-one");
    }

    public static void run(String name) {
        count.set(1);

        Mat image = loadImage(name);
        saveImage(name, image, "original");

        Mat original_resized = resize(name, image, CONFIG_WIDTH_SIZE);

        Mat working = copy(original_resized);
        blur(name, working, CONFIG_BLUR_SIZE);
        grayscale2(name, working);
        drawCountours(name, working, original_resized);

        saveImage(name, original_resized, "final");
    }
      private static Mat copy(Mat image){
          Mat result = new Mat();
          image.copyTo(result);
          return result;
      }

    private static void blur(String name, Mat image, int size){
        Imgproc.medianBlur(image, image, size);
        saveImage(name, image, "blur");
    }
    private static void grayscale1(String name, Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        //Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 7, 15);
        Imgproc.threshold(image, image, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        saveImage(name, image, "gray");
    }
    private static void grayscale2(String name, Mat image){
        for (int a = 0; a < image.rows(); a++) {
            for(int b = 0; b < image.cols(); b++) {
                double [] ps = image.get(a,b);
                //assume BGR (need RGB)
                //https://stackoverflow.com/questions/9780632/how-do-i-determine-if-a-color-is-closer-to-white-or-black
                //color c = Y < 128 ? black : white
                double lume = 0.2126*ps[2] + 0.7152*ps[1] + 0.0722*ps[0];
                double average = (ps[0] + ps[1] + ps[2]) / 3 ;
                if(average < 150) {
                    image.put(a, b, BLACK_A);
                }else{
                    image.put(a, b, WHITE_A);
                }
            }
        }
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        saveImage(name, image, "gray");
    }
    private static Node<Shape> toNode(List<MatOfPoint> points, Mat hierarchy){
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
    private static void drawCountours(String name, Mat gray, Mat drawTo){
        Mat edge = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> points = new ArrayList<>();

        Imgproc.Canny(gray, edge, 84, 255, 7);
        saveImage(name, edge, "canny");

        Imgproc.findContours(edge, points, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Node<Shape> node = toNode(points, hierarchy);
        node.getChildren().stream().forEach(c -> draw(c, drawTo));
    }
    private static void draw(Node<Shape> n, Mat drawTo){
        Shape s = n.getData();

        if(!s.isImportant()) return;

        if (s.isCircle()) {
            Imgproc.drawContours(drawTo, Arrays.asList(s.getContour()), 0, GREEN, 2);
        } else {
            Imgproc.drawContours(drawTo, Arrays.asList(s.getContour()), 0, RED, 2);
        }
        n.getChildren().stream().forEach(c -> draw(c, drawTo));
    }

    private static Mat resize(String name, Mat image, double width){
        Mat dest = new Mat();
        int scale_percent = (int)(100 * width / image.width());
        int new_width = image.width() * scale_percent / 100;
        int new_height = image.height() * scale_percent / 100;
        Size dim = new Size(new_width, new_height);

        Imgproc.resize(image, dest, dim);
        saveImage(name, dest, "resize");

        return dest;
    }
    private static Mat loadImage(String imagePath) {
        Imgcodecs imageCodecs = new Imgcodecs();
        return imageCodecs.imread("src/main/resources/" + imagePath + ".jpg");
    }

    private static void saveImage(String name, Mat imageMatrix, String suffix) {
         Imgcodecs imgcodecs = new Imgcodecs();
        imgcodecs.imwrite("build/images/" + name + "-" + count.getAndIncrement() + "-" + suffix + ".jpg", imageMatrix);
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
}
