package org.hear2read.telugu;

/**
 * Created by saikrishnalticmu on 7/6/17.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.FileReader;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

public class Voice {
    private final static String LOG_TAG = "Flite_Java_" + Voice.class.getSimpleName();
    private final static String FLITE_DATA_PATH = Environment.getExternalStorageDirectory()
            + "/hear2read-data/";
    public final static String DATA_FILE = FLITE_DATA_PATH+"cg/data";

    // private final static String FLITE_DATA_PATH = "file:///android_asset/";

    // private final static Uri path = Uri.parse("file:///src/assets/");

    // private final static String FLITE_DATA_PATH = path.toString();


    private final static String VOICE_BASE_URL = "http://festvox.org/flite/voices/cg/voxdata-tamilonly/";
    // private final static String VOICE_BASE_URL = "http://tts.speech.cs.cmu.edu/temp_info_for_flite_app/";

    private String mVoiceName;
    private String mVoiceMD5;
    private String mVoiceLanguage;
    private String mVoiceCountry;
    private String mVoiceVariant;
    private boolean mIsValidVoice;
    private String mVoicePath;
    private boolean mIsVoiceAvailable;
    private static Context mContext;
    private StringBuilder mDebugData;
    private String mDebugText;


    /**
     * @return absolute path to the hear2read-data directory
     */
    public static String getDataStorageBasePath() {
        return FLITE_DATA_PATH;
    }

    /**
     * @return base URL to download voices and other flite data
     */
    public static String getDownloadURLBasePath() {
        return VOICE_BASE_URL;
    }

    /**
     * @param voiceInfoLine is the line that is found in "voices_tamil.list" file
     * as downloaded on the server and cached. This line has text in the format:
     * language-country-variant<TAB>MD5SUM
     */
    Voice(String voiceInfoLine) {
        boolean parseSuccessful = false;

        String[] voiceInfo = voiceInfoLine.split("\t");
        if (voiceInfo.length != 2) {
            Log.e(LOG_TAG, "Voice line could not be read: " + voiceInfoLine);
        }
        else {
            mVoiceName = voiceInfo[0];
            mVoiceMD5 = voiceInfo[1];

            String[] voiceParams = mVoiceName.split("-");
            if(voiceParams.length != 3) {
                Log.e(LOG_TAG,"Incorrect voicename:" + mVoiceName);
            }
            else {
                mVoiceLanguage = voiceParams[0];
                mVoiceCountry = voiceParams[1];
                mVoiceVariant = voiceParams[2];

                mDebugData = new StringBuilder();

                try {
                    File file = new File(DATA_FILE);
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;

                    while ((line = br.readLine()) != null) {
                        mDebugData.append(line);
                        mDebugData.append('\n');
                    }
                    br.close();
                    mDebugText = mDebugData.toString();
                }
                catch (IOException e) {
                    //You'll need to add proper error handling here
                    Toast toast = Toast.makeText(mContext, "Error", Toast.LENGTH_SHORT);
                    toast.show();
                }
                parseSuccessful = true;
            }
        }

        if (parseSuccessful) {

            //Toast toast = Toast.makeText(mContext, "Parse succeeded", Toast.LENGTH_SHORT);
            //toast.show();
            mIsValidVoice = true;
            mVoicePath = getDataStorageBasePath() + "cg/" + mVoiceLanguage +
                    "/" + mVoiceCountry + "/" + mVoiceVariant + ".cg.flitevox";
            checkVoiceAvailability();
        }
        else {
            mIsValidVoice = false;

        }

    }

    private void checkVoiceAvailability() {
        Log.v(LOG_TAG, "Checking for Voice Available: " + mVoiceName);

        mIsVoiceAvailable = false;

        // The file should exist, as well as the MD5 sum should match.
        // Only then do we mark a voice as available.
        //
        // We can attempt getting an MD5sum, and an IOException will
        // tell us if the file didn't exist at all.

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "MD5 could not be computed");
            return;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(mVoicePath);
        }
        catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Voice File not found: " + mVoicePath);
            return;
        }

        byte[] dataBytes = new byte[1024];
        int nread = 0;
        try {
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not read voice file: " + mVoicePath);
            return;
        }
        finally {
            try {
                fis.close();
            } catch (IOException e) {
                // Ignoring this exception.
            }
        }

        byte[] mdbytes = md.digest();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        if (sb.toString().equals(mVoiceMD5)) {
            mIsVoiceAvailable = true;
            return;
        }
        else {
            Log.e(LOG_TAG,"Voice file found, but MD5 sum incorrect. Found" +
                    sb.toString() + ". Expected: " + mVoiceMD5);
            return;
        }
    }

    public boolean isValid() {
        return mIsValidVoice;
    }

    public boolean isAvailable() {
        return mIsVoiceAvailable;
    }

    public String getDebugText() { return mDebugText; }

    public String getName() {
        return mVoiceName;
    }

    public String getDisplayName() {
        Locale loc = new Locale(mVoiceLanguage, mVoiceCountry, mVoiceVariant);
        String displayName = loc.getDisplayLanguage() +
                "(" + loc.getDisplayCountry() + "," + loc.getVariant() + ")";
        return displayName;
    }

    public String getVariant() {
        return mVoiceVariant;
    }

    public String getDisplayLanguage() {
        Locale loc = new Locale(mVoiceLanguage, mVoiceCountry, mVoiceVariant);
        String displayLanguage = loc.getDisplayLanguage() +
                " (" + loc.getDisplayCountry() + ")";

        return displayLanguage;
    }

    public String getPath() {
        return mVoicePath;
    }

    public Locale getLocale() {
        return new Locale(mVoiceLanguage, mVoiceCountry, mVoiceVariant);
    }
}
