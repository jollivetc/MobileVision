package fr.jollivetc.facetracker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Size;

import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Map;

/**
 * Handle the drawing of the mask on the overlay.
 */
class FaceGraphic extends GraphicOverlay.Graphic {

    private final static int TEMPLATE_INTER_EYES_DISTANCE = 60;
    private final static int TEMPLATE_WIDTH = 310;
    private final static int TEMPLATE_HEIGHT = 310;
    private volatile Face mFace;
    private Drawable drawable;
    private Resources resources;
    private Map<Integer, PointF> previousLandmark = new HashMap<>();

    FaceGraphic(GraphicOverlay overlay, Resources resources) {
        super(overlay);
        drawable = resources.getDrawable(R.drawable.flash);
        this.resources = resources;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Compute the position and size of the face to adapt the mask and draw it on the overlay.
     */
    @Override
    public void draw(Canvas canvas) {
        PointF detectLeftPosition = getLandmark(Landmark.LEFT_EYE);
        PointF detectRightPosition = getLandmark(Landmark.RIGHT_EYE);
        if ((detectLeftPosition == null) || (detectRightPosition == null)) {
            return;
        }

        PointF leftEyePosition =
                new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
        PointF rightEyePosition =
                new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

        double rotationAngle = Math.atan((leftEyePosition.y-rightEyePosition.y)/(leftEyePosition.x-rightEyePosition.x));
        float eyesDistance = (float)(Math.sqrt(Math.pow((rightEyePosition.x-leftEyePosition.x), 2)+ Math.pow((rightEyePosition.y - leftEyePosition.y), 2)));
        float resizeFactor = (eyesDistance)/ TEMPLATE_INTER_EYES_DISTANCE;

        PointF faceCenter = new PointF(leftEyePosition.x + (rightEyePosition.x - leftEyePosition.x) / 2, leftEyePosition.y + (rightEyePosition.y - leftEyePosition.y) / 2);
        Size finalDrawableSize = computeNewDrawableSize(resizeFactor, Math.abs(rotationAngle));
        Drawable drawableToDraw = rotateDrawable(rotationAngle);

        float top = faceCenter.y - finalDrawableSize.getHeight()/2;
        float left = faceCenter.x- finalDrawableSize.getWidth()/2;
        float right = faceCenter.x+finalDrawableSize.getWidth()/2;
        float bottom = faceCenter.y+ finalDrawableSize.getHeight()/2;

        drawableToDraw.setBounds(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
        drawableToDraw.draw(canvas);
    }

    private PointF getLandmark(int landMarkType){
        for (Landmark landmark : mFace.getLandmarks()) {
            if (landmark.getType() == landMarkType) {
                previousLandmark.put(landMarkType, landmark.getPosition());
                return landmark.getPosition();
            }
        }
        return previousLandmark.get(landMarkType);
    }

    private Drawable rotateDrawable(double rotationAngle) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate((float)(Math.toDegrees(rotationAngle)));
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,bitmap.getWidth(),bitmap.getHeight(),true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);
        return new BitmapDrawable(resources, rotatedBitmap);
    }

    private Size computeNewDrawableSize(float resizeFactor, double rotationAngle){
        float resizeWidth = TEMPLATE_WIDTH * resizeFactor;
        float resizeHeight = TEMPLATE_HEIGHT * resizeFactor;
        float finalWidth = (float) (resizeWidth * Math.cos(rotationAngle) + resizeHeight * Math.sin(rotationAngle));
        float finalHeight = (float) (resizeWidth * Math.sin(rotationAngle) + resizeHeight * Math.cos(rotationAngle));

        return new Size(Math.round(finalWidth), Math.round(finalHeight));
    }
}