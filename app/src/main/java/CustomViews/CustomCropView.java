package CustomViews;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.example.chateasy.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import Utility.LoggerUtil;

enum CropMode {
    NONE, MOVE, RESIZE
}

public class CustomCropView extends AppCompatImageView {
    private final int numGridLines = 3;
    private final float gridLineWidth = 2;
    private final float cornerCircleRadius = 10;
    private RectF cropRect;
    private Paint cropPaint;
    private CropMode mode;
    private PointF start;
    private PointF end;
    private int corner;
    private Paint blurPaint;
    private RectF blurRect;
    private RectF unselectedRect;
    private Paint gridPaint;
    private int cropBorderColor;
    private float cropBorderWidth;
    private float cropBorderRadius;

    // Single instance of Paint and RectF objects
    private Paint backgroundPaint;
    private Paint filledCirclePaint;
    private RectF filledCircleRect;

    public CustomCropView(Context context) {
        super(context);
        initCustomCropView(context, null);
    }

    public CustomCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCustomCropView(context, attrs);
    }

    public CustomCropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCustomCropView(context, attrs);
    }

    private void initCustomCropView(Context context, AttributeSet attrs) {

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomCropView);
            cropBorderColor = a.getColor(R.styleable.CustomCropView_cropBorderColor, Color.YELLOW);
            cropBorderWidth = a.getDimension(R.styleable.CustomCropView_cropBorderWidth, 5);
            cropBorderRadius = a.getDimension(R.styleable.CustomCropView_cropBorderRadius, 0);
            a.recycle();
        } else {
            cropBorderColor = Color.YELLOW;
            cropBorderWidth = 5;
            cropBorderRadius = 0;
        }

        cropRect = new RectF(100, 100, 400, 400);

        cropPaint = new Paint();
        cropPaint.setColor(cropBorderColor);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(cropBorderWidth);
        PathEffect pathEffect = new android.graphics.CornerPathEffect(cropBorderRadius);
        cropPaint.setPathEffect(pathEffect);

        mode = CropMode.NONE;
        start = new PointF();
        end = new PointF();
        corner = -1;

        setClickable(true);
        blurPaint = new Paint();
        blurRect = new RectF();
        unselectedRect = new RectF();

        gridPaint = new Paint();
        gridPaint.setColor(cropPaint.getColor());
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(gridLineWidth);
        PathEffect gridPathEffect = new android.graphics.CornerPathEffect(cropBorderRadius);
        gridPaint.setPathEffect(gridPathEffect);

        // Initialize Paint and RectF objects
        backgroundPaint = new Paint();
        filledCirclePaint = new Paint();
        filledCircleRect = new RectF();
    }

    /*    public void setImageUri(String imageUriString) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    public void setImageUri(String imageUriString) {
        Uri imageUri = Uri.parse(imageUriString);
        try {
            ContentResolver contentResolver = getContext().getContentResolver();
            ImageDecoder.Source source = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                source = ImageDecoder.createSource(contentResolver, imageUri);
            }
            Bitmap bitmap = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(source);
            }
            setImageBitmap(bitmap);
        } catch (IOException e) {
            LoggerUtil.logError("Image is not convert: ", e);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawGridLines(canvas);
        drawCropFrame(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handleTouchEvent(event);
        return true;
    }

    private void drawBackground(@NonNull Canvas canvas) {
        float left = Math.max(0, cropRect.left);
        float top = Math.max(0, cropRect.top);
        float right = Math.min(getWidth(), cropRect.right);
        float bottom = Math.min(getHeight(), cropRect.bottom);

        // Draw shadow outside the crop area
        canvas.save();
        /*canvas.clipRect(left, top, right, bottom, Region.Op.DIFFERENCE);*/
        canvas.clipOutRect(left, top, right, bottom);

        backgroundPaint.setColor(Color.parseColor("#80000000")); // Replace with your shadow color
        backgroundPaint.setStyle(Paint.Style.FILL);

        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        canvas.restore();

        cropPaint.setColor(cropBorderColor);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(cropBorderWidth);
    }

    private void drawCropFrame(@NonNull Canvas canvas) {
        canvas.drawRect(cropRect, cropPaint);

        drawFilledCornerCircle(canvas, cropRect.left, cropRect.top);
        drawFilledCornerCircle(canvas, cropRect.right, cropRect.top);
        drawFilledCornerCircle(canvas, cropRect.left, cropRect.bottom);
        drawFilledCornerCircle(canvas, cropRect.right, cropRect.bottom);
    }

    private void handleTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                start.set(event.getX(), event.getY());
                corner = checkCorner(start.x, start.y);
                if (corner != -1) {
                    mode = CropMode.RESIZE;
                } else if (cropRect.contains(start.x, start.y)) {
                    mode = CropMode.MOVE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                end.set(event.getX(), event.getY());
                if (mode == CropMode.MOVE) {
                    adjustRectPosition();
                } else if (mode == CropMode.RESIZE) {
                    adjustRectSize();
                }
                invalidate();
                start.set(end.x, end.y);
                break;
            case MotionEvent.ACTION_UP:
                mode = CropMode.NONE;
                corner = -1;
                performClick(); // Trigger performClick when a touch is released
                break;
        }
    }

    private void drawFilledCornerCircle(@NonNull Canvas canvas, float x, float y) {
        // Increase the size of the circle by multiplying the radius with a scaling factor
        float increasedRadius = cornerCircleRadius * 2.0f; // You can adjust the scaling factor as needed

        filledCircleRect.set(x - increasedRadius, y - increasedRadius, x + increasedRadius, y + increasedRadius);
        filledCirclePaint.setColor(cropPaint.getColor());
        filledCirclePaint.setStyle(Paint.Style.FILL);
        canvas.drawOval(filledCircleRect, filledCirclePaint);
    }


    private void drawGridLines(Canvas canvas) {
        float cellWidth = cropRect.width() / numGridLines;
        float cellHeight = cropRect.height() / numGridLines;

        float startX = cropRect.left;
        float startY = cropRect.top;

        // Draw horizontal grid lines
        for (int i = 1; i < numGridLines; i++) {
            float y = startY + i * cellHeight;
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint); // Use gridPaint for grid lines
        }

        // Draw vertical grid lines
        for (int i = 1; i < numGridLines; i++) {
            float x = startX + i * cellWidth;
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint); // Use gridPaint for grid lines
        }
    }

    private int checkCorner(float x, float y) {
        float threshold = 20;
        if (Math.abs(x - cropRect.left) < threshold && Math.abs(y - cropRect.top) < threshold) {
            return 0;
        } else if (Math.abs(x - cropRect.right) < threshold && Math.abs(y - cropRect.top) < threshold) {
            return 1;
        } else if (Math.abs(x - cropRect.left) < threshold && Math.abs(y - cropRect.bottom) < threshold) {
            return 2;
        } else if (Math.abs(x - cropRect.right) < threshold && Math.abs(y - cropRect.bottom) < threshold) {
            return 3;
        } else {
            return -1;
        }
    }

    private void adjustRectPosition() {
        int dx = (int) (end.x - start.x);
        int dy = (int) (end.y - start.y);
        if (cropRect.left + dx >= 0 && cropRect.right + dx <= getWidth()
                && cropRect.top + dy >= 0 && cropRect.bottom + dy <= getHeight()) {
            cropRect.offset(dx, dy);
        }
    }

    private void adjustRectSize() {
        int dx = (int) (end.x - start.x);
        int dy = (int) (end.y - start.y);
        switch (corner) {
            case 0:
                cropRect.left += dx;
                cropRect.top += dy;
                break;
            case 1:
                cropRect.right += dx;
                cropRect.top += dy;
                break;
            case 2:
                cropRect.left += dx;
                cropRect.bottom += dy;
                break;
            case 3:
                cropRect.right += dx;
                cropRect.bottom += dy;
                break;
        }
        int minSize = 50;
        if (cropRect.left < 0) {
            cropRect.left = 0;
        }
        if (cropRect.right > getWidth()) {
            cropRect.right = getWidth();
        }
        if (cropRect.top < 0) {
            cropRect.top = 0;
        }
        if (cropRect.bottom > getHeight()) {
            cropRect.bottom = getHeight();
        }
        if (cropRect.width() < minSize) {
            if (corner == 0 || corner == 2) {
                cropRect.left = cropRect.right - minSize;
            } else {
                cropRect.right = cropRect.left + minSize;
            }
        }
        if (cropRect.height() < minSize) {
            if (corner == 0 || corner == 1) {
                cropRect.top = cropRect.bottom - minSize;
            } else {
                cropRect.bottom = cropRect.top + minSize;
            }
        }
    }

    public void rotateImage(float degrees) {
        Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap originalBitmap = bitmapDrawable.getBitmap();

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            setImageBitmap(rotatedBitmap);

            // Update cropRect after rotation
            RectF newCropRect = new RectF(0, 0, rotatedBitmap.getWidth(), rotatedBitmap.getHeight());
            getImageMatrix().mapRect(newCropRect);
            cropRect.set(newCropRect);

            // Redraw the view
            invalidate();
        }
    }


    public byte[] getCroppedImage() {
        BitmapDrawable drawable = (BitmapDrawable) getDrawable();
        if (drawable != null) {
            float[] matrixValues = new float[9];
            getImageMatrix().getValues(matrixValues);
            float scale = matrixValues[Matrix.MSCALE_X];
            float transX = matrixValues[Matrix.MTRANS_X];
            float transY = matrixValues[Matrix.MTRANS_Y];

            int cropStartX = (int) ((cropRect.left - transX) / scale);
            int cropStartY = (int) ((cropRect.top - transY) / scale);
            int cropWidth = (int) (cropRect.width() / scale);
            int cropHeight = (int) (cropRect.height() / scale);

            cropStartX = Math.max(0, cropStartX);
            cropStartY = Math.max(0, cropStartY);

            Bitmap cropped = Bitmap.createBitmap(drawable.getBitmap(), cropStartX, cropStartY,
                    cropWidth, cropHeight);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            cropped.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            return stream.toByteArray();
        } else {
            return new byte[0];
        }
    }
}

/*package CustomViews;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

import com.example.chateasy.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import Utility.LoggerUtil;

enum CropMode {
    NONE, MOVE, RESIZE
}

public class CustomCropView extends AppCompatImageView {
    private RectF cropRect;
    private Paint cropPaint;
    private CropMode mode;
    private PointF start;
    private PointF end;
    private int corner;
    private Paint blurPaint;
    private RectF blurRect;
    private RectF unselectedRect;
    private Paint gridPaint;
    private int numGridLines = 3;
    private float gridLineWidth = 2;
    private float cornerCircleRadius = 10;
    private int cropBorderColor;
    private float cropBorderWidth;
    private float cropBorderRadius;

    // Single instance of Paint and RectF objects
    private Paint backgroundPaint;
    private Paint filledCirclePaint;
    private RectF filledCircleRect;

    public CustomCropView(Context context) {
        super(context);
        initCustomCropView(context, null);
    }

    public CustomCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCustomCropView(context, attrs);
    }

    public CustomCropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCustomCropView(context, attrs);
    }

    private void initCustomCropView(Context context, AttributeSet attrs) {

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomCropView);
            cropBorderColor = a.getColor(R.styleable.CustomCropView_cropBorderColor, Color.YELLOW);
            cropBorderWidth = a.getDimension(R.styleable.CustomCropView_cropBorderWidth, 5);
            cropBorderRadius = a.getDimension(R.styleable.CustomCropView_cropBorderRadius, 0);
            a.recycle();
        } else {
            cropBorderColor = Color.YELLOW;
            cropBorderWidth = 5;
            cropBorderRadius = 0;
        }

        cropRect = new RectF(100, 100, 400, 400);

        cropPaint = new Paint();
        cropPaint.setColor(cropBorderColor);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(cropBorderWidth);
        PathEffect pathEffect = new android.graphics.CornerPathEffect(cropBorderRadius);
        cropPaint.setPathEffect(pathEffect);

        mode = CropMode.NONE;
        start = new PointF();
        end = new PointF();
        corner = -1;

        setClickable(true);
        blurPaint = new Paint();
        blurRect = new RectF();
        unselectedRect = new RectF();

        gridPaint = new Paint();
        gridPaint.setColor(cropPaint.getColor());
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(gridLineWidth);
        PathEffect gridPathEffect = new android.graphics.CornerPathEffect(cropBorderRadius);
        gridPaint.setPathEffect(gridPathEffect);

        // Initialize Paint and RectF objects
        backgroundPaint = new Paint();
        filledCirclePaint = new Paint();
        filledCircleRect = new RectF();
    }

    public void setImageUri(String imageUriString) {
        Uri imageUri = Uri.parse(imageUriString);
        try {
            ContentResolver contentResolver = getContext().getContentResolver();
            ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, imageUri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(source);
            setImageBitmap(bitmap);
        } catch (IOException e) {
            LoggerUtil.logError("Image is not convert: ", e);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawGridLines(canvas);
        drawCropFrame(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handleTouchEvent(event);
        return true;
    }

    private void drawBackground(Canvas canvas) {
        float left = Math.max(0, cropRect.left);
        float top = Math.max(0, cropRect.top);
        float right = Math.min(getWidth(), cropRect.right);
        float bottom = Math.min(getHeight(), cropRect.bottom);

        // Draw shadow outside the crop area
        canvas.save();
        canvas.clipRect(left, top, right, bottom);

        backgroundPaint.setColor(Color.parseColor("#80000000")); // Replace with your shadow color
        backgroundPaint.setStyle(Paint.Style.FILL);

        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        canvas.restore();

        cropPaint.setColor(cropBorderColor);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(cropBorderWidth);
    }

    private void drawCropFrame(Canvas canvas) {
        canvas.drawRect(cropRect, cropPaint);

        drawFilledCornerCircle(canvas, cropRect.left, cropRect.top);
        drawFilledCornerCircle(canvas, cropRect.right, cropRect.top);
        drawFilledCornerCircle(canvas, cropRect.left, cropRect.bottom);
        drawFilledCornerCircle(canvas, cropRect.right, cropRect.bottom);
    }

    private void handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                start.set(event.getX(), event.getY());
                corner = checkCorner(start.x, start.y);
                if (corner != -1) {
                    mode = CropMode.RESIZE;
                } else if (cropRect.contains(start.x, start.y)) {
                    mode = CropMode.MOVE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                end.set(event.getX(), event.getY());
                if (mode == CropMode.MOVE) {
                    adjustRectPosition();
                } else if (mode == CropMode.RESIZE) {
                    adjustRectSize();
                }
                invalidate();
                start.set(end.x, end.y);
                break;
            case MotionEvent.ACTION_UP:
                mode = CropMode.NONE;
                corner = -1;
                performClick(); // Trigger performClick when a touch is released
                break;
        }
    }

    private void drawFilledCornerCircle(Canvas canvas, float x, float y) {
        filledCircleRect.set(x - cornerCircleRadius, y - cornerCircleRadius, x + cornerCircleRadius, y + cornerCircleRadius);
        filledCirclePaint.setColor(cropPaint.getColor());
        filledCirclePaint.setStyle(Paint.Style.FILL);
        canvas.drawOval(filledCircleRect, filledCirclePaint);
    }

    private void drawGridLines(Canvas canvas) {
        float cellWidth = cropRect.width() / numGridLines;
        float cellHeight = cropRect.height() / numGridLines;

        float startX = cropRect.left;
        float startY = cropRect.top;

        // Draw horizontal grid lines
        for (int i = 1; i < numGridLines; i++) {
            float y = startY + i * cellHeight;
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint); // Use gridPaint for grid lines
        }

        // Draw vertical grid lines
        for (int i = 1; i < numGridLines; i++) {
            float x = startX + i * cellWidth;
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint); // Use gridPaint for grid lines
        }
    }

    private int checkCorner(float x, float y) {
        float threshold = 20;
        if (Math.abs(x - cropRect.left) < threshold && Math.abs(y - cropRect.top) < threshold) {
            return 0;
        } else if (Math.abs(x - cropRect.right) < threshold && Math.abs(y - cropRect.top) < threshold) {
            return 1;
        } else if (Math.abs(x - cropRect.left) < threshold && Math.abs(y - cropRect.bottom) < threshold) {
            return 2;
        } else if (Math.abs(x - cropRect.right) < threshold && Math.abs(y - cropRect.bottom) < threshold) {
            return 3;
        } else {
            return -1;
        }
    }

    private void adjustRectPosition() {
        int dx = (int) (end.x - start.x);
        int dy = (int) (end.y - start.y);
        if (cropRect.left + dx >= 0 && cropRect.right + dx <= getWidth()
                && cropRect.top + dy >= 0 && cropRect.bottom + dy <= getHeight()) {
            cropRect.offset(dx, dy);
        }
    }

    private void adjustRectSize() {
        int dx = (int) (end.x - start.x);
        int dy = (int) (end.y - start.y);
        switch (corner) {
            case 0:
                cropRect.left += dx;
                cropRect.top += dy;
                break;
            case 1:
                cropRect.right += dx;
                cropRect.top += dy;
                break;
            case 2:
                cropRect.left += dx;
                cropRect.bottom += dy;
                break;
            case 3:
                cropRect.right += dx;
                cropRect.bottom += dy;
                break;
        }
        int minSize = 50;
        if (cropRect.left < 0) {
            cropRect.left = 0;
        }
        if (cropRect.right > getWidth()) {
            cropRect.right = getWidth();
        }
        if (cropRect.top < 0) {
            cropRect.top = 0;
        }
        if (cropRect.bottom > getHeight()) {
            cropRect.bottom = getHeight();
        }
        if (cropRect.width() < minSize) {
            if (corner == 0 || corner == 2) {
                cropRect.left = cropRect.right - minSize;
            } else {
                cropRect.right = cropRect.left + minSize;
            }
        }
        if (cropRect.height() < minSize) {
            if (corner == 0 || corner == 1) {
                cropRect.top = cropRect.bottom - minSize;
            } else {
                cropRect.bottom = cropRect.top + minSize;
            }
        }
    }

    public void rotateImage(float degrees) {
        Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap originalBitmap = bitmapDrawable.getBitmap();

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            setImageBitmap(rotatedBitmap);

            // Update cropRect after rotation
            RectF newCropRect = new RectF(0, 0, rotatedBitmap.getWidth(), rotatedBitmap.getHeight());
            getImageMatrix().mapRect(newCropRect);
            cropRect.set(newCropRect);

            // Redraw the view
            invalidate();
        }
    }


    public byte[] getCroppedImage() {
        BitmapDrawable drawable = (BitmapDrawable) getDrawable();
        if (drawable != null) {
            float[] matrixValues = new float[9];
            getImageMatrix().getValues(matrixValues);
            float scale = matrixValues[Matrix.MSCALE_X];
            float transX = matrixValues[Matrix.MTRANS_X];
            float transY = matrixValues[Matrix.MTRANS_Y];

            int cropStartX = (int) ((cropRect.left - transX) / scale);
            int cropStartY = (int) ((cropRect.top - transY) / scale);
            int cropWidth = (int) (cropRect.width() / scale);
            int cropHeight = (int) (cropRect.height() / scale);

            cropStartX = Math.max(0, cropStartX);
            cropStartY = Math.max(0, cropStartY);

            Bitmap cropped = Bitmap.createBitmap(drawable.getBitmap(), cropStartX, cropStartY,
                    cropWidth, cropHeight);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            cropped.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            return stream.toByteArray();
        } else {
            return new byte[0];
        }
    }
}*/