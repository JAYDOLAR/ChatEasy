package Activitys;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chateasy.R;

public class UserAppSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_app_settings);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardView), this::onApplyWindowInsets);
    }

//    @NonNull
//    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
//        final Insets displayCutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
//        final Insets systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
//        final Insets safeInsets = Insets.of(
//                max(displayCutoutInsets.left, systemBarsInsets.left),
//                max(displayCutoutInsets.top, systemBarsInsets.top),
//                max(displayCutoutInsets.right, systemBarsInsets.right),
//                max(displayCutoutInsets.bottom, systemBarsInsets.bottom)
//        );
//
//        Log.d(getLocalClassName(), "");
//        Log.d(getLocalClassName(), "displayCutoutInsets");
//        Log.d(getLocalClassName(), "top=" + displayCutoutInsets.top);
//        Log.d(getLocalClassName(), "left=" + displayCutoutInsets.left);
//        Log.d(getLocalClassName(), "right=" + displayCutoutInsets.right);
//        Log.d(getLocalClassName(), "bottom=" + displayCutoutInsets.bottom);
//
//        Log.d(getLocalClassName(), "");
//        Log.d(getLocalClassName(), "systemBarsInsets");
//        Log.d(getLocalClassName(), "top=" + systemBarsInsets.top);
//        Log.d(getLocalClassName(), "left=" + systemBarsInsets.left);
//        Log.d(getLocalClassName(), "right=" + systemBarsInsets.right);
//        Log.d(getLocalClassName(), "bottom=" + systemBarsInsets.bottom);
//
//        Log.d(getLocalClassName(), "");
//        Log.d(getLocalClassName(), "safeInsets");
//        Log.d(getLocalClassName(), "top=" + safeInsets.top);
//        Log.d(getLocalClassName(), "left=" + safeInsets.left);
//        Log.d(getLocalClassName(), "right=" + safeInsets.right);
//        Log.d(getLocalClassName(), "bottom=" + safeInsets.bottom);
//        Log.d(getLocalClassName(), "");
//
//        final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
//        mlp.leftMargin = safeInsets.left;
//        mlp.topMargin = safeInsets.top;
//        mlp.bottomMargin = safeInsets.bottom;
//        mlp.rightMargin = safeInsets.right;
//        v.setLayoutParams(mlp);
//
//        return WindowInsetsCompat.CONSUMED;
//    }

}