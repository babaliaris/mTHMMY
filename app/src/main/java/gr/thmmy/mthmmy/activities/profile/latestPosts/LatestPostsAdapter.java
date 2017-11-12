package gr.thmmy.mthmmy.activities.profile.latestPosts;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.base.BaseFragment;
import gr.thmmy.mthmmy.model.PostSummary;
import gr.thmmy.mthmmy.model.TopicSummary;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * {@link RecyclerView.Adapter} that can display a {@link TopicSummary} and makes a call to the
 * specified {@link LatestPostsFragment.LatestPostsFragmentInteractionListener}.
 */
class LatestPostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_EMPTY = -1;
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    final private LatestPostsFragment.LatestPostsFragmentInteractionListener interactionListener;
    private final ArrayList<PostSummary> parsedTopicSummaries;

    LatestPostsAdapter(BaseFragment.FragmentInteractionListener interactionListener,
                       ArrayList<PostSummary> parsedTopicSummaries) {
        this.interactionListener = (LatestPostsFragment.LatestPostsFragmentInteractionListener) interactionListener;
        this.parsedTopicSummaries = parsedTopicSummaries;
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    @Override
    public int getItemViewType(int position) {
        if (parsedTopicSummaries.get(position) == null && position == 0) return VIEW_TYPE_EMPTY;
        return parsedTopicSummaries.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_latest_posts_empty_message, parent, false);
            return new RecyclerView.ViewHolder(view){};
        }
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_latest_posts_row, parent, false);
            return new LatestPostViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.recycler_loading_item, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LatestPostViewHolder) {
            PostSummary topic = parsedTopicSummaries.get(position);
            final LatestPostViewHolder latestPostViewHolder = (LatestPostViewHolder) holder;

            latestPostViewHolder.postTitle.setText(topic.getSubject());
            latestPostViewHolder.postDate.setText(topic.getDateTime());
            latestPostViewHolder.post.loadDataWithBaseURL("file:///android_asset/"
                    , topic.getPost(), "text/html", "UTF-8", null);

            latestPostViewHolder.latestPostsRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (interactionListener != null) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that a post has been selected.
                        interactionListener.onLatestPostsFragmentInteraction(
                                parsedTopicSummaries.get(holder.getAdapterPosition()));
                    }

                }
            });
        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return parsedTopicSummaries == null ? 0 : parsedTopicSummaries.size();
    }

    private static class LatestPostViewHolder extends RecyclerView.ViewHolder {
        final RelativeLayout latestPostsRow;
        final TextView postTitle;
        final TextView postDate;
        final WebView post;

        LatestPostViewHolder(View itemView) {
            super(itemView);
            latestPostsRow = itemView.findViewById(R.id.latest_posts_row);
            postTitle = itemView.findViewById(R.id.title);
            postDate = itemView.findViewById(R.id.date);
            post = itemView.findViewById(R.id.post);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        final MaterialProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.recycler_progress_bar);
        }
    }
}