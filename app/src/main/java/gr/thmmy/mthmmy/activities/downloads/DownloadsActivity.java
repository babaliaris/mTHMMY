package gr.thmmy.mthmmy.activities.downloads;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.base.BaseActivity;
import gr.thmmy.mthmmy.model.Download;
import gr.thmmy.mthmmy.model.LinkTarget;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import mthmmy.utils.Report;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadsActivity extends BaseActivity implements DownloadsAdapter.OnLoadMoreListener {
    /**
     * Debug Tag for logging debug output to LogCat
     */
    @SuppressWarnings("unused")
    private static final String TAG = "DownloadsActivity";
    /**
     * The key to use when putting download's url String to {@link DownloadsActivity}'s Bundle.
     */
    public static final String BUNDLE_DOWNLOADS_URL = "DOWNLOADS_URL";
    /**
     * The key to use when putting download's title String to {@link DownloadsActivity}'s Bundle.
     */
    public static final String BUNDLE_DOWNLOADS_TITLE = "DOWNLOADS_TITLE";
    private static final String downloadsIndexUrl = "https://www.thmmy.gr/smf/index.php?action=tpmod;dl;";
    private String downloadsUrl;
    String downloadsTitle;
    private ArrayList<Download> parsedDownloads = new ArrayList<>();

    private MaterialProgressBar progressBar;
    private RecyclerView recyclerView;
    private DownloadsAdapter downloadsAdapter;
    private FloatingActionButton uploadFAB;

    private ParseDownloadPageTask parseDownloadPageTask;
    private int numberOfPages = -1;
    private int pagesLoaded = 0;
    private boolean isLoadingMore;
    private static final int visibleThreshold = 5;
    private int lastVisibleItem, totalItemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        Bundle extras = getIntent().getExtras();
        downloadsTitle = extras.getString(BUNDLE_DOWNLOADS_TITLE);
        if (downloadsTitle == null || Objects.equals(downloadsTitle, ""))
            downloadsTitle = "Downloads";
        downloadsUrl = extras.getString(BUNDLE_DOWNLOADS_URL);
        if (downloadsUrl != null && !Objects.equals(downloadsUrl, "")) {
            LinkTarget.Target target = LinkTarget.resolveLinkTarget(Uri.parse(downloadsUrl));
            if (!target.is(LinkTarget.Target.DOWNLOADS)) {
                Report.e(TAG, "Bundle came with a non board url!\nUrl:\n" + downloadsUrl);
                Toast.makeText(this, "An error has occurred\nAborting.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else downloadsUrl = downloadsIndexUrl;

        //Initialize toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(downloadsTitle);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        createDrawer();
        drawer.setSelection(DOWNLOADS_ID);

        progressBar = (MaterialProgressBar) findViewById(R.id.progressBar);

        recyclerView = (RecyclerView) findViewById(R.id.downloads_recycler_view);
        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        downloadsAdapter = new DownloadsAdapter(getApplicationContext(), parsedDownloads);
        recyclerView.setAdapter(downloadsAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = layoutManager.getItemCount();
                lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (!isLoadingMore && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    isLoadingMore = true;
                    onLoadMore();
                }
            }
        });

        uploadFAB = (FloatingActionButton) findViewById(R.id.download_fab);
        uploadFAB.setEnabled(false);

        parseDownloadPageTask = new ParseDownloadPageTask();
        parseDownloadPageTask.execute(downloadsUrl);
    }

    @Override
    public void onLoadMore() {
        if (pagesLoaded < numberOfPages) {
            parsedDownloads.add(null);
            downloadsAdapter.notifyItemInserted(parsedDownloads.size());

            //Load data
            parseDownloadPageTask = new ParseDownloadPageTask();
            if (downloadsUrl.contains("tpstart"))
                parseDownloadPageTask.execute(downloadsUrl.substring(0
                        , downloadsUrl.lastIndexOf(";tpstart=")) + ";tpstart=" + pagesLoaded * 10);
            else parseDownloadPageTask.execute(downloadsUrl + ";tpstart=" + pagesLoaded * 10);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        drawer.setSelection(DOWNLOADS_ID);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recyclerView.setAdapter(null);
        if (parseDownloadPageTask != null && parseDownloadPageTask.getStatus() != AsyncTask.Status.RUNNING)
            parseDownloadPageTask.cancel(true);
    }

    /**
     * An {@link AsyncTask} that handles asynchronous fetching of a downloads page and parsing it's
     * data. {@link AsyncTask#onPostExecute(Object) OnPostExecute} method calls {@link RecyclerView#swapAdapter}
     * to build graphics.
     * <p>
     * <p>Calling TopicTask's {@link AsyncTask#execute execute} method needs to have profile's url
     * as String parameter!</p>
     */
    class ParseDownloadPageTask extends AsyncTask<String, Void, Void> {
        /**
         * Debug Tag for logging debug output to LogCat
         */
        private static final String TAG = "ParseDownloadPageTask"; //Separate tag for AsyncTask
        private String thisPageUrl;

        @Override
        protected void onPreExecute() {
            if (!isLoadingMore) progressBar.setVisibility(ProgressBar.VISIBLE);
            if (uploadFAB.getVisibility() != View.GONE) uploadFAB.setEnabled(false);
        }

        @Override
        protected Void doInBackground(String... downloadsUrl) {
            thisPageUrl = downloadsUrl[0];
            Request request = new Request.Builder()
                    .url(downloadsUrl[0])
                    .build();
            try {
                Response response = BaseActivity.getClient().newCall(request).execute();
                parseDownloads(Jsoup.parse(response.body().string()));
            } catch (SSLHandshakeException e) {
                Report.w(TAG, "Certificate problem (please switch to unsafe connection).");
            } catch (Exception e) {
                Report.e("TAG", "ERROR", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            if (downloadsTitle == null || Objects.equals(downloadsTitle, ""))
                toolbar.setTitle(downloadsTitle);

            ++pagesLoaded;
            if (uploadFAB.getVisibility() != View.GONE) uploadFAB.setEnabled(true);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            downloadsAdapter.notifyDataSetChanged();
            isLoadingMore = false;
        }

        private void parseDownloads(Document downloadPage) {
            if (downloadsTitle == null || Objects.equals(downloadsTitle, ""))
                downloadsTitle = downloadPage.select("div.nav>b>a.nav").last().text();

            //Removes loading item
            if (isLoadingMore) {
                if (parsedDownloads.size() > 0) parsedDownloads.remove(parsedDownloads.size() - 1);
            }

            Download.DownloadItemType type;
            if (LinkTarget.resolveLinkTarget(Uri.parse(thisPageUrl)).is(LinkTarget.
                    Target.DOWNLOADS_CATEGORY)) type = Download.DownloadItemType.DOWNLOADS_CATEGORY;
            else type = Download.DownloadItemType.DOWNLOADS_FILE;

            Elements pages = downloadPage.select("a.navPages");
            if (pages != null) {
                for (Element page : pages) {
                    int pageNumber = Integer.parseInt(page.text());
                    if (pageNumber > numberOfPages) numberOfPages = pageNumber;
                }
            } else numberOfPages = 1;

            Elements rows = downloadPage.select("table.tborder>tbody>tr");
            if (type == Download.DownloadItemType.DOWNLOADS_CATEGORY) {
                Elements navigationLinks = downloadPage.select("div.nav>b");
                for (Element row : rows) {
                    if (row.select("td").size() == 1) continue;

                    String url = row.select("b>a").first().attr("href"),
                            title = row.select("b>a").first().text(),
                            subtitle = row.select("div.smalltext:not(:has(a))").text();
                    if (!row.select("td").last().hasClass("windowbg2")) {
                        if (navigationLinks.size() < 4) {

                            parsedDownloads.add(new Download(type, url, title, subtitle, null,
                                    true, null));
                        } else {
                            String stats = row.text();
                            stats = stats.replace(title, "").replace(subtitle, "").trim();
                            parsedDownloads.add(new Download(type, url, title, subtitle, stats,
                                    false, null));
                        }
                    } else {
                        String stats = row.text();
                        stats = stats.replace(title, "").replace(subtitle, "").trim();
                        parsedDownloads.add(new Download(type, url, title, subtitle, stats,
                                false, null));
                    }
                }
            } else {
                parsedDownloads.add(new Download(type,
                        rows.select("b>a").first().attr("href"),
                        rows.select("b>a").first().text(),
                        rows.select("div.smalltext:not(:has(a))").text(),
                        rows.select("span:not(:has(a))").first().text(),
                        false,
                        rows.select("span:has(a)").first().html()));
            }
        }
    }
}
