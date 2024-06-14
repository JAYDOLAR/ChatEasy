package CustomViews;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chateasy.R;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import Utility.LoggerUtil;


public class CustomCropViewHandler extends AppCompatActivity {

    private CustomCropView customCropView;
    private Button doneButton, rotateImg, Set_As_It;
    private Intent intent;
    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_crop_view);

        customCropView = findViewById(R.id.customCropView);
        doneButton = findViewById(R.id.doneButton);
        rotateImg = findViewById(R.id.rotateImg);
        Set_As_It = findViewById(R.id.Set_As_It);

        intent = getIntent();
        if (intent != null) {
            String imageUriString = intent.getStringExtra("imageUri");
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString);
                customCropView.setImageUri(String.valueOf(imageUri));
            }
        }

        Set_As_It.setOnClickListener(v -> {
            intent = new Intent();
            assert imageUri != null;
            intent.putExtra("croppedImageUri", imageUri.toString());
            setResult(RESULT_OK, intent);
            finish();
        });

        doneButton.setOnClickListener(v -> {

            byte[] croppedImage = customCropView.getCroppedImage();

            Bitmap croppedBitmap = BitmapFactory.decodeByteArray(croppedImage, 0, croppedImage.length);

            System.out.println(croppedBitmap);
            if (croppedBitmap != null && !croppedBitmap.isRecycled()) {
                finishWithResult(croppedBitmap);
            } else {
                // Handle the case where the croppedBitmap is null or recycled
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        rotateImg.setOnClickListener(v -> customCropView.rotateImage(90));
    }

    private void finishWithResult(Bitmap croppedBitmap) {
        // Convert the Bitmap to a Uri
        Uri croppedImageUri = saveBitmapAsImageFile(croppedBitmap);

        intent = new Intent();
        assert croppedImageUri != null;
        intent.putExtra("croppedImageUri", croppedImageUri.toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    // Save the Bitmap as an image file and return the Uri
    @Nullable
    private Uri saveBitmapAsImageFile(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Title");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        // API 29 and later, use relative path to DCIM directory
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/ChatEase");

        // Get the content resolver and insert the image
        ContentResolver resolver = getContentResolver();
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Open an output stream to write the bitmap data into the content resolver
        try (OutputStream outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri))) {
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                outputStream.close();
                return imageUri;
            }
        } catch (IOException e) {
            LoggerUtil.logError("Image is not save: ", e);
        }

        return null;
    }
}
