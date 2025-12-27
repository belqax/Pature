package app.belqax.pature.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.adapter.MyAnimalsAdapter;
import app.belqax.pature.data.repository.AnimalRepository;

public final class MyAnimalsActivity extends AppCompatActivity {

    private static final String TAG = "MyAnimalsActivity";

    private RecyclerView recycler;
    private FloatingActionButton fab;

    private AnimalRepository repo;
    private ImageButton backButton;
    private MyAnimalsAdapter adapter;

    private final androidx.activity.result.ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadMyAnimals();
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_animals);

        setTitle(getString(R.string.my_animals_title));

        recycler = findViewById(R.id.myAnimalsRecycler);
        fab = findViewById(R.id.myAnimalsFabAdd);
        backButton = findViewById(R.id.myAnimalsBackButton);

        repo = new AnimalRepository();

        adapter = new MyAnimalsAdapter(this);
        adapter.setListener(item -> openEditAnimal(item.id));

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        fab.setOnClickListener(v -> openCreateAnimal());
        backButton.setOnClickListener(v -> finish());

        loadMyAnimals();
    }

    private void loadMyAnimals() {
        repo.listMyAnimals(result -> {
            if (isFinishing()) {
                return;
            }

            if (!result.isSuccess) {
                Log.w(TAG, "listMyAnimals error http=" + result.httpCode + " body=" + result.errorBody);
                Toast.makeText(this, R.string.my_animals_load_error, Toast.LENGTH_LONG).show();
                return;
            }

            List<AnimalRepository.AnimalDto> items = result.data;
            if (items == null) {
                items = java.util.Collections.emptyList();
            }
            adapter.setItems(items);
        });
    }

    private void openCreateAnimal() {
        Intent i = new Intent(this, AnimalFormActivity.class);
        i.putExtra(AnimalFormActivity.EXTRA_MODE, AnimalFormActivity.MODE_CREATE);
        formLauncher.launch(i);
    }

    private void openEditAnimal(long animalId) {
        Intent i = new Intent(this, AnimalFormActivity.class);
        i.putExtra(AnimalFormActivity.EXTRA_MODE, AnimalFormActivity.MODE_EDIT);
        i.putExtra(AnimalFormActivity.EXTRA_ANIMAL_ID, animalId);
        formLauncher.launch(i);
    }
}
