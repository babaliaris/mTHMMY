package gr.thmmy.mthmmy.activities.profile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.activities.profile.latestPosts.LatestPostsFragment;
import gr.thmmy.mthmmy.activities.profile.stats.StatsFragment;
import gr.thmmy.mthmmy.activities.profile.summary.SummaryFragment;
import gr.thmmy.mthmmy.activities.topic.TopicActivity;
import gr.thmmy.mthmmy.base.BaseActivity;
import gr.thmmy.mthmmy.model.PostSummary;
import gr.thmmy.mthmmy.model.ThmmyPage;
import gr.thmmy.mthmmy.utils.CenterVerticalSpan;
import gr.thmmy.mthmmy.utils.CircleTransform;
import gr.thmmy.mthmmy.utils.NetworkResultCodes;
import gr.thmmy.mthmmy.utils.Parcel;
import gr.thmmy.mthmmy.utils.parsing.NewParseTask;
import gr.thmmy.mthmmy.utils.parsing.ParseException;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import okhttp3.Response;
import timber.log.Timber;

import static gr.thmmy.mthmmy.activities.topic.TopicActivity.BUNDLE_TOPIC_TITLE;
import static gr.thmmy.mthmmy.activities.topic.TopicActivity.BUNDLE_TOPIC_URL;
import static gr.thmmy.mthmmy.utils.parsing.ParseHelpers.emojiTagToHtml;

/**
 * Activity for user profile. When creating an Intent of this activity you need to bundle a <b>String</b>
 * containing this user's profile url using the key {@link #BUNDLE_PROFILE_URL}, a <b>String</b> containing
 * this user's avatar url using the key {@link #BUNDLE_PROFILE_THUMBNAIL_URL} and a <b>String</b> containing
 * the username using the key {@link #BUNDLE_PROFILE_USERNAME}.
 */
public class ProfileActivity extends BaseActivity implements LatestPostsFragment.LatestPostsFragmentInteractionListener {
    /**
     * The key to use when putting profile's url String to {@link ProfileActivity}'s Bundle.
     */
    public static final String BUNDLE_PROFILE_URL = "PROFILE_URL";
    /**
     * The key to use when putting user's thumbnail url String to {@link ProfileActivity}'s Bundle.
     * If user doesn't have a thumbnail put an empty string or leave it null.
     */
    public static final String BUNDLE_PROFILE_THUMBNAIL_URL = "THUMBNAIL_URL";
    /**
     * The key to use when putting username String to {@link ProfileActivity}'s Bundle.
     * If username is not available put an empty string or leave it null.
     */
    public static final String BUNDLE_PROFILE_USERNAME = "USERNAME";

    private TextView usernameView;
    private ImageView avatarView;
    private ImageView expandedImageView;
    private Animator currentAnimator;
    private int shortAnimationDuration;
    private TextView personalTextView;
    private MaterialProgressBar progressBar;
    private FloatingActionButton pmFAB;
    private ViewPager viewPager;

    private ProfileTask profileTask;
    private String personalText;
    private String profileUrl;
    private String avatarUrl;
    private String username;
    private int tabSelect;

    //Fix for vector drawables on android <21
    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Bundle extras = getIntent().getExtras();
        avatarUrl = extras.getString(BUNDLE_PROFILE_THUMBNAIL_URL);
        if (avatarUrl == null) avatarUrl = "";
        username = extras.getString(BUNDLE_PROFILE_USERNAME);
        profileUrl = extras.getString(BUNDLE_PROFILE_URL);

        //Initializes graphic elements
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(null);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        createDrawer();

        progressBar = findViewById(R.id.progressBar);

        avatarView = findViewById(R.id.user_thumbnail);
        expandedImageView = findViewById(R.id.expanded_user_thumbnail);
        if (!Objects.equals(avatarUrl, "")) {
            //noinspection ConstantConditions
            loadAvatar();
            avatarView.setOnClickListener(view -> zoomImageFromThumb(avatarView));
            shortAnimationDuration = getResources().getInteger(
                    android.R.integer.config_shortAnimTime);
        } else
            loadDefaultAvatar();

        usernameView = findViewById(R.id.profile_activity_username);
        usernameView.setTypeface(Typeface.createFromAsset(this.getAssets()
                , "fonts/fontawesome-webfont.ttf"));
        if (username != null && !Objects.equals(username, "")) usernameView.setText(username);
        personalTextView = findViewById(R.id.profile_activity_personal_text);

        viewPager = findViewById(R.id.profile_tab_container);

        pmFAB = findViewById(R.id.profile_fab);
        pmFAB.setEnabled(false);
        pmFAB.hide();
        /*if (!sessionManager.isLoggedIn()) pmFAB.hide();
        else {
            pmFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (sessionManager.isLoggedIn()) {
                        //TODO PM
                    } else {
                        new AlertDialog.Builder(ProfileActivity.this)
                                .setMessage("You need to be logged in to sent a personal message!")
                                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                })
                                .show();
                    }
                }
            });
        }*/

        ThmmyPage.PageCategory target = ThmmyPage.resolvePageCategory(Uri.parse(profileUrl));
        if (!target.is(ThmmyPage.PageCategory.PROFILE)) {
            Timber.e("Bundle came with a non profile url!\nUrl:\n%s", profileUrl);
            Toast.makeText(this, "An error has occurred\n Aborting.", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (target.is(ThmmyPage.PageCategory.PROFILE_STATS)) {
            profileUrl = profileUrl.substring(0, profileUrl.indexOf(";sa=statPanel"));
            tabSelect = 2;
        } else if (target.is(ThmmyPage.PageCategory.PROFILE_LATEST_POSTS)) {
            profileUrl = profileUrl.substring(0, profileUrl.indexOf(";sa=showPosts"));
            tabSelect = 1;
        }

        profileTask = new ProfileTask();
        profileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, profileUrl + ";wap"); //Attempts data parsing
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileTask != null && profileTask.getStatus() != AsyncTask.Status.RUNNING)
            profileTask.cancel(true);
    }

    @Override
    public void onLatestPostsFragmentInteraction(PostSummary postSummary) {
        Intent i = new Intent(ProfileActivity.this, TopicActivity.class);
        i.putExtra(BUNDLE_TOPIC_URL, postSummary.getPostUrl());
        i.putExtra(BUNDLE_TOPIC_TITLE, postSummary.getSubject().substring(postSummary.getSubject().
                lastIndexOf("/ ") + 2));
        startActivity(i);
    }

    public void onProfileTaskStarted() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        if (pmFAB.getVisibility() != View.GONE) pmFAB.setEnabled(false);
    }

    private void loadAvatar() {
        Picasso.with(this)
                .load(avatarUrl)
                .fit()
                .centerCrop()
                .error(Objects.requireNonNull(ResourcesCompat.getDrawable(this.getResources()
                        , R.drawable.ic_default_user_avatar, null)))
                .placeholder(Objects.requireNonNull(ResourcesCompat.getDrawable(this.getResources()
                        , R.drawable.ic_default_user_avatar, null)))
                .transform(new CircleTransform())
                .into(avatarView);

        Picasso.with(this)
                .load(avatarUrl)
                .fit()
                .centerCrop()
                .error(Objects.requireNonNull(ResourcesCompat.getDrawable(this.getResources()
                        , R.drawable.ic_default_user_avatar, null)))
                .placeholder(Objects.requireNonNull(ResourcesCompat.getDrawable(this.getResources()
                        , R.drawable.ic_default_user_avatar, null)))
                .into(expandedImageView);
    }

    private void loadDefaultAvatar() {
        Picasso.with(this)
                .load(R.drawable.ic_default_user_avatar)
                .fit()
                .centerCrop()
                .error(Objects.requireNonNull(ResourcesCompat.getDrawable(this.getResources()
                        , R.drawable.ic_default_user_avatar, null)))
                .placeholder(Objects.requireNonNull(ResourcesCompat.getDrawable(this.getResources()
                        , R.drawable.ic_default_user_avatar, null)))
                .transform(new CircleTransform())
                .into(avatarView);
    }

    /*
     * Also check this out:
     * https://stackoverflow.com/questions/39749404/android-shared-element-transition-transforming-an-imageview-from-a-circle-to-a
     */
    private void zoomImageFromThumb(final View thumbView) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.main_content)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;
            }
        });
    }

    /**
     * An {@link AsyncTask} that handles asynchronous fetching of a profile page and parsing this
     * user's personal text. The {@link Document} resulting from the parse is stored for use in
     * the {@link SummaryFragment}.
     * <p>ProfileTask's {@link AsyncTask#execute execute} method needs a profile's url as String
     * parameter!</p>
     *
     * @see Jsoup
     */
    public class ProfileTask extends NewParseTask<Void> {
        //Class variables
        Document profilePage;
        Spannable usernameSpan;
        Boolean isOnline = false;

        ProfileTask() {
            super(ProfileActivity.this::onProfileTaskStarted, null);
        }

        @Override
        protected Void parse(Document document, Response response) throws ParseException {
            profilePage = document;
            Elements contentsTable = profilePage.
                    select(".bordercolor > tbody:nth-child(1) > tr:nth-child(2) tbody");

            //Finds username if missing
            if (username == null || Objects.equals(username, "")) {
                username = contentsTable.select("tr").first().select("td").last().text();
            }
            if (avatarUrl == null || Objects.equals(avatarUrl, "")) { //Maybe there is an avatar
                Element profileAvatar = profilePage.select("img.avatar").first();
                if (profileAvatar != null) avatarUrl = profileAvatar.attr("abs:src");
            }
            { //Finds personal text
                Element tmpEl = profilePage.select("td.windowbg:nth-child(2)").first();
                if (tmpEl != null) {
                    personalText = emojiTagToHtml(tmpEl.text().trim());
                } else {
                    //Should never get here!
                    //Something is wrong.
                    Timber.e("An error occurred while trying to find profile's personal text.");
                    personalText = null;
                }
            }
            { //Finds status
                usernameSpan = new SpannableString(getResources()
                        .getString(R.string.fa_circle) + "  " + username);
                usernameSpan.setSpan(new CenterVerticalSpan(), 0, 2,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                usernameSpan.setSpan(new RelativeSizeSpan(0.45f)
                        , 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                /*usernameSpan.setSpan(new ForegroundColorSpan(Color.GRAY)
                        , 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);*/
                isOnline = contentsTable.toString().contains("Online")
                        || contentsTable.toString().contains("Συνδεδεμένος");
                usernameSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#26A69A"))
                        , 2, usernameSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Parcel<Void> parcel) {
            int result = parcel.getResultCode();
            if (result == NetworkResultCodes.SUCCESSFUL) {
                //Parse was successful
                if (pmFAB.getVisibility() != View.GONE) pmFAB.setEnabled(true);
                progressBar.setVisibility(ProgressBar.INVISIBLE);

                if (usernameSpan != null) {
                    if (isOnline) {
                        usernameView.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        usernameView.setTextColor(Color.GRAY);
                    }
                    usernameView.setText(usernameSpan);
                } else if (usernameView.getText() != username) usernameView.setText(username);
                if (avatarUrl != null && !Objects.equals(avatarUrl, ""))
                    //noinspection ConstantConditions
                    loadAvatar();
                else
                    loadDefaultAvatar();
                if (personalText != null) {
                    personalTextView.setText(personalText);
                    personalTextView.setVisibility(View.VISIBLE);
                }

                setupViewPager(viewPager, profilePage);
                TabLayout tabLayout = findViewById(R.id.profile_tabs);
                tabLayout.setupWithViewPager(viewPager);
                if (tabSelect != 0) {
                    TabLayout.Tab tab = tabLayout.getTabAt(tabSelect);
                    if (tab != null) tab.select();
                }
            } else if (result == NetworkResultCodes.NETWORK_ERROR) {
                Timber.w("Network error while excecuting profile activity");
                Toast.makeText(getBaseContext(), "Network error"
                        , Toast.LENGTH_LONG).show();
                finish();
            } else {
                Timber.d("Parse failed!");
                Toast.makeText(getBaseContext(), "Fatal error!\n Aborting..."
                        , Toast.LENGTH_LONG).show();
                finish();
            }
        }

        @Override
        protected int getResultCode(Response response, Void data) {
            return NetworkResultCodes.SUCCESSFUL;
        }
    }

    /**
     * Simple method that sets up the {@link ViewPager} of a {@link ProfileActivity}
     *
     * @param viewPager   the ViewPager to be setup
     * @param profilePage this profile's parsed page
     */
    private void setupViewPager(ViewPager viewPager, Document profilePage) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(SummaryFragment.newInstance(profilePage), "SUMMARY");
        adapter.addFrag(LatestPostsFragment.newInstance(profileUrl), "LATEST POSTS");
        adapter.addFrag(StatsFragment.newInstance(profileUrl), "STATS");
        viewPager.setAdapter(adapter);
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
