package mthmmy.utils;

import android.util.Log;

public class Report
{

    public static void v (String TAG, String message)
    {
        Log.v(TAG,message);
    }

    public static void v (String TAG, String message, Throwable tr)
    {
        Log.v(TAG,message + ": " + tr.getMessage(),tr);
    }

    public static void d (String TAG, String message)
    {
        Log.d(TAG,message);
    }

    public static void d (String TAG, String message, Throwable tr)
    {
        Log.d(TAG,message + ": " + tr.getMessage(),tr);
    }

    public static void i (String TAG, String message)
    {
        Log.i(TAG,message);
    }

    public static void i (String TAG, String message, Throwable tr)
    {
        Log.i(TAG,message + ": " + tr.getMessage(),tr);
    }

    public static void w (String TAG, String message)
    {
        Log.w(TAG,message);
    }

    public static void w (String TAG, String message, Throwable tr)
    {
        Log.w(TAG,message + ": " + tr.getMessage(),tr);
    }

    public static void e (String TAG, String message)
    {
        Log.e(TAG,message);
    }

    public static void e (String TAG, String message, Throwable tr)
    {
        Log.e(TAG,message + ": " + tr.getMessage(),tr);
    }

    public static void wtf (String TAG, String message)
    {
        Log.wtf(TAG,message);
    }

    public static void wtf (String TAG, String message, Throwable tr)
    {
        Log.wtf(TAG,message + ": " + tr.getMessage(),tr);
    }

    /**
     * Prints long messages in logcat (debug level).
     */
    public static void longMessage(String TAG, String message)
    {
        int maxLogSize = 1000;
        for(int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i+1) * maxLogSize;
            end = end > message.length() ? message.length() : end;
            Report.d(TAG, message.substring(start, end));
        }
    }
}