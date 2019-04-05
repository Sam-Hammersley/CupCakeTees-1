package uk.ac.tees.cupcake.home.steps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import uk.ac.tees.cupcake.ApplicationConstants;

/**
 * A {@link BroadcastReceiver} that upon receiving an intent broadcast, resets the daily step count
 * and inserts the data into the firestore database.
 *
 * @author Sam-Hammersley <q5315908@tees.ac.uk>
 */
public class StepCounterResetReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(ApplicationConstants.PREFERENCES_NAME, Context.MODE_PRIVATE);
        
        final int steps = preferences.getInt(ApplicationConstants.STEPS_PREFERENCE_KEY, 0);
    
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("UserStats")
                    .document(user.getUid())
                    .collection("StepCounts")
                    .add(new StepCountMeasurement(calendar.getTime(), steps))
                    .addOnFailureListener(x -> Log.e("StepCounterResetReceive", "onReceive: Failed to add step count to database", x));
        }
    
        context.sendBroadcast(new Intent(ApplicationConstants.STEP_COUNT_BROADCAST_INTENT_ACTION));
        preferences.edit().putInt("steps", 0).apply();
    }
    
}