package gr.thmmy.mthmmy.sections.recent;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.activities.BaseActivity;
import gr.thmmy.mthmmy.data.TopicSummary;
import gr.thmmy.mthmmy.utils.CustomRecyclerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RecentFragment.OnListFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RecentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecentFragment extends Fragment
{
    private static final String TAG = "RecentFragment";
    // Fragment initialization parameters, e.g. ARG_SECTION_NUMBER
    private static final String ARG_SECTION_NUMBER = "SectionNumber";
    private int sectionNumber;

    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CustomRecyclerView recyclerView;
    private RecentAdapter recentAdapter;

    private List<TopicSummary> topicSummaries;

    private OnListFragmentInteractionListener mListener;

    OkHttpClient client;

    // Required empty public constructor
    public RecentFragment() {}

    /**
     * Use ONLY this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param sectionNumber
     * @return A new instance of fragment Recent.
     */
    public static RecentFragment newInstance(int sectionNumber)
    {
        RecentFragment fragment = new RecentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public int getSectionNumber() {
        return sectionNumber;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);

        client = BaseActivity.getClient();

        topicSummaries = new ArrayList<>();


        if(sectionNumber==1)    //?
        Log.d(TAG,"onCreate");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        if(sectionNumber==1)//temp
        {
            if(topicSummaries.isEmpty())
                new RecentTask().execute();


        }
        Log.d(TAG,"onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        if(sectionNumber==1)
        Log.d(TAG,"onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        if(sectionNumber==1)
        Log.d(TAG,"onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        if(sectionNumber==1)
        Log.d(TAG,"onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(sectionNumber==1)
        Log.d(TAG,"onStop");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_recent, container, false);

        // Set the adapter
        if (rootView instanceof RelativeLayout)
        {
            progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
            recentAdapter = new RecentAdapter(topicSummaries, mListener);

            recyclerView = (CustomRecyclerView) rootView.findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(rootView.findViewById(R.id.list).getContext()));
            recyclerView.setAdapter(recentAdapter);

            swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);
            swipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            new RecentTask().execute();

                        }

                    }
            );


        }

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;

        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(TopicSummary topicSummary);
    }


    int n=0;
    long s=0;


    //---------------------------------------ASYNC TASK-----------------------------------

    public class RecentTask extends AsyncTask<Void, Void, Integer>
    {
        private static final String TAG="RecentTask";
        private final String thmmyUrl = "https://www.thmmy.gr/smf/index.php";

        private Document document;


        protected void onPreExecute() {

            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        protected Integer doInBackground(Void... voids)
        {

            Request request = new Request.Builder()
                    .url(thmmyUrl)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                document = Jsoup.parse(response.body().string());
                parse(document);
                return 0;
            } catch (IOException e) {
                    Log.d("DEB", "ERROR", e);
                return 1;
            } catch (Exception e) {
                Log.d("DEB", "ERROR", e);
                return 2;
            }

        }



        protected void onPostExecute(Integer result)
        {

            if(result==0)
                recentAdapter.notifyDataSetChanged();
            else if (result==1)
                Toast.makeText(getActivity(), "Network error", Toast.LENGTH_SHORT).show();
            
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            swipeRefreshLayout.setRefreshing(false);
        }

        private boolean parse(Document document)
        {
            Elements recent = document.select("#block8 :first-child div");
            if(recent.size()==30)
            {
                topicSummaries.clear();

                for(int i=0; i<recent.size(); i+=3) {
                    String link = recent.get(i).child(0).attr("href");
                    String title = recent.get(i).child(0).attr("title");

                    String lastUser = recent.get(i + 1).text();
                    Pattern pattern = Pattern.compile("by (.*)");
                    Matcher matcher = pattern.matcher(lastUser);
                    if (matcher.find())
                        lastUser = matcher.group(1);
                    else
                    {
                        Log.e(TAG, "Parsing failed (lastUser)!");
                        return false;
                    }

                    String dateTime = recent.get(i + 2).text();
                    pattern = Pattern.compile("\\[(.*)\\]");
                    matcher = pattern.matcher(dateTime);
                    if (matcher.find())
                        dateTime = matcher.group(1);
                    else
                    {
                        Log.e(TAG, "Parsing failed (dateTime)!");
                        return false;
                    }


                    topicSummaries.add(new TopicSummary(link, title, lastUser, dateTime));
                }

                return true;
            }
            Log.e(TAG, "Parsing failed!");
            return false;
        }

        // TODO: replace parse function with this when a method to get recent TOPICS and not POSTS becomes available
//        private boolean parse(String document) throws XmlPullParserException, IOException {
//            String text = null, tagName, link = null, title = null, poster = null,dateTime = null;
//            boolean posterFlag = false;
//            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
//            factory.setNamespaceAware(true);
//            XmlPullParser xpp = factory.newPullParser();
//
//            xpp.setInput( new StringReader(document) );
//            int eventType = xpp.getEventType();
//            topicSummaries.clear();
//
//            while (eventType != XmlPullParser.END_DOCUMENT) {
//                tagName = xpp.getName();
//                switch (eventType) {
//                    case XmlPullParser.START_TAG:
//                        if (tagName.equals("poster"))
//                            posterFlag=true;
//                        break;
//
//                    case XmlPullParser.TEXT:
//                        text = xpp.getText();
//                        break;
//
//                    case XmlPullParser.END_TAG:
//                        switch (tagName)
//                        {
//                            case "recent-post":
//                                topicSummaries.add(new TopicSummary(link, title, poster, dateTime));
//                                break;
//                            case "name":
//                                if(posterFlag) {
//                                    poster = text;
//                                    posterFlag = false;
//                                }
//                                break;
//                            case "link":
//                                link = text;
//                                break;
//                            case "time":
//                                dateTime = text;
//                                break;
//                            case "subject":
//                                title = text;
//                                break;
//                        }
//                        break;
//
//                    default:
//                        break;
//                }
//                eventType = xpp.next();
//            }
//            return true;
//        }
    }

}
