package com.bodins.jfx;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.FileNotFoundException;
import java.io.IOException;

public class WebCam extends Application {

    Mat matrix = null;

    public static void main(String args[]) throws InterruptedException {
        OpenCV.loadLocally();
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) {
        try {
            // load the FXML resource
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("jfx/FXHelloCV.fxml"));

            // store the root element so that the controllers can use it
            BorderPane rootElement = loader.load();
            // create and style a scene
            Scene scene = new Scene(rootElement, 800, 600);
            scene.getStylesheets().add(getClass().getClassLoader().getResource("jfx/application.css").toExternalForm());
            // create the stage with the given title and the previously created
            // scene
            primaryStage.setTitle("JavaFX meets OpenCV");
            primaryStage.setScene(scene);
            // show the GUI
            primaryStage.show();

            // set the proper behavior on closing the application
            WebCamController controller = loader.getController();
            primaryStage.setOnCloseRequest((e) -> controller.setClosed());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NOT USED
    private void start2(Stage stage) throws FileNotFoundException, IOException, InterruptedException {
        // Capturing the snapshot from the camera
        WebCam obj = new WebCam();
        WritableImage writableImage = obj.capureSnapShot();

        // Saving the image
        //obj.saveImage();

        // Setting the image view
        ImageView imageView = new ImageView(writableImage);

        // setting the fit height and width of the image view
        imageView.setFitHeight(400);
        imageView.setFitWidth(600);

        // Setting the preserve ratio of the image view
        imageView.setPreserveRatio(true);

        // Creating a Group object
        Group root = new Group(imageView);

        // Creating a scene object
        Scene scene = new Scene(root, 600, 400);

        // Setting title to the Stage
        stage.setTitle("Capturing an image");

        // Adding scene to the stage
        stage.setScene(scene);

        // Displaying the contents of the stage
        stage.show();

    }
    private WritableImage capureSnapShot() {
        WritableImage WritableImage = null;

        // Instantiating the VideoCapture class (camera:: 0)
        VideoCapture capture = new VideoCapture(0);

        // Reading the next video frame from the camera
        Mat matrix = new Mat();
        capture.read(matrix);

        // If camera is opened
        if( capture.isOpened()) {
            // If there is next video frame
            if (capture.read(matrix)) {
                // Creating BuffredImage from the matrix
                BufferedImage image = new BufferedImage(matrix.width(),
                        matrix.height(), BufferedImage.TYPE_3BYTE_BGR);

                WritableRaster raster = image.getRaster();
                DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
                byte[] data = dataBuffer.getData();
                matrix.get(0, 0, data);
                this.matrix = matrix;

                // Creating the Writable Image
                WritableImage = SwingFXUtils.toFXImage(image, null);
            }
        }
        return WritableImage;
    }
    public void saveImage() {
        // Saving the Image
        String file = "E:/OpenCV/chap22/sanpshot.jpg";

        // Instantiating the imgcodecs class
        Imgcodecs imageCodecs = new Imgcodecs();

        // Saving it again
        imageCodecs.imwrite(file, matrix);
    }
}
