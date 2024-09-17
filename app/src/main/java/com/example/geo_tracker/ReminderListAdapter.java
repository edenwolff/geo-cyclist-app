package com.example.geo_tracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Adapter class holding all reminder data
 */
public class ReminderListAdapter extends ArrayAdapter<Reminder>
{
    public ReminderListAdapter(@NonNull Context context, @NonNull List<Reminder> reminders) {
        super(context, 0, reminders);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.reminder_list_item, parent, false);
        }

        Reminder currentReminder = getItem(position);

        // Define name text view
        TextView nameTextView = listItemView.findViewById(R.id.reminder_list_name);
        nameTextView.setText(currentReminder.getTitle());

        // Define time text view
        TextView timeTextView = listItemView.findViewById(R.id.reminder_list_description);
        timeTextView.setText(currentReminder.getDescription());

        // Return each list view item
        return listItemView;
    }
}
