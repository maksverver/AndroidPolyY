package ch.verver.poly_y;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GameView extends View {
    private static final String TAG = "GameView";

    private final Matrix viewMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    final OnTouchListener touchListener =
        (View v, MotionEvent event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] xy = new float[]{event.getX(), event.getY()};
                    inverseMatrix.mapPoints(xy);
                    if (Math.hypot(xy[0], xy[1]) < 1) {
                        Log.i(TAG, "touched inside");
                    } else {
                        Log.i(TAG, "touched outside");
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP: {
                    performClick();
                    return true;
                }
            }
            return false;
        };

    public GameView(Context context) {
        super(context);
        init(context, null);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setOnTouchListener(touchListener);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        viewMatrix.reset();
        float remainingWidth = width - getPaddingLeft() - getPaddingRight();
        float remainingHeight = height - getPaddingTop() - getPaddingBottom();
        viewMatrix.preTranslate(getPaddingLeft() + 0.5f*remainingWidth, getPaddingTop() + 0.5f*remainingHeight);
        float scale = Math.min(remainingHeight, remainingWidth) * 0.5f;
        viewMatrix.preScale(scale, scale);
        if (!viewMatrix.invert(inverseMatrix)) {
            Log.e(TAG, "view matrix is not invertible somehow‽");
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.concat(viewMatrix);

        final Paint fill = new Paint();
        fill.setColor(0xffffff00);  // yellow
        fill.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, 0, 0.95f, fill);

        final Paint stroke = new Paint();
        stroke.setColor(0xff0000ff);  // blue
        stroke.setStrokeWidth(0.1f);
        stroke.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(0, 0, 0.95f, stroke);
    }
}
