package cityfreqs.com.pilfershushjammer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cityfreqs.com.pilfershushjammer.R;

public class InspectorAdapter extends RecyclerView.Adapter<InspectorAdapter.ViewHolder> {
    //private static final String TAG = "PS-INS_ADAPTER";
    private final String[] mDataSet;
    private RecyclerViewClickListener clickListener;

    // provide reference to the ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView textView;
        //private RecyclerViewClickListener clickListener;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this); // bind the listener
            textView = view.findViewById(R.id.inspector_text_view);
        }
        public TextView getTextView() {
            return textView;
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(v, getAdapterPosition());
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
        // get element from dataset position, set contents of view with element
        viewHolder.getTextView().setText(mDataSet[position]);
    }

    @Override
    public int getItemCount() {
        return mDataSet.length;
    }

    public void setClickListener(RecyclerViewClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface RecyclerViewClickListener {
        void onClick(View view, int position);
    }
}
