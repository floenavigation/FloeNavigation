package de.awi.floenavigation;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

public class ListViewActivity extends Activity {

    private final String TAG = "ListViewActivity";
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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mAdapter.notifyDataSetChanged();
            }
        });
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

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
        ViewHolder viewHolder= new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewAdapter.ViewHolder holder, int position) {

        holder.labelIDView.setText(parameters.get(position).getLabelID());
        holder.xPosValue.setText(parameters.get(position).getxValue());
        holder.yPosValue.setText(parameters.get(position).getyValue());
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView labelIDView;
        TextView xPosValue;
        TextView yPosValue;

        ViewHolder(View view) {
            super(view);
            labelIDView = view.findViewById(R.id.labelIDView);
            xPosValue = view.findViewById(R.id.xPosView);
            yPosValue = view.findViewById(R.id.yPosView);

        }
    }
}
