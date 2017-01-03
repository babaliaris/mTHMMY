package gr.thmmy.mthmmy.activities.profile.latestPosts;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.data.TopicSummary;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static gr.thmmy.mthmmy.activities.profile.latestPosts.LatestPostsFragment.parsedTopicSummaries;

class LatestPostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "LatestPostsAdapter";
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private OnLoadMoreListener mOnLoadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
        this.mOnLoadMoreListener = mOnLoadMoreListener;
    }

    @Override
    public int getItemViewType(int position) {
        return parsedTopicSummaries.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.profile_fragment_latest_posts_row, parent, false);
            return new LatestPostViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            Log.d(TAG, "GOT");
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.recycler_loading_item, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LatestPostViewHolder) {
            TopicSummary topic = parsedTopicSummaries.get(position);
            final LatestPostViewHolder latestPostViewHolder = (LatestPostViewHolder) holder;
            latestPostViewHolder.postTitle.setText(topic.getTitle());
            latestPostViewHolder.postDate.setText(topic.getDateTimeModified());
            latestPostViewHolder.post.loadDataWithBaseURL("file:///android_asset/"
                    , topic.getPost(), "text/html", "UTF-8", null);
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
        TextView postTitle;
        TextView postDate;
        WebView post;

        LatestPostViewHolder(View itemView) {
            super(itemView);
            postTitle = (TextView) itemView.findViewById(R.id.title);
            postDate = (TextView) itemView.findViewById(R.id.date);
            post = (WebView) itemView.findViewById(R.id.post);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        MaterialProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (MaterialProgressBar) itemView.findViewById(R.id.recycler_progress_bar);
        }
    }
}