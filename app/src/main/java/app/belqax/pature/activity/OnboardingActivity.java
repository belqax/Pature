package app.belqax.pature.activity;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.adapter.StepsOnboardingAdapter;
import app.belqax.pature.helper.SmoothScrollerHelper;
import app.belqax.pature.infc.StepActionListener;
import app.belqax.pature.model.Step;

public class OnboardingActivity extends AppCompatActivity implements StepActionListener {

    private RecyclerView stepsList;
    private ProgressBar progressBar;
    private ImageView backButton;

    private StepsOnboardingAdapter adapter;
    private List<Step> steps;

    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupSteps();
        setupRecycler();
        setupListeners();
        updateProgress();
    }

    private void initViews() {
        stepsList = findViewById(R.id.stepsList);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backButton);
    }

    private void setupSteps() {
        steps = Arrays.asList(
                Step.ANIMAL_TYPE,
                Step.PET_NAME,
                Step.BIRTHDAY,
                Step.PURPOSE
        );
    }

    private void setupRecycler() {
        adapter = new StepsOnboardingAdapter(steps, this);
        stepsList.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );

        stepsList.setAdapter(adapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> goBack());
    }

    private void updateProgress() {
        float stepProgress = (float) (currentStep + 1) / steps.size();
        int percent = (int) (stepProgress * 100);
        animateProgress(progressBar, percent);

    }
    private void animateProgress(ProgressBar bar, int target) {
        ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", bar.getProgress(), target);
        anim.setDuration(400); // скорость
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }

    private void goNext() {
        if (currentStep < steps.size() - 1) {
            currentStep++;
            smoothGoToPosition(currentStep);
            updateProgress();
        } else {
            finishOnboarding();
        }
    }

    private void goBack() {
        if (currentStep > 0) {
            currentStep--;
            smoothGoToPosition(currentStep);
            updateProgress();
        } else {
            finish();
        }
    }

    private void smoothGoToPosition(int position) {
        SmoothScrollerHelper smoothScroller =
                new SmoothScrollerHelper(this);

        smoothScroller.setTargetPosition(position);
        stepsList.getLayoutManager().startSmoothScroll(smoothScroller);
    }


    private void finishOnboarding() {
        // TODO: переход в основное приложение
    }

    @Override
    public void onStepCompleted() {
        goNext();
    }
}
