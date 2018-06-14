package gr.thmmy.mthmmy.services.downloads;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import gr.thmmy.mthmmy.base.BaseApplication;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import timber.log.Timber;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DownloadsService extends IntentService {
    private static final String TAG = "DownloadsService";
    private static int sDownloadId = 0;

    private DownloadsReceiver receiver;

    public static final String SAVE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "mthmmy";

    public static final String ACTION_DOWNLOAD = "gr.thmmy.mthmmy.services.action.DOWNLOAD";
    public static final String EXTRA_DOWNLOAD_URL = "gr.thmmy.mthmmy.services.extra.DOWNLOAD_URL";

    public static final String EXTRA_DOWNLOAD_ID = "gr.thmmy.mthmmy.services.extra.DOWNLOAD_ID";
    public static final String EXTRA_DOWNLOAD_STATE = "gr.thmmy.mthmmy.services.extra.DOWNLOAD_STATE";
    public static final String EXTRA_FILE_NAME = "gr.thmmy.mthmmy.services.extra.FILE_NAME";
    public static final String EXTRA_NOTIFICATION_TITLE = "gr.thmmy.mthmmy.services.extra.NOTIFICATION_TITLE";
    public static final String EXTRA_NOTIFICATION_TEXT = "gr.thmmy.mthmmy.services.extra.NOTIFICATION_TEXT";
    public static final String EXTRA_NOTIFICATION_TICKER = "gr.thmmy.mthmmy.services.extra.NOTIFICATION_TICKER";

    public static final String STARTED = "Started";
    public static final String COMPLETED = "Completed";
    public static final String FAILED = "Failed";


    public DownloadsService() {
        super("DownloadsService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final IntentFilter filter = new IntentFilter(DownloadsService.ACTION_DOWNLOAD);
        receiver = new DownloadsReceiver();
        registerReceiver(receiver, filter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(receiver);
    }

    /**
     * Starts this service to perform action Download with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionDownload(Context context, String downloadUrl) {
        Intent intent = new Intent(context, DownloadsService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_DOWNLOAD_URL, downloadUrl);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                final String downloadLink = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
                handleActionDownload(downloadLink);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDownload(String downloadLink) {
        OkHttpClient client = BaseApplication.getInstance().getClient();
        BufferedSink sink = null;
        String fileName = "file";

        int downloadId = sDownloadId;
        sDownloadId++;

        try {
            Request request = new Request.Builder().url(downloadLink).build();
            Response response = client.newCall(request).execute();

            String contentDisposition = response.headers("Content-Disposition").toString();   //check if link provides an attachment
            if (contentDisposition.contains("attachment")) {
                fileName = contentDisposition.split("\"")[1];

                File dirPath = new File(SAVE_DIR);
                if (!dirPath.isDirectory()) {
                    if (dirPath.mkdirs())
                        Timber.i("mTHMMY's directory created successfully!");
                    else
                        Timber.e("Couldn't create mTHMMY's directory...");
                }


                String nameFormat;
                String[] tokens = fileName.split("\\.(?=[^\\.]+$)");

                if (tokens.length != 2) {
                    Timber.w("Couldn't get file extension...");
                    nameFormat = fileName + "(%d)";
                } else
                    nameFormat = tokens[0] + "(%d)." + tokens[1];


                File file = new File(dirPath, fileName);

                for (int i = 1; ; i++) {
                    if (!file.exists())
                        break;

                    file = new File(dirPath, String.format(nameFormat, i));
                }

                fileName = file.getName();

                Timber.v("Started saving file %s", fileName);
                sendNotification(downloadId, STARTED, fileName);

                sink = Okio.buffer(Okio.sink(file));
                sink.writeAll(response.body().source());
                sink.flush();
                Timber.i("Download OK!");
                sendNotification(downloadId, COMPLETED, fileName);

                // Register download
                DownloadManager mManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                long length = file.length();
                mManager.addCompletedDownload(fileName, fileName, false, getMimeType(file), SAVE_DIR +File.separator+ fileName, length, false);

            } else
                Timber.e("No attachment in response!");
        } catch (FileNotFoundException e) {
            Timber.i("Download failed...");
            Timber.e(e, "FileNotFound");
            sendNotification(downloadId, FAILED, fileName);
        } catch (IOException e) {
            Timber.i("Download failed...");
            Timber.e(e, "IOException");
            sendNotification(downloadId, FAILED, fileName);
        } finally {
            if (sink != null) {
                try {
                    sink.close();
                } catch (IOException e) {
                    // Ignore - Significant errors should already have been reported
                }
            }
        }
    }

    private void sendNotification(int downloadId, String type, @NonNull String fileName) {
        Intent intent = new Intent(ACTION_DOWNLOAD);
        switch (type) {
            case STARTED: {
                intent.putExtra(EXTRA_NOTIFICATION_TITLE, "\"" + fileName + "\"");
                intent.putExtra(EXTRA_NOTIFICATION_TEXT, "Download Started");
                intent.putExtra(EXTRA_NOTIFICATION_TICKER, "Downloading...");
                break;
            }
            case COMPLETED: {
                intent.putExtra(EXTRA_NOTIFICATION_TITLE, "\"" + fileName + "\"");
                intent.putExtra(EXTRA_NOTIFICATION_TEXT, "Download Completed");
                intent.putExtra(EXTRA_NOTIFICATION_TICKER, "Download Completed");
                break;
            }
            case FAILED: {
                intent.putExtra(EXTRA_NOTIFICATION_TITLE, "\"" + fileName + "\"");
                intent.putExtra(EXTRA_NOTIFICATION_TEXT, "Download Failed");
                intent.putExtra(EXTRA_NOTIFICATION_TICKER, "Download Failed");
                break;
            }
            default: {
                Timber.e("Invalid notification case!");
                return;
            }
        }
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        intent.putExtra(EXTRA_DOWNLOAD_STATE, type);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        sendBroadcast(intent);

    }

    @NonNull
    static String getMimeType(@NonNull File file) {
        String type = null;
        final String url = file.toString();
        final String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        if (type == null)
            type = "*/*";

        return type;
    }

}