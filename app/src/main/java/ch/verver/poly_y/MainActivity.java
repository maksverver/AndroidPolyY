package ch.verver.poly_y;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

/**
 * Activity which implements the app's home screen, where the player can start a new game.
 *
 * <p>It is essentially just a {@link WebView} that serves static content from the subdirectory:
 * app/src/main/assets/www/, but with the {@link AppApi} that allows Javascript code to interact
 * with the activity, to start new games and so on.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private GameRegistry gameRegistry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameRegistry = GameRegistry.getInstance(this);

        // Check if there is a game to resume. If so, switch to the GameActivity immediately.
        GameState currentGameState = gameRegistry.getCurrentGameState();
        if (currentGameState != null && !currentGameState.isGameOver()) {
            switchToGameActivity();
            return;
        }

        setContentView(new AppView(this));
    }

    private void startCampaignGame() {
        int difficulty = gameRegistry.getCampaignDifficulty();
        int aiPlayer = gameRegistry.getCampaignAiPlayer();
        Log.i(TAG, "Starting campaign game with aiPlayer=" + aiPlayer + " difficulty="+ difficulty);
        gameRegistry.startGame(GameState.DEFAULT_GAME_STATE, aiPlayer, AiConfig.fromDifficulty(difficulty), true);
        switchToGameActivity();
    }

    private void startCustomGame() {
        Log.i(TAG, "Custom game button clicked! TODO: start custom game");
        /*
        boolean pieRule = pieRuleSwitch.isChecked();
        GameState gameState = GameState.calculate(BoardGeometry.DEFAULT_GEOMETRY, pieRule);
        int aiPlayer = aiPlayerFirstButton.isChecked() ? 1 : aiPlayerSecondButton.isChecked() ? 2 : 0;
        int difficulty = difficultyPicker.getValue();
        boolean openingBook = openingBookSwitch.isChecked();
        AiConfig aiConfig = AiConfig.fromDifficulty(difficulty, openingBook);
        Log.i(TAG, "Starting custom game with pieRule=" + pieRule + " aiPlayer=" + aiPlayer + " difficulty="+ difficulty + " openingBook=" + openingBook);
        gameRegistry.startGame(gameState, aiPlayer, aiConfig, false);
        switchToGameActivity();
        */
    }

    private void switchToGameActivity() {
        startActivity(new Intent(this, GameActivity.class));
        finish();
    }

    class AppView extends WebView {
        private static final String indexUrl = "https://" + WebViewAssetLoader.DEFAULT_DOMAIN + "/assets/www/index.html";

        @SuppressWarnings("SetJavascriptEnabled")
        public AppView(@NonNull Context context) {
            super(context);
            final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(context))
                    .addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(context))  // not currently used?
                    .build();
            setWebViewClient(new WebViewClientCompat() {
                @Override
                @Nullable
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                }

                @Override
                public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
                    // Open external links in the default browser, instead of the WebView.
                    //
                    // This has several advantages:
                    //
                    //  - the app doesn't need internet access
                    //  - the browser is more user-friendly
                    //  - external sites won't have access to the `app` Javascript object
                    Uri uri = request.getUrl();
                    if (request.isForMainFrame() && uri.isAbsolute() &&
                            !WebViewAssetLoader.DEFAULT_DOMAIN.equals(uri.getHost())) {
                        // Start browser activity.
                        startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                        return true;
                    }
                    return false;
                }
            });
            WebSettings webSettings = getSettings();
            webSettings.setJavaScriptEnabled(true);
            addJavascriptInterface(new AppApi(), "app");
            loadUrl(indexUrl);
        }
    }

    /**
     * Provides an API to the Javascript code running in the WebView.
     *
     * <p>IMPORTANT: these methods are called on a background thread!
     */
    class AppApi {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public int getCampaignLevel() {
            return gameRegistry.getCampaignLevel();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public int getCampaignDifficulty() {
            return gameRegistry.getCampaignDifficulty();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public int getCampaignAiPlayer() {
            return gameRegistry.getCampaignAiPlayer();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void startCampaignGame() {
            MainActivity.this.startCampaignGame();
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void startCustomGame() {
            MainActivity.this.startCustomGame();
        }
    }
}
