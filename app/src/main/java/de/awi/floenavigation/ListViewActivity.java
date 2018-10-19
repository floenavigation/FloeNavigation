package de.awi.floenavigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.awi.floenavigation.waypoint.WaypointActivity;

public class ListViewActivity extends ActionBarActivity {

    private static final String TAG = "ListViewActivity";
    private ArrayList<ParameterListObject> parameterObjects = new ArrayList<ParameterListObject>();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        mRecyclerView = findViewById(R.id.parametersListRecyclerView);
        mLayoutManager = new GridLayoutManager(this, 1);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ListViewAdapter(this, generateData());
        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                Log.d(TAG, "Size: " + parameterObjects.size());
                Log.d(TAG, "Pos: " + position);
                mAdapter.notifyDataSetChanged();
                Log.d(TAG, "labelID: " + parameterObjects.get(position).getLabelID());
                deleteEntryfromWaypointsTableinDB(parameterObjects.get(position).getLabelID());
                parameterObjects.remove(position);
                mAdapter.notifyItemRemoved(position);
                mAdapter.notifyItemRangeChanged(position, parameterObjects.size());
                if (parameterObjects.size() == 0){
                    Intent waypointIntent = new Intent(getApplicationContext(), WaypointActivity.class);
                    startActivity(waypointIntent);
                }
                /*
                Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), "Removed from waypoints table", Snackbar.LENGTH_LONG);
                snackbar.setActionTextColor(Color.RED);
                snackbar.show();*/

            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                final int position = viewHolder.getAdapterPosition();
                Bitmap icon;
                Paint p = new Paint();
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if(dX > 0){
                        p.setColor(Color.parseColor("#D32F2F"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                        c.drawRect(background,p);
                        Drawable drawable = getResources().getDrawable(R.drawable.delete);
                        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        //icon = BitmapFactory.decodeResource(getResources(), R.drawable.delete);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                        c.drawBitmap(bitmap,null,icon_dest,p);
                    } else {
                        p.setColor(Color.parseColor("#D32F2F")); //388E3C
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background,p);
                        Drawable drawable = getResources().getDrawable(R.drawable.delete);
                        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        //icon = BitmapFactory.decodeResource(getResources(), R.drawable.delete);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2*width ,(float) itemView.getTop() + width,(float) itemView.getRight() - width,(float)itemView.getBottom() - width);
                        c.drawBitmap(bitmap,null,icon_dest,p);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

        itemTouchHelper.attachToRecyclerView(mRecyclerView);

    }


    private void deleteEntryfromWaypointsTableinDB(String waypointToBeRemoved){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            db.delete(DatabaseHelper.waypointsTable, DatabaseHelper.labelID + " = ?", new String[]{waypointToBeRemoved});
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
    }

    private ArrayList<ParameterListObject> generateData(){
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor waypointsCursor = db.query(DatabaseHelper.waypointsTable,
                    new String[] {DatabaseHelper.labelID, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                    null,
                    null,
                    null, null, null);
            if (waypointsCursor.moveToFirst()) {
                do {
                    String labelID = waypointsCursor.getString(waypointsCursor.getColumnIndex(DatabaseHelper.labelID));
                    double xPosition = waypointsCursor.getDouble(waypointsCursor.getColumnIndex(DatabaseHelper.xPosition));
                    double yPosition = waypointsCursor.getDouble(waypointsCursor.getColumnIndex(DatabaseHelper.yPosition));

                    parameterObjects.add(new ParameterListObject(labelID, xPosition, yPosition));

                } while (waypointsCursor.moveToNext());
            }else {
                Log.d(TAG, "Error reading from waypointstable stn table");
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }


}

class ParameterListObject{

    private String labelID;
    private double xValue;
    private double yValue;

    ParameterListObject(String labelID, double xValue, double yValue){
        this.labelID = labelID;
        this.xValue = xValue;
        this.yValue = yValue;
    }

    public String getLabelID() {
        return labelID;
    }

    public String getxValue() {
        return String.valueOf(xValue);
    }

    public String getyValue() {
        return String.valueOf(yValue);
    }
}

class ListViewAdapter extends RecyclerView.Adapter<ListViewAdapter.ViewHolder> {

    private static final String TAG = "ListViewActivity";
    private ArrayList<ParameterListObject> parameters;
    private Context context;
    private View view;

    ListViewAdapter(ListViewActivity listViewActivity, ArrayList<ParameterListObject> parameterListObjects) {
        this.parameters = parameterListObjects;
    }

    @NonNull
    @Override
    public ListViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "Parameter label: " + parameters.get(position).getLabelID());
        holder.labelIDView.setText(parameters.get(position).getLabelID());
        holder.xPosValue.setText(parameters.get(position).getxValue());
        holder.yPosValue.setText(parameters.get(position).getyValue());
    }


    @Override
    public int getItemCount() {
        return parameters.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView labelIDView;
        TextView xPosValue;
        TextView yPosValue;

        ViewHolder(View view) {
            super(view);
            Log.d(TAG, "Parameter label: " + labelIDView);
            labelIDView = view.findViewById(R.id.labelIDView);
            xPosValue = view.findViewById(R.id.xPosView);
            yPosValue = view.findViewById(R.id.yPosView);

        }

    }
}
