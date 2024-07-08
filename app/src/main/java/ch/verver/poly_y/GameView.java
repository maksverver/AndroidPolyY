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

    private final BoardGeometry geometry = new BoardGeometry();

    private final Matrix viewMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    private final Paint vertexFill = new Paint();
    private final Paint edgeStroke = new Paint();
    private final Paint selectedVertexFill = new Paint();

    private int selected = -1;

    final OnTouchListener touchListener =
        (View view, MotionEvent event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] xy = new float[]{event.getX(), event.getY()};
                    inverseMatrix.mapPoints(xy);
                    float x = xy[0], y = xy[1];
                    // This could be optimized... but it's probably not necessary.
                    for (BoardGeometry.Vertex v : geometry.vertices) {
                        if (Math.hypot(v.x - x, v.y - y) < 0.5f / geometry.boardSize) {
                            if (v.id == selected) {
                                selected = -1;
                            } else {
                                selected = v.id;
                            }
                            invalidate();
                            return true;
                        }
                    }
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

        int neutralColor = 0xffeeeeaa;  // light yellow

        edgeStroke.reset();
        edgeStroke.setColor(neutralColor);
        edgeStroke.setStyle(Paint.Style.STROKE);
        edgeStroke.setStrokeWidth(0.075f / geometry.boardSize);
        edgeStroke.setStrokeCap(Paint.Cap.ROUND);
        for (BoardGeometry.Edge e : geometry.edges) {
            canvas.drawLine(e.v.x, e.v.y, e.w.x, e.w.y, edgeStroke);
        }

        vertexFill.reset();
        vertexFill.setColor(neutralColor);
        vertexFill.setStyle(Paint.Style.FILL);
        for (BoardGeometry.Vertex v : geometry.vertices) {
            if (v.id == selected) {
                selectedVertexFill.reset();
                selectedVertexFill.setColor(0xffff00ff);  // magenta
                selectedVertexFill.setStyle(Paint.Style.FILL);
                canvas.drawCircle(v.x, v.y, 0.4f / geometry.boardSize, selectedVertexFill);
            } else {
                canvas.drawCircle(v.x, v.y, 0.25f / geometry.boardSize, vertexFill);
            }
        }
    }
}
