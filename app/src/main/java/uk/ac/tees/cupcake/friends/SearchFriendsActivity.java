package uk.ac.tees.cupcake.friends;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import uk.ac.tees.cupcake.R;
import uk.ac.tees.cupcake.adapters.SearchFriendsAdapter;
import uk.ac.tees.cupcake.feed.Post;
import uk.ac.tees.cupcake.navigation.NavigationBarActivity;

public class SearchFriendsActivity extends NavigationBarActivity {
    private static final String TAG = "SearchFriendsActivity";

    private RecyclerView.Adapter adapter;
    @Override
    protected int layoutResource() {
        return R.layout.activity_recycler_view;
    }

    @Override
    public void setup(Bundle savedInstanceState) {

        ArrayList<Post> profiles = new ArrayList<>();
        FirebaseFirestore.getInstance().collection("Users").get().addOnSuccessListener(documentSnapshots -> {

            for (DocumentSnapshot imageSnapShots : documentSnapshots) {
                Post profile = imageSnapShots.toObject(Post.class);
                profile.setUserUid(imageSnapShots.getId());
                profiles.add(profile);
                adapter.notifyDataSetChanged();
            }
        });
        adapter = new SearchFriendsAdapter(profiles, SearchFriendsActivity.this);

        RecyclerView recyclerView = findViewById(R.id.myRecycleView);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setQueryHint("Enter Friends name");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ((SearchFriendsAdapter) adapter).getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }
}
