package com.pilldetectionapp.pilloid;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.pilldetectionapp.pilloid.exceptions.BoundingBoxOutOfPictureException;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FaceNetModel {

    private Interpreter interpreter;


    public FaceNetModel(AssetManager assetManager) {
        try {
            this.interpreter = new Interpreter(loadModelFile(assetManager));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.interpreter.setNumThreads(4);
    }

    // Gets an face embedding using FaceNet
    public float[] getFaceEmbedding( Bitmap image, Rect crop, float angle) throws BoundingBoxOutOfPictureException {
        //saveTempBitmap(cropRectFromBitmap(image, crop, angle));
        return runFaceNet(
                convertBitmapToBuffer(
                        cropRectFromBitmap( image , crop , angle )
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
        int imgSize = 160;
        ByteBuffer imageByteBuffer = ByteBuffer.allocateDirect(imgSize * imgSize * 3 * 4);
        imageByteBuffer.order( ByteOrder.nativeOrder() );
        Bitmap resizedImage = Bitmap.createScaledBitmap(image, imgSize, imgSize, false);
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

    private static MappedByteBuffer loadModelFile(AssetManager assets) throws IOException {
        AssetFileDescriptor fileDescriptor;
        fileDescriptor = assets.openFd("facenet.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Crop the given bitmap with the given rect.
    private Bitmap cropRectFromBitmap(Bitmap source, Rect rect , float angle ) throws BoundingBoxOutOfPictureException {
        Bitmap cropped;
        if ((rect.left+rect.width())>source.getWidth() || (rect.top+rect.height())>source.getHeight()) {
            throw new BoundingBoxOutOfPictureException("The face is partly out of the picture");
        }
        if ( angle != 0 ) {
            Log.e("SIZE BITMAP : ", source.getWidth() + " " + source.getHeight());
            Log.e("SIZE BITMAP : ", rect.left + " " + rect.top + " " + rect.width() + " " + rect.height());
            source = rotateBitmap( source, angle );
            cropped = Bitmap.createBitmap(source,
                    rect.left,
                    rect.top,
                    rect.width(),
                    rect.height());
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


    public void saveTempBitmap(Bitmap bitmap) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap);
        }else{
            //prompt the user or do something
        }
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fname = "Photo_"+ timeStamp +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
