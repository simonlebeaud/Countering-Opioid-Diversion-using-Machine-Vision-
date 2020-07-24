package com.pilldetectionapp.pilloid;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class HandDetector {
    private static final String TAG = "HandDetector";
    private String modelFilename;
    private String labelFilename;
    private String label;
    private Interpreter handDetector;
    private Activity activity;
    Boolean isDownloaded = false;
    private static final double threshold = 0.2;
    private int inputSize;
    private ByteBuffer imgData;
    private AssetManager assetManager;

    private static float[][] anchors = new float[2944][4];
    private float[][][] outputReg = new float[1][2944][18];
    private float[][][] outputClf = new float[1][2944][1];

    private int[] intValues;
    // Only return this many results.
    private static final int NUM_DETECTIONS = 1;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;

    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;
    private static final int INPUT_SIZE = 256;

    private Boolean hand_detected;


    public HandDetector(Activity activity ,
                        final AssetManager assetManager,
                        final String modelFilename,
                        final String labelFilename,
                        final int inputSize,
                        final boolean isQuantized) throws IOException{
        this.hand_detected = false;
        this.activity = activity;
        this.inputSize = inputSize;
        this.assetManager = assetManager;


        InputStream labelsInput = null;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        this.label = br.readLine();


        br.close();

        this.inputSize = inputSize;

        try {
            this.handDetector = new Interpreter(loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.isModelQuantized = isQuantized;

        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        this.imgData = ByteBuffer.allocateDirect(this.inputSize * this.inputSize * 3 * numBytesPerChannel);
        this.imgData.order(ByteOrder.nativeOrder());
        this.intValues = new int[this.inputSize * this.inputSize];

        this.handDetector.setNumThreads(NUM_THREADS);
        this.outputLocations = new float[1][NUM_DETECTIONS][4];
        this.outputClasses = new float[1][NUM_DETECTIONS];
        this.outputScores = new float[1][NUM_DETECTIONS];
        this.numDetections = new float[1];

//        // read anchors.csv
//        try (Scanner scanner = new Scanner(assetManager.open("anchors.csv"));) {
//            int x = 0;
//            while (scanner.hasNextLine()) {
////        records.add(getRecordFromLine());
//                String[] cols = scanner.nextLine().split(",");
//                anchors[x++] = new float[]{Float.valueOf(cols[0]) , Float.valueOf(cols[1]) , Float.valueOf(cols[2]), Float.valueOf(cols[3])};
//            }
//        }
    }



    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = null;
        fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Boolean getHand_detected(){
        return this.hand_detected;
    }


    public void StartHandDetection(Mat frame) {

        Bitmap oribmp = bitmapFromMat(frame);
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
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
//          imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//          imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//          imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                    imgData.putFloat(((((pixelValue >> 16) & 0xFF) / 255f) - .5f) * 2);
                    imgData.putFloat(((((pixelValue >> 8) & 0xFF) / 255f) - .5f) * 2);
                    imgData.putFloat((((pixelValue & 0xFF) / 255f) - .5f) * 2);

                }
            }
        }
        Trace.endSection(); // preprocessBitmap;

        Trace.beginSection("feed");
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputReg);
        outputMap.put(1, outputClf);

        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        handDetector.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();

        // Show the best detections.
        // after scaling them back to the input size.

        ArrayList<float[]> candidate_detect_array = new ArrayList<float[]>();

        float[] clf = new float[outputClf[0].length];

        int max_idx = 0;
        double max_suggestion = 0;
        int count = 0;
        int clf_max_idx = 0;
        float x =0;

        // finding the best result of detecting hand
        for (int i = 0; i < outputClf[0].length; i++) {
            clf[i] = outputClf[0][i][0];

            x = 1 / Double.valueOf(1 + Math.exp(-outputClf[0][i][0])).floatValue();

            if (x > 0.98f) {
                count++;
                }
        }
        if (count > 2 ) {
            this.hand_detected = true;
            Log.e("Hand proba",String.valueOf(x));
        } else this.hand_detected = false;
    }

    public Bitmap bitmapFromMat(Mat mRgba) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);
        return bitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}
