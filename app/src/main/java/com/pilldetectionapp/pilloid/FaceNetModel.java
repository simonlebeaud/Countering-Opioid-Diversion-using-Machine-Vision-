package com.pilldetectionapp.pilloid;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceNetModel {

    private Interpreter interpreter;

    private int imgSize = 160;


    public FaceNetModel(AssetManager assetManager) {
        try {
            this.interpreter = new Interpreter(loadModelFile(assetManager, "facenet.tflite"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.interpreter.setNumThreads(4);
    }

    // Gets an face embedding using FaceNet
    public float[] getFaceEmbedding( Bitmap image, Rect crop, Boolean preRotate) {
        return runFaceNet(
                convertBitmapToBuffer(
                        cropRectFromBitmap( image , crop , preRotate )
                )
        )[0];
    }

    // Run the FaceNet model.
    private float[][] runFaceNet(ByteBuffer inputs){
        long t1 = System.currentTimeMillis();
        float[][] outputs = new float[1][128];
        interpreter.run(inputs, outputs);
        Log.e( "Performance" , "FaceNet Inference Speed in ms : " + (System.currentTimeMillis() - t1));
        return outputs;
    }

    // Resize the given bitmap and convert it to a ByteBuffer
    private ByteBuffer convertBitmapToBuffer(Bitmap image) {
        ByteBuffer imageByteBuffer = ByteBuffer.allocateDirect( 1 * imgSize * imgSize * 3 * 4 );
        imageByteBuffer.order( ByteOrder.nativeOrder() );
        Bitmap resizedImage = Bitmap.createScaledBitmap(image, imgSize , imgSize, false);
        for (int i = 0; i < imgSize; i++) {
            for (int j = 0; j < imgSize; j++) {
                int pixelValue = resizedImage.getPixel(i, j);
                imageByteBuffer.putFloat((((pixelValue >> 16) & 0xFF) - 128f) / 128f);
                imageByteBuffer.putFloat((((pixelValue >> 8) & 0xFF) - 128f) / 128f);
                imageByteBuffer.putFloat((((pixelValue) & 0xFF) - 128f) / 128f);
            }
        }
        return imageByteBuffer;
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

    // Crop the given bitmap with the given rect.
    private Bitmap cropRectFromBitmap(Bitmap source, Rect rect , Boolean preRotate ) {
        Bitmap cropped = null;
        if ( preRotate ) {
            cropped = rotateBitmap( source, 90f );
        } else {
            cropped = Bitmap.createBitmap(source,
                    rect.left,
                    rect.top,
                    rect.width(),
                    rect.height());
        }
        return cropped;
    }

    private Bitmap rotateBitmap( Bitmap source , Float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate( angle );
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix , false );
    }
}
