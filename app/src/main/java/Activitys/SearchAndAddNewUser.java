package Activitys;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.BaseProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Adapters.ContactAdapter;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class SearchAndAddNewUser extends AppCompatActivity {
    private static final int REQUEST_READ_CONTACTS = 79;
    private static final String TAG = "SearchAndAddNewUser";
    private final List<String> contactNumbers = new ArrayList<>();
    private FirebaseFirestore db;
    private ContactAdapter adapter; // Declaring the adapter instance
    private Toolbar SearchAddToolbarView;
    /*private EditText open_search_view_edit_text_View;
    private MaterialButton sendToSearchView;*/
    private ImageView user_expanded_image_view;
    private LinearProgressIndicator linearProgressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_and_add_new_user);
        /*FireStoreDatabaseUtils.enableOfflinePersistence(db);*/

        RecyclerView recyclerView = findViewById(R.id.findOutUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SearchAddToolbarView = findViewById(R.id.SearchAddToolbar);
        /*open_search_view_edit_text_View = findViewById(R.id.open_search_view_edit_text);
        sendToSearchView = findViewById(R.id.sendToSearch);
        linearProgressIndicator = findViewById(R.id.updateNewTask);*/
        user_expanded_image_view = findViewById(R.id.user_expanded_image);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        adapter = new ContactAdapter(this, user_expanded_image_view); // Instantiating the adapter
        recyclerView.setAdapter(adapter); // Setting adapter to RecyclerView

        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        } else {
            // Permission already granted, proceed to fetch contacts
            fetchContacts();
        }
        SearchAddToolbarView.setNavigationOnClickListener(v -> finish());
/*        open_search_view_edit_text_View.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int visibility = s.toString().trim().isEmpty() ? View.INVISIBLE : View.VISIBLE;
                sendToSearchView.setVisibility(visibility);
                linearProgressIndicator.setShowAnimationBehavior(BaseProgressIndicator.SHOW_INWARD);
                linearProgressIndicator.setHideAnimationBehavior(BaseProgressIndicator.HIDE_INWARD);
                linearProgressIndicator.setVisibility(visibility);
                sendToSearchView.setOnClickListener(v -> open_search_view_edit_text_View.setText(""));
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int visibility = s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE;
                sendToSearchView.setVisibility(visibility);
                linearProgressIndicator.setVisibility(visibility);
                sendToSearchView.setOnClickListener(v -> open_search_view_edit_text_View.setText(""));

                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) open_search_view_edit_text_View.getLayoutParams();
                float dpValue = 14f;
                float pxValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
                layoutParams.setMarginEnd(visibility == View.VISIBLE ? 0 : (int) pxValue);
                open_search_view_edit_text_View.setLayoutParams(layoutParams);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });*/
    }

    private void fetchContacts() {
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        if (cursor != null) {
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                if (numberIndex != -1) {
                    String number = cursor.getString(numberIndex);
                    contactNumbers.add(number);
                    Log.e(TAG, number);
                } else {
                    Log.e(TAG, "Column index for number is invalid.");
                }
            }
            cursor.close();
            // Now all contact numbers are fetched, let's remove duplicates
            removeDuplicatesIgnoreLastFourDigits();
        } else {
            Log.e(TAG, "Cursor is null.");
        }
    }

    private void removeDuplicatesIgnoreLastFourDigits() {
        Set<String> uniquePrefixes = new HashSet<>();
        List<String> tempList = new ArrayList<>();

        for (String number : contactNumbers) {
            if (number.length() >= 4) { // Check if the length is at least 4
                String prefix = number.substring(0, number.length() - 4); // Extracting prefix
                if (uniquePrefixes.add(prefix)) {
                    tempList.add(number);
                    for (String tempList1 : tempList) {
                        Log.e(TAG, "number : " + tempList1);
                    }
                }
            } else {
                // Handle cases where the length of the number is less than 4
                Log.e(TAG, "Number length is less than 4: " + number);
            }
        }

        contactNumbers.clear();
        contactNumbers.addAll(tempList);
        searchContactsInFirestore();
    }


    private void searchContactsInFirestore() {
        for (final String number : contactNumbers) {
            db.collection("users")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String userPhoneNumber = document.getString("userMobileNumber");
                                if (userPhoneNumber != null && userPhoneNumber.length() >= 4) {
                                    String lastFourDigits = userPhoneNumber.substring(userPhoneNumber.length() - 4);
                                    if (number.endsWith(lastFourDigits)) {
                                        // Document found, add it to the RecyclerView
                                        String userId = document.getId();
                                        if (!isUserAlreadyAdded(userId)) {
                                            // If user not already added, fetch user details
                                            fetchProfileDetail(userId);
                                            Log.e(TAG, userId);
                                        }
                                    }
                                } else {
                                    // Handle cases where userPhoneNumber is null or its length is less than 4
                                    Log.e(TAG, "Invalid userPhoneNumber: " + userPhoneNumber);
                                }
                            }
                        } else {
                            LoggerUtil.logError("Error getting documents: ", task.getException());
                        }
                    });
        }
    }


    private boolean isUserAlreadyAdded(String userId) {
        for (Map<String, Object> contact : adapter.getContacts()) {
            String existingUserId = (String) contact.get("userId");
            if (existingUserId != null && existingUserId.equals(userId)) {
                return true; // User already added
            }
        }
        return false; // User not added yet
    }

    private void fetchProfileDetail(final String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> userData = document.getData();
                            if (userData != null) {
                                // Add the user details to the RecyclerView adapter
                                userData.put("userId", userId); // Add user ID to the data
                                adapter.addUser(userData);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            LoggerUtil.logError("No such document", task.getException());
                        }
                    } else {
                        LoggerUtil.logError("Error getting document", task.getException());
                    }
                });
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch contacts
                fetchContacts();
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
                ValidationUtils.showToast(this, "Permission denied");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
/*
        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
*/
    }

    @Override
    protected void onRestart() {
        super.onRestart();
/*
        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
*/
    }

}
