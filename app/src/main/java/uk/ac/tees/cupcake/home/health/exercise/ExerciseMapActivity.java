package uk.ac.tees.cupcake.home.health.exercise;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.concurrent.TimeUnit;

import uk.ac.tees.cupcake.ApplicationConstants;
import uk.ac.tees.cupcake.R;
import uk.ac.tees.cupcake.utils.HealthUtility;

public class ExerciseMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final int LOCATION_UPDATE_INTERVAL = 100;
    
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    
    /**
     * The {@link GoogleMap} to display route and current location.
     */
    private GoogleMap googleMap;
    
    /**
     * Provides location updates.
     */
    private FusedLocationProviderClient locationProviderClient;
    
    /**
     * For tracking the exercise route.
     */
    private ExerciseRouteTracker exerciseRouteTracker;
    
    /**
     * The type of exercise.
     */
    private Exercise currentExercise;
    
    /**
     * Denotes whether the user has permissions.
     */
    private boolean hasPermissions;
    
    /**
     * Chronometer used for timing the workout.
     */
    private Chronometer chronometer;
    
    private TextView paceTextView;
    private TextView caloriesTextView;
    private TextView exerciseTextView;
    
    private float userBmr;
    
    private long caloriesBurned;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.activity_exercise_map);
    
        if (ActivityCompat.checkSelfPermission(ExerciseMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasPermissions = true;
        } else {
            ActivityCompat.requestPermissions(ExerciseMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    
        locationProviderClient = LocationServices.getFusedLocationProviderClient(ExerciseMapActivity.this);
        currentExercise = (Exercise) getIntent().getSerializableExtra("exercise");
        
        chronometer = findViewById(R.id.exercise_map_chronometer);
        paceTextView = findViewById(R.id.exercise_map_current_pace);
        caloriesTextView = findViewById(R.id.exercise_map_calories_burned);
    
        exerciseTextView = findViewById(R.id.exercise_map_exercise_label);
        exerciseTextView.setText(currentExercise.getName());
    
        userBmr = getSharedPreferences(ApplicationConstants.PREFERENCES_NAME, Context.MODE_PRIVATE).getFloat("user_bmr", 0);
    
        startChronometer(0);
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.exercise_map_map);
        mapFragment.getMapAsync(ExerciseMapActivity.this);
    }
    
    /**
     * Starts periodic location requests.
     *
     * @throws SecurityException if necessary permissions are not granted.
     */
    private void requestLocationUpdates() throws SecurityException {
        LocationRequest locationRequest = buildLocationRequest(LOCATION_UPDATE_INTERVAL, LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationProviderClient.requestLocationUpdates(locationRequest, exerciseRouteTracker, locationProviderClient.getLooper());
    }
    
    /**
     * Adds appropriate listener to and starts {@link #chronometer}
     */
    private void startChronometer(long base) {
        chronometer.setBase(SystemClock.elapsedRealtime() + base);
    
        chronometer.setOnChronometerTickListener(c -> {
        
            float distance = exerciseRouteTracker.getDistanceTravelled();
            long elapsedMillis = SystemClock.elapsedRealtime() - c.getBase();
        
            long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);
            if (seconds > 0) {
                String pace = String.format("%.2f", (distance / seconds));
                paceTextView.setText("Current pace: " + pace + " m/s");
            }
        
            caloriesBurned = HealthUtility.calculateCaloriesBurnedMovement(userBmr, distance, elapsedMillis);
            caloriesTextView.setText("Calories burned: " + caloriesBurned + " kcal");
        
            exerciseTextView.setText(currentExercise.getName() + " for " + distance / 1000 + "km");
        });
        
        chronometer.start();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        hasPermissions = false;
        
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                }
                break;
        }
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.exerciseRouteTracker = new ExerciseRouteTracker(googleMap, currentExercise.getMaxDisplacement());
        
        try {
            if (hasPermissions) {
                googleMap.getUiSettings().setAllGesturesEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(ExerciseMapActivity.this, R.raw.exercise_map_style));
                googleMap.setMyLocationEnabled(false);
    
                locationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                    requestLocationUpdates();
                });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
    
        locationProviderClient.removeLocationUpdates(exerciseRouteTracker);
    }
    
    /**
     * Builds a {@link LocationRequest} with the given interval, accuracy and minimum displacement.
     *
     * @return a {@link LocationRequest}
     */
    private LocationRequest buildLocationRequest(int interval, int accuracy) {
        return new LocationRequest()
                .setInterval(interval)
                .setPriority(accuracy)
                .setSmallestDisplacement(0);
    }
    
}