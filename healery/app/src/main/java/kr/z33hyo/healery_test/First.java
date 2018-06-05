package kr.z33hyo.healery_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class First extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        GridView gridView = (GridView) findViewById(R.id.category_gridview);
        ArrayList<String> items = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.category)));

        gridView.setAdapter(new GridAdapter(items));
        /*ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_multiple_choice, items);
        gridView.setAdapter(adapter);*/
    }

    private class GridAdapter extends BaseAdapter {
        ArrayList <String> cItems;
        int cCount;

        public GridAdapter(ArrayList<String> items){
            cCount = items.size();
            cItems = new ArrayList<String>(items);
        }
        public int getCount(){
            return cCount;
        }
        public Object getItem(int position){
            return cItems.get(position);
        }
        public long getItemId(int position){
            return position;
        }
        public View getView(int position, View convertView, ViewGroup parent){
            View view = convertView;

            /*if (view==null) view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            TextView txt = (TextView)view.findViewById(android.R.id.text1);
            txt.setText(cItems.get(position));*/
            if (view==null) view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_gridview_item, parent, false);
            CheckBox chk = (CheckBox)view.findViewById(R.id.category_checkBox);
            chk.setText(cItems.get(position));
            return view;
        }

    }
}
