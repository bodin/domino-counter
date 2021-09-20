package com.bodins.jfx;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bodins.DominoCounter;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a> (minor fixes)
 * @version 2.0 (2016-09-17)
 * @since 1.0 (2013-10-20)
 *
 */
public class WebCamController
{
	// the FXML button

	@FXML
	private Button startStop;

	@FXML
	private Button analyze;

	@FXML
	private ImageView currentFrame;

	private ScheduledExecutorService timer;
	private VideoCapture capture = new VideoCapture();
	private boolean cameraActive = false;
	private static int cameraId = 0;
	private DominoCounter counter = new DominoCounter();
	@FXML
	protected void startOrStopCamera(ActionEvent event) {
		if (this.cameraActive)	{
			this.stopCamera();
		} else {
			this.startCamera();
		}
		this.cameraActive = !this.cameraActive;
	}

	private void startCamera(){
		// start the video capture
		this.capture.open(cameraId);

		// is the video stream available?
		if (this.capture.isOpened()) {

			// grab a frame every 33 ms (30 frames/sec)
			Runnable frameGrabber = () -> {
				// effectively grab and process a single frame
				Mat frame = grabFrame();
				Mat marked = counter.identify(frame);

				// convert and show the frame
				Image imageToShow = Utils.mat2Image(marked);
				updateImageView(currentFrame, imageToShow);
			};

			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

			// update the button content
			this.startStop.setText("Stop Camera");
		} else {
			// log the error
			System.err.println("Impossible to open the camera connection...");
		}
	}
	private void stopCamera() {

		// update again the button content
		this.startStop.setText("Start Camera");

		// stop the timer
		this.stopAcquisition();
	}

	private Mat grabFrame() {
		// init everything
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened()){
			// read the current frame
			this.capture.read(frame);
		}
		
		return frame;
	}

	private void stopAcquisition() {
		if (this.timer!=null && !this.timer.isShutdown()){
			// stop the timer
			this.timer.shutdown();
			try{
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (this.capture.isOpened()){
			// release the camera
			this.capture.release();
		}
	}

	private void updateImageView(ImageView view, Image image){
		Utils.onFXThread(view.imageProperty(), image);
	}

	protected void setClosed() {
		this.stopAcquisition();
	}
}