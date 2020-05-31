package com.example.scan.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.example.scan.R;

import java.util.ArrayList;

public class RideAdapter extends ArrayAdapter<PreviousRides> {
    private static class ViewHolder {
        TextView bikeName;
        TextView date;
        TextView startTime;
        TextView distance;
        TextView duration;
    }
    public RideAdapter(Context context, ArrayList<PreviousRides> rides){
        super(context, 0, rides);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PreviousRides ride = getItem(position);
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.activity_ride, parent, false);
            viewHolder.bikeName = (TextView) convertView.findViewById(R.id.bikeName);
            viewHolder.date = (TextView) convertView.findViewById(R.id.date);
            viewHolder.startTime= (TextView) convertView.findViewById(R.id.startTime);
            viewHolder.distance= (TextView) convertView.findViewById(R.id.distance);
            viewHolder.duration= (TextView) convertView.findViewById(R.id.duration);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_ride, parent, false);
        }

        viewHolder.bikeName.setText(ride.bikeName);
        viewHolder.date.setText(ride.date);
        viewHolder.startTime.setText(ride.startTime);
        viewHolder.distance.setText(ride.distance);
        viewHolder.duration.setText(ride.duration);
        return convertView;
    }
}
