package gr.thmmy.mthmmy.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import gr.thmmy.mthmmy.utils.Thmmy;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;

public class BaseActivity extends AppCompatActivity {

    //Shared preferences
    static final String SHARED_PREFS_NAME = "thmmySharedPrefs";
    static final String USER_NAME = "userNameKey";
    static final String GUEST_PREF_USERNAME = "GUEST";
    static final String IS_LOGGED_IN = "isLoggedIn";
    static OkHttpClient client;
    static Thmmy.LoginData loginData;
    private static CookieJar cookieJar;
    private static SharedPrefsCookiePersistor sharedPrefsCookiePersistor;
    private static boolean init =false;   //To initialize stuff only once per app start

    public static CookieJar getCookieJar()
    {
        return cookieJar;
    }

    public static SharedPrefsCookiePersistor getSharedPrefsCookiePersistor() {
        return sharedPrefsCookiePersistor;
    }

    public static OkHttpClient getClient() {
        return client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!init)
        {
            sharedPrefsCookiePersistor = new SharedPrefsCookiePersistor(BaseActivity.this);
            cookieJar = new PersistentCookieJar(new SetCookieCache(), sharedPrefsCookiePersistor);
            client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .build();
            loginData = new Thmmy.LoginData();
            loginData.setStatus(0);
            init =true;
        }

    }

    void setLoginData(Thmmy.LoginData loginData) {
        BaseActivity.loginData = loginData;
    }
}
