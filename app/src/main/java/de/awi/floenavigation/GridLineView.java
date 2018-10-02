package de.awi.floenavigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class GridLineView extends View {

    private static final int DEFAULT_PAINT_COLOR = Color.BLACK;
    private static final int DEFAULT_NUMBER_OF_ROWS = 20;
    private static final int DEFAULT_NUMBER_OF_COLUMNS = 20;

    private boolean showGrid = true;
    private final Paint paint = new Paint();
    private int numRows = DEFAULT_NUMBER_OF_ROWS, numColumns = DEFAULT_NUMBER_OF_COLUMNS;
    private Rect mRectSquare;
    private float[] results;
    private int coordRowNum = 10, coordColNum = 15;

    public GridLineView(Context context) {
        super(context);
        init();
    }

    public GridLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridLineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setColor(DEFAULT_PAINT_COLOR);
        mRectSquare = new Rect();
    }

    public void setLineColor(int color) {
        paint.setColor(color);
    }

    public void setInitialCoordinates(double[] macceptcoord){

        //android.location.Location.distanceBetween(macceptcoord.get(0), macceptcoord.get(1), macceptcoord.get(2), macceptcoord.get(3),results);
        /*
        if(macceptcoord.get(0) == macceptcoord.get(2)){
            coordRowNum = 15;
            coordColNum = 15;
        }*/

        coordRowNum = 15;
        coordColNum = 15;
        postInvalidate();
    }

    public void setStrokeWidth(int pixels) {
        paint.setStrokeWidth(pixels);
    }

    public int getNumberOfRows() {
        return numRows;
    }

    public void setNumberOfRows(int numRows) {
        if (numRows > 0) {
            this.numRows = numRows;
        } else {
            throw new RuntimeException("Cannot have a negative number of rows");
        }
    }

    public int getNumberOfColumns() {
        return numColumns;
    }



    public void setNumberOfColumns(int numColumns) {
        if (numColumns > 0) {
            this.numColumns = numColumns;
        } else {
            throw new RuntimeException("Cannot have a negative number of columns");
        }
    }

    public boolean isGridShown() {
        return showGrid;
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (showGrid)
        {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            /*
            mRectSquare.left = (width * 10 / numColumns)-15;
            mRectSquare.right = (width * 10 / numColumns)+15;
            mRectSquare.top = (height * 10 / numRows)-15;
            mRectSquare.bottom = (height * 10 / numRows)+15;

            int dx = (width * 15 / numColumns) - (width * 10 / numColumns);
            int dy = (height * 15 / numRows) - (height * 18 / numRows);
            int ox = -dy;//(width * 10 / numColumns) + (dx - dy) / 2;
            int oy = dx;//(height * 18 / numRows) + (dx + dy) / 2;
            int a = width * 10 / numColumns;
            int b = height * 18 / numRows;
            */

            // Vertical lines
            for (int i = 1; i < numColumns; i++) {
                canvas.drawLine(width * i / numColumns, 0, width * i / numColumns, height, paint);
            }

            // Horizontal lines
            for (int i = 1; i < numRows; i++) {
                canvas.drawLine(0, height * i / numRows, width, height * i / numRows, paint);
            }

            setLineColor(Color.GREEN);
            canvas.drawCircle(width * 10 / numColumns, height * 15 / numRows, 15, paint);
            canvas.drawCircle(width * coordRowNum / numColumns, height * coordColNum / numRows, 15, paint);


            setLineColor(Color.RED);
            canvas.drawLine(width * 10 / numColumns,height * 15 / numRows,width * coordRowNum / numColumns,
                    height * coordColNum / numRows, paint);

            setLineColor(Color.BLACK);
            /*

            //canvas.drawPoint(width * 10 / numColumns, height * 10 / numRows, paint);
            canvas.drawRect(mRectSquare, paint);

            canvas.drawLine(a, b, a-ox, b-oy, paint);


            //setLineColor(Color.GREEN);
            drawTriangle((width * 5 / numColumns)-15, (height * 5 / numRows)+15, 30, 30, false, paint, canvas);
            drawStar((width * 1 / numColumns)*2, 80, paint, canvas);

            //canvas.rotate(45, width * 10 / numColumns, height * 18 / numRows);
            //
            */

        }
    }

    private void drawTriangle(int x, int y, int width, int height, boolean inverted, Paint paint, Canvas canvas){

        Point p1 = new Point(x,y);
        int pointX = x + width/2;
        int pointY = inverted?  y + height : y - height;

        Point p2 = new Point(pointX,pointY);
        Point p3 = new Point(x+width,y);


        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(p1.x,p1.y);
        path.lineTo(p2.x,p2.y);
        path.lineTo(p3.x,p3.y);
        path.close();

        canvas.drawPath(path, paint);
    }

    private void drawStar(int width, int height, Paint paint, Canvas canvas)
    {
        float mid = width / 2;
        float min = Math.min(width, height);
        float fat = min / 17;
        float half = min / 2;
        mid = mid - half;

        paint.setStrokeWidth(fat);
        paint.setStyle(Paint.Style.STROKE);
        Path path = new Path();


        path.reset();

        paint.setStyle(Paint.Style.FILL);


        // top left
        path.moveTo(mid + half * 0.5f, half * 0.84f);
        // top right
        path.lineTo(mid + half * 1.5f, half * 0.84f);
        // bottom left
        path.lineTo(mid + half * 0.68f, half * 1.45f);
        // top tip
        path.lineTo(mid + half * 1.0f, half * 0.5f);
        // bottom right
        path.lineTo(mid + half * 1.32f, half * 1.45f);
        // top left
        path.lineTo(mid + half * 0.5f, half * 0.84f);

        path.close();
        canvas.drawPath(path, paint);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean value = super.onTouchEvent(event);

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN: {

                float x = event.getX();
                float y = event.getY();

                if(mRectSquare.left < x && mRectSquare.right > x)
                    if(mRectSquare.top < y && mRectSquare.bottom > y){
                        Log.d("touched", "Rectangle: " + x + "," + y);
                        //postInvalidate();
                    }

                return true;
            }

        }
        return value;
    }


}

