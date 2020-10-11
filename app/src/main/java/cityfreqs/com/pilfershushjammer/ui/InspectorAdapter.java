package cityfreqs.com.pilfershushjammer.ui;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cityfreqs.com.pilfershushjammer.R;

public class InspectorAdapter extends RecyclerView.Adapter<InspectorAdapter.ViewHolder> {
    private static final String TAG = "PS-INS_ADAPTER";
    private String[] mDataSet;

    // provide reference to the ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                }
            });
            textView = v.findViewById(R.id.inspector_text_view);
        }
        public TextView getTextView() {
            return textView;
        }
    }

    //init with dataset
    public InspectorAdapter(String[] dataSet) {
        mDataSet = dataSet;
    }

    @NonNull
    @Override
    public InspectorAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // create new view
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.inspector_row_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull InspectorAdapter.ViewHolder viewHolder, int position) {
        // get element from datasetat position, set contents of view with element
        Log.d(TAG, "Element " + position + " set.");
        viewHolder.getTextView().setText(mDataSet[position]);
    }

    @Override
    public int getItemCount() {
        return mDataSet.length;
    }
}
