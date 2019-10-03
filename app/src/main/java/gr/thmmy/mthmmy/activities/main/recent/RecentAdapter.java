package gr.thmmy.mthmmy.activities.main.recent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.base.BaseApplication;
import gr.thmmy.mthmmy.base.BaseFragment;
import gr.thmmy.mthmmy.model.TopicSummary;
import gr.thmmy.mthmmy.utils.RelativeTimeTextView;


/**
 * {@link RecyclerView.Adapter} that can display a {@link TopicSummary} and makes a call to the
 * specified {@link RecentFragment.RecentFragmentInteractionListener}.
 */
class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.ViewHolder> {
    private final List<TopicSummary> recentList;
    private final RecentFragment.RecentFragmentInteractionListener mListener;

    RecentAdapter(@NonNull List<TopicSummary> topicSummaryList, BaseFragment.FragmentInteractionListener listener) {
        this.recentList = topicSummaryList;
        mListener = (RecentFragment.RecentFragmentInteractionListener) listener;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_recent_row, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mTitleView.setText(recentList.get(position).getSubject());

        String dateTimeString = recentList.get(position).getDateTimeModified();
        if(BaseApplication.getInstance().isDisplayRelativeTimeEnabled())
            holder.mDateTimeView.setReferenceTime(Long.valueOf(dateTimeString));
        else
            holder.mDateTimeView.setText(dateTimeString);

        holder.mUserView.setText(recentList.get(position).getLastUser());
        holder.topic = recentList.get(position);

        holder.mView.setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onRecentFragmentInteraction(holder.topic);  //?
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mTitleView;
        final TextView mUserView;
        final RelativeTimeTextView mDateTimeView;
        public TopicSummary topic;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = view.findViewById(R.id.title);
            mUserView = view.findViewById(R.id.lastUser);
            mDateTimeView = view.findViewById(R.id.dateTime);
        }
    }
}
