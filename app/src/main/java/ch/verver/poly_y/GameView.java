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

    private final Paint vertexFill = new Paint();
    private final Paint edgeStroke = new Paint();
    private final Paint selectedVertexPaint = new Paint();
    private final Paint lastMoveMarkerPaint = new Paint();

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

    public boolean removeFieldClickListener(FieldClickListener listener) {
        return fieldClickListeners.remove(listener);
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

        edgeStroke.reset();
        edgeStroke.setStyle(Paint.Style.STROKE);
        edgeStroke.setStrokeWidth(0.075f / geometry.boardSize);
        edgeStroke.setStrokeCap(Paint.Cap.ROUND);
        for (BoardGeometry.Edge e : geometry.edges) {
            int p1 = state.gameState.getPiece(e.v);
            int p2 = state.gameState.getPiece(e.w);
            edgeStroke.setColor(PLAYER_COLORS[p1 == p2 ? p1 : 0]);
            canvas.drawLine(e.v.x, e.v.y, e.w.x, e.w.y, edgeStroke);
        }

        vertexFill.reset();
        vertexFill.setStyle(Paint.Style.FILL);
        for (BoardGeometry.Vertex v : geometry.vertices) {
            int player = state.gameState.getPiece(v);
            vertexFill.setColor(PLAYER_COLORS[player]);
            canvas.drawCircle(v.x, v.y, (player == 0 ? 0.167f : 0.33f) / geometry.boardSize, vertexFill);

            if (v.equals(state.selection)) {
                selectedVertexPaint.reset();
                selectedVertexPaint.setColor(PLAYER_COLORS[state.gameState.getNextPlayer()]);
                selectedVertexPaint.setStyle(Paint.Style.STROKE);
                selectedVertexPaint.setStrokeWidth(0.167f / geometry.boardSize);
                canvas.drawCircle(v.x, v.y, 0.25f / geometry.boardSize, selectedVertexPaint);
            }
        }

        BoardGeometry.Vertex lastMovePos = state.gameState.getLastMove();
        if (lastMovePos != null) {
            lastMoveMarkerPaint.reset();
            lastMoveMarkerPaint.setStyle(Paint.Style.FILL);
            lastMoveMarkerPaint.setColor(0xffffffff);
            float d = 0.1f / geometry.boardSize;
            canvas.drawRect(
                    lastMovePos.x - d, lastMovePos.y - d,
                    lastMovePos.x + d, lastMovePos.y + d,
                    lastMoveMarkerPaint);
        }
    }
}
