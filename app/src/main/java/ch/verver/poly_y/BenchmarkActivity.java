package ch.verver.poly_y;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ch.verver.poly_y.ai.TreeBot;

public class BenchmarkActivity extends Activity {
    private static final String DURATION_MS_KEY = "duration_ms";
    private static final long DEFAULT_DURATION_MS = 5000;

    private TextView text;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.text = new TextView(this);
        text.setText("AI Benchmark\n");
        setContentView(text);

        final long durationMs = getDurationMs();
        text.setText(text.getText() + "Duration: " + (durationMs * 1e-3) + " seconds\n");

        final Handler mainHandler = new Handler(getMainLooper());
        new Thread(() -> {
            double expansionsPerSecond = new TreeBot().benchmark(durationMs);
            mainHandler.post(() -> {
                text.setText(text.getText() + "Result: " + Math.round(expansionsPerSecond) + " expansions/second\n");
            });
        }, "AI Benchmark").start();;
    }

    private long getDurationMs() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            long durationMs = extras.getLong(DURATION_MS_KEY);
            if (durationMs > 0) return durationMs;
        }
        return DEFAULT_DURATION_MS;
    }
}
