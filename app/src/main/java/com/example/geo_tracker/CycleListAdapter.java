package com.example.geo_tracker;

import android.widget.ArrayAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Adapter class holding cycling data
 */
public class CycleListAdapter extends ArrayAdapter<Cycle>
{
    // Declare constructor
    public CycleListAdapter(@NonNull Context context, @NonNull List<Cycle> cycles) {
        super(context, 0, cycles);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        // Use convert view if already exists
        View listItemView = convertView;
        if (listItemView == null)
        {
            // Inflate a new view from the list
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.cycle_list_item, parent, false);
        }

        // Get the current cycle at the given position
        Cycle currentCycle = getItem(position);


        // Initialise cycle name text view
        TextView nameTextView = listItemView.findViewById(R.id.listItemName);
        nameTextView.setText(currentCycle.getTitle());

        // Initialise time text view
        TextView timeTextView = listItemView.findViewById(R.id.listItemTime);
        timeTextView.setText(currentCycle.getTime());

        // Return populated list view item
        return listItemView;
    }
}
