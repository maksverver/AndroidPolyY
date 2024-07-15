package ch.verver.poly_y;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class OldMainActivity extends Activity {
    private static final String TAG = "OldMainActivity";

    private GameRegistry gameRegistry;
    private TextView currentLevelTextView;
    private Button startCampaignGameButton;
    private Switch pieRuleSwitch;
    private RadioButton aiPlayerFirstButton;
    private RadioButton aiPlayerSecondButton;
    private NumberPicker difficultyPicker;
    private Switch openingBookSwitch;
    private Button startCustomGameButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameRegistry = GameRegistry.getInstance(this);

        // Check if there is a game to resume.
        GameState currentGameState = gameRegistry.getCurrentGameState();
        if (currentGameState != null && !currentGameState.isGameOver()) {
            switchToGameActivity();
            return;
        }

        // TODO: this doesn't seem to work.
        // Force night-mode, which looks more consistent with the GameActivity.
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setContentView(R.layout.old_main_layout);

        currentLevelTextView = findViewById(R.id.currentLevelTextView);
        currentLevelTextView.setText(
                getString(R.string.currentLevelTextFormat, gameRegistry.getCampaignLevel(), gameRegistry.getCampaignDifficulty()));
        startCampaignGameButton = findViewById(R.id.startCampaignGameButton);
        startCampaignGameButton.setOnClickListener(this::onStartCampaignButtonClicked);
        pieRuleSwitch = findViewById(R.id.pieRuleSwitch);
        aiPlayerFirstButton = findViewById(R.id.aiPlayerFirstButton);
        aiPlayerSecondButton = findViewById(R.id.aiPlayerSecondButton);
        aiPlayerSecondButton.setChecked(true);
        openingBookSwitch = findViewById(R.id.openingBookSwitch);
        difficultyPicker = findViewById(R.id.difficultyPicker);
        difficultyPicker.setMinValue(AiConfig.MIN_DIFFICULTY);
        difficultyPicker.setMaxValue(AiConfig.MAX_DIFFICULTY);
        difficultyPicker.setValue(AiConfig.MEDIUM_DIFFICULTY);
        startCustomGameButton = findViewById(R.id.startCustomGameButton);
        startCustomGameButton.setOnClickListener(this::onStartCustomGameButtonClick);
    }

    private void onStartCampaignButtonClicked(View unused) {
        int difficulty = gameRegistry.getCampaignDifficulty();
        int aiPlayer = gameRegistry.getCampaignAiPlayer();
        Log.i(TAG, "Starting campaign game with aiPlayer=" + aiPlayer + " difficulty="+ difficulty);
        gameRegistry.startGame(GameState.DEFAULT_GAME_STATE, aiPlayer, AiConfig.fromDifficulty(difficulty), true);
        switchToGameActivity();
    }

    private void onStartCustomGameButtonClick(View unused) {
        boolean pieRule = pieRuleSwitch.isChecked();
        GameState gameState = GameState.calculate(BoardGeometry.DEFAULT_GEOMETRY, pieRule);
        int aiPlayer = aiPlayerFirstButton.isChecked() ? 1 : aiPlayerSecondButton.isChecked() ? 2 : 0;
        int difficulty = difficultyPicker.getValue();
        boolean openingBook = openingBookSwitch.isChecked();
        AiConfig aiConfig = AiConfig.fromDifficulty(difficulty, openingBook);
        Log.i(TAG, "Starting custom game with pieRule=" + pieRule + " aiPlayer=" + aiPlayer + " difficulty="+ difficulty + " openingBook=" + openingBook);
        gameRegistry.startGame(gameState, aiPlayer, aiConfig, false);
        switchToGameActivity();
    }

    private void switchToGameActivity() {
        startActivity(new Intent(this, GameActivity.class));
        finish();
    }
}
