package com.pilldetectionapp.pilloid;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Trace;

import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * class to detect hand on a frame
 */
public class HandDetector {
    private Utils utils;
    private static final String TAG = "HandDetector";
    private static final int INPUT_SIZE = 256;
    private String label;
    private Interpreter handDetector;
    private ByteBuffer imgData;
    private AssetManager assetManager;

    private float[][][] outputReg = new float[1][2944][18];
    private float[][][] outputClf = new float[1][2944][1];

    private int[] intValues;
    // Only return this many results.
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;



    private Boolean hand_detected;

    /**
     * constructor
     * @param assetManager assets containing the model
     * @param modelFilename the model file name
     * @param labelFilename the labels file name
     */
    public HandDetector(final AssetManager assetManager,
                        final String modelFilename,
                        final String labelFilename) throws IOException{
        this.utils = new Utils();
        this.hand_detected = false;
        this.assetManager = assetManager;


        InputStream labelsInput = null;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        this.label = br.readLine();


        br.close();


        try {
            this.handDetector = new Interpreter(utils.loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Pre-allocate buffers.
        int numBytesPerChannel = 4; // Floating point

        this.imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * numBytesPerChannel);
        this.imgData.order(ByteOrder.nativeOrder());
        this.intValues = new int[INPUT_SIZE * INPUT_SIZE];

        this.handDetector.setNumThreads(NUM_THREADS);

    }

    /**
     * getter on private boolean attribute hand_detected
     * @return the value of the attribute
     */
    public Boolean getHand_detected(){
        return this.hand_detected;
    }

    /**
     * Does hand detection on the hand and assign true to hand_detected attribute if hand is detected
     * @param frame
     */
    public void StartHandDetection(Mat frame) {

        Bitmap oribmp = utils.bitmapFromMat(frame);
        Bitmap bitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        bitmap = getResizedBitmap(oribmp, 256, 256);
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // put the image in imgData
        imgData.rewind();
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
                imgData.putFloat(((((pixelValue >> 16) & 0xFF) / 255f) - .5f) * 2);
                imgData.putFloat(((((pixelValue >> 8) & 0xFF) / 255f) - .5f) * 2);
                imgData.putFloat((((pixelValue & 0xFF) / 255f) - .5f) * 2);

            }
        }
        Trace.endSection(); // preprocessBitmap;

        Trace.beginSection("feed");

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputReg);
        outputMap.put(1, outputClf);

        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        handDetector.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();


        float[] clf = new float[outputClf[0].length];

        int count = 0;
        float x =0;

        // finding the best result of detecting hand
        for (int i = 0; i < outputClf[0].length; i++) {
            clf[i] = outputClf[0][i][0];

            x = 1 / Double.valueOf(1 + Math.exp(-outputClf[0][i][0])).floatValue();

            if (x > 0.98f) {
                count++;
                }
        }
        if (count > 3 ) {
            this.hand_detected = true;

        } else this.hand_detected = false;
    }

    /**
     * Get the resized bitmap of a given bitmap
     * @param bitmap Bitmap to be resized
     * @param newWidth width of the resized bitmap
     * @param newHeight height of the resized bitmap
     * @return the resized bitmap
     */
    public Bitmap getResizedBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, false);
        bitmap.recycle();
        return resizedBitmap;
    }

}
