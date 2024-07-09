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

import java.util.ArrayList;

public class GameView extends View {
    private static final String TAG = "GameView";

    private static final int[] PLAYER_COLORS = {
        0xff404040,  // beige (neutral)
        0xffe00000,  // red (player 1)
        0xff0040c0,  // blue (player 2)
    };

    public interface FieldClickListener {
        void onFieldClick(BoardGeometry.Vertex v);
    }

    private GameStateWithSelection state = GameStateWithSelection.DUMMY_GAME_STATE_WITH_SELECTION;

    private final Matrix viewMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    // Some temporary paint objects that are used in draw() to avoid allocations (which triggers
    // a warning, even though it is probably not a big deal). Usually, paint1 is used to stroke,
    // and paint2 to fill.
    private final Paint paint1 = new Paint();
    private final Paint paint2 = new Paint();

    private final ArrayList<FieldClickListener> fieldClickListeners = new ArrayList<>();

    private final OnTouchListener touchListener =
        (View view, MotionEvent event) -> {
            final BoardGeometry geometry = state.gameState.getGeometry();;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] xy = new float[]{event.getX(), event.getY()};
                    inverseMatrix.mapPoints(xy);
                    float x = xy[0], y = xy[1];
                    // This could be optimized... but it's probably not necessary.
                    for (BoardGeometry.Vertex v : geometry.vertices) {
                        if (Math.hypot(v.x - x, v.y - y) < 0.5f / geometry.boardSize) {
                            for (FieldClickListener listener : fieldClickListeners) {
                                try {
                                    listener.onFieldClick(v);
                                } catch (Exception e) {
                                    Log.w(TAG, "FieldClickListener threw exception!", e);
                                }
                            }
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

    public void addFieldClickListener(FieldClickListener listener) {
        fieldClickListeners.add(listener);
    }

    public void removeFieldClickListener(FieldClickListener listener) {
        fieldClickListeners.remove(listener);
    }

    public void setGameState(GameStateWithSelection state) {
        if (this.state.equals(state)) return;
        this.state = state;
        invalidate();
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

        final BoardGeometry geometry = state.gameState.getGeometry();;

        paint1.reset();
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setStrokeWidth(0.075f / geometry.boardSize);
        paint1.setStrokeCap(Paint.Cap.ROUND);
        for (BoardGeometry.Edge e : geometry.edges) {
            int p1 = state.gameState.getPiece(e.v);
            int p2 = state.gameState.getPiece(e.w);
            paint1.setColor(PLAYER_COLORS[p1 == p2 ? p1 : 0]);
            canvas.drawLine(e.v.x, e.v.y, e.w.x, e.w.y, paint1);
        }

        paint2.reset();
        paint2.setStyle(Paint.Style.FILL);
        for (BoardGeometry.Vertex v : geometry.vertices) {
            int player = state.gameState.getPiece(v);
            paint2.setColor(PLAYER_COLORS[player]);
            canvas.drawCircle(v.x, v.y, (player == 0 ? 0.167f : 0.33f) / geometry.boardSize, paint2);

            if (v.equals(state.selection)) {
                paint1.reset();
                paint1.setStyle(Paint.Style.STROKE);
                paint1.setStrokeWidth(0.167f / geometry.boardSize);
                paint1.setColor(PLAYER_COLORS[state.gameState.getNextPlayer()]);
                canvas.drawCircle(v.x, v.y, 0.25f / geometry.boardSize, paint1);
            }
        }

        BoardGeometry.Vertex lastMovePos = state.gameState.getLastMove();
        if (lastMovePos != null) {
            paint2.reset();
            paint2.setStyle(Paint.Style.FILL);
            paint2.setColor(0xffffffff);
            float d = 0.1f / geometry.boardSize;
            canvas.drawRect(
                    lastMovePos.x - d, lastMovePos.y - d,
                    lastMovePos.x + d, lastMovePos.y + d,
                    paint2);
        }

        // Draw corners that have been captured.
        paint1.reset();
        paint1.setStyle(Paint.Style.STROKE);
        for (int corner = 0; corner < geometry.sides; ++corner) {
            int player = state.gameState.getCornerWinner(corner);
            paint1.setStrokeWidth((player == 0 ? 0.1f : 0.2f) / geometry.boardSize);
            paint1.setColor(PLAYER_COLORS[player]);
            float sweepAngle = 360f / geometry.sides * 0.8f;
            float startAngle = 360f * corner / geometry.sides - sweepAngle/2 - 90;
            canvas.drawArc(-0.95f, -0.95f, 0.95f, 0.95f,  startAngle, sweepAngle, false, paint1);
        }
    }
}
