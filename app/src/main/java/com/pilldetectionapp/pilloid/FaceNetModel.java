package com.pilldetectionapp.pilloid;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import com.pilldetectionapp.pilloid.exceptions.BoundingBoxOutOfPictureException;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FaceNetModel {

    private Interpreter interpreter;

    /**
     * Class computing any necessary information in order to do face recognition
     * @param assetManager assets, including the FaceNet model used for face recognition
     */
    public FaceNetModel(AssetManager assetManager) {
        Utils utils = new Utils();
        try {
            this.interpreter = new Interpreter(utils.loadModelFile(assetManager,"facenet.tflite"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.interpreter.setNumThreads(4);
    }

    /**
     * compute the embeddings from an image's face
     * @param image the image containing the face to be recognise
     * @param faceBoundingBox the bounding box of the face
     * @param angle angle needed if the image need to be rotated (it depends on the camera captor orientation)
     * @return the embedding of the face, which is a table of float of size 128
     * @throws BoundingBoxOutOfPictureException Sometimes the face can be somehow out of the picture
     */
    // Gets an face embedding using FaceNet
    public float[] getFaceEmbedding( Bitmap image, Rect faceBoundingBox, float angle) throws BoundingBoxOutOfPictureException {
        return runFaceNet(
                convertBitmapToBuffer(
                        cropRectFromBitmap( image, faceBoundingBox, angle )
                )
        )[0];

    }

    /**
     * Run the image through the model
     * @param inputs the image of the face converted to ByteBuffer
     * @return
     */
    private float[][] runFaceNet(ByteBuffer inputs){
        long t1 = System.currentTimeMillis();
        float[][] outputs = new float[1][128];
        interpreter.run(inputs, outputs);
        Log.e( "Performance" , "FaceNet Inference Speed in ms : " + (System.currentTimeMillis() - t1));
        return outputs;
    }

    /**
     * Coverts a bitmap to the byteBuffer of right dimension for the model's input
     * @param image the bitmap image to be converted
     * @return the converted bitmap to bytebuffer
     */
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

    /**
     * Crop the given bitmap with the given rect.
     * @param source bitmap image to be cropped
     * @param rect matrix used to crop
     * @param angle angle if image needs to be rotated prior to cropping
     * @return the cropped bitmap
     * @throws BoundingBoxOutOfPictureException
     */
    private Bitmap cropRectFromBitmap(Bitmap source, Rect rect , float angle ) throws BoundingBoxOutOfPictureException {
        Bitmap cropped;
        if ((rect.left+rect.width())>source.getWidth() || (rect.top+rect.height())>source.getHeight()) {
            throw new BoundingBoxOutOfPictureException("The face is partly out of the picture");
        }
        if ( angle != 0 ) {
            source = rotateBitmap( source, angle );
        }
        cropped = Bitmap.createBitmap(source,
                rect.left,
                rect.top,
                rect.width(),
                rect.height());
        return cropped;
    }

    /**
     * Rotate a given bitmap
     * @param source bitmap to be rotated
     * @param angle angle of rotation
     * @return the rotated bitma
     */
    private Bitmap rotateBitmap( Bitmap source , Float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate( angle );
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix , false );
    }

}
