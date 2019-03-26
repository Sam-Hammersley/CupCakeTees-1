package uk.ac.tees.cupcake.home.health.heartrate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.tees.cupcake.R;
import uk.ac.tees.cupcake.home.MainActivity;
import uk.ac.tees.cupcake.utils.IntentUtils;
import uk.ac.tees.cupcake.utils.views.CheckableLinearViewGroup;
import uk.ac.tees.cupcake.utils.views.GraphViewUtility;
import uk.ac.tees.cupcake.utils.views.OneSelectedOnCheckStrategy;

public final class SaveHeartRateActivity extends AppCompatActivity {
    
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    
    private CheckableLinearViewGroup measurementTypes;
    
    private LineChart graph;

    private final Map<String, List<Entry>> cachedGraphingData = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_heart_rate);
    
        graph = findViewById(R.id.heart_rate_chart);
        GraphViewUtility.setupLineChart(graph);
    
        TextView measurementTextView = findViewById(R.id.save_heart_rate_bpm);
        HeartRateMeasurement measurement = (HeartRateMeasurement) getIntent().getSerializableExtra("heart_rate_measurement");
        measurementTextView.setText(Integer.toString(measurement.getBpm()));
    
        measurementTypes = findViewById(R.id.measurement_types);
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce_anim);
        measurementTypes.startAnimation(bounce);
        getData().addOnCompleteListener(e -> measurementTypes.setChecked(R.id.general_measurement_type, true));
        measurementTypes.addOnCheckStrategy(new OneSelectedOnCheckStrategy());
        measurementTypes.addOnCheckStrategy((id, v) -> {
            String resourceId = getResources().getResourceName(id);
            String typeName = resourceId.substring(resourceId.indexOf("/") + 1, resourceId.indexOf("_"));
            applyGraphData(cachedGraphingData.get(typeName));
        });
    }
    
    /**
     * Takes the average of the given data set and then sets the text value of the appropriate {@link TextView}.
     */
    private void applyGraphData(List<Entry> entries) {
        TextView graphText = findViewById(R.id.graph_heart_rate);
        
        float average = 0;
        for (Entry e : entries) {
            average += e.getY();
        }
        
        String averageValue = Integer.toString(Math.round(average / entries.size()));
        graphText.setText(averageValue);
    
        GraphViewUtility.setChartData(graph, "#FF0000", entries);
        
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1000);
        graph.startAnimation(fadeIn);
    }
    
    /**
     * Gets the heart rate data to plot in the graph of previous heart rate measurementTypes.
     *
     * @return a {@link List} of {@link Entry} to be plot in a graph.
     */
    private Task<QuerySnapshot> getData() {
        return firestore.collection("UserStats")
                .document(auth.getCurrentUser().getUid())
                .collection("HeartRates")
                .get()
                .addOnSuccessListener(documentSnapshots -> {
                    final int childCount = measurementTypes.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        int type = measurementTypes.getChildAt(i).getId();
                        
                        String resourceId = getResources().getResourceName(type);
                        String typeName = resourceId.substring(resourceId.indexOf("/") + 1, resourceId.indexOf("_"));
                        cachedGraphingData.put(typeName, new ArrayList<>());
                    }
                    
                    List<HeartRateMeasurement> measurements = documentSnapshots.toObjects(HeartRateMeasurement.class);
                    for (int i = 0; i < measurements.size(); i++) {
                        List<Entry> entries = cachedGraphingData.get(measurements.get(i).getMeasurementType());
                        entries.add(new Entry(i, measurements.get(i).getBpm()));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SaveHeartRateActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    /**
     * Invoked upon pressing the save button.
     * @param view
     */
    public void saveMeasurement(View view) {
        Intent intent = getIntent();
        HeartRateMeasurement measurement = (HeartRateMeasurement) intent.getSerializableExtra("heart_rate_measurement");
        
        String resourceId = getResources().getResourceName(measurementTypes.getChecked().get(0));
        measurement.setMeasurementType(resourceId.substring(resourceId.indexOf("/") + 1, resourceId.indexOf("_")));
        
        firestore.collection("UserStats")
                .document(auth.getCurrentUser().getUid())
                .collection("HeartRates")
                .add(measurement)
                .addOnSuccessListener(doc -> {
                    IntentUtils.invokeBaseView(SaveHeartRateActivity.this, MainActivity.class);
                    finish();
                })
                .addOnFailureListener(doc -> Toast.makeText(SaveHeartRateActivity.this, doc.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    /**
     * Invoked upon pressing the cancel button.
     * @param view
     */
    public void cancelMeasurement(View view) {
        IntentUtils.invokeBaseView(SaveHeartRateActivity.this, MainActivity.class);
        finish();
    }
}