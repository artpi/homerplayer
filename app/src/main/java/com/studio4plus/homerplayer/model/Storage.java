package com.studio4plus.homerplayer.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class Storage implements AudioBook.UpdateObserver {

    private static final String PREFERENCES_NAME = Storage.class.getSimpleName();
    private static final String AUDIOBOOK_KEY_PREFIX = "audiobook_";
    private static final String LAST_AUDIOBOOK_KEY = "lastPlayedId";

    private static final String FIELD_POSITION = "position";
    private static final String FIELD_COLOUR_SCHEME = "colourScheme";
    private static final String FIELD_POSITION_FILEPATH = "filePath";
    private static final String FIELD_POSITION_SEEK = "seek";
    private static final String FIELD_FILE_DURATIONS = "fileDurations";


    private final SharedPreferences preferences;

    public Storage(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        EventBus.getDefault().register(this);
    }

    public void readAudioBookState(AudioBook audioBook) {
        String bookData = preferences.getString(getAudioBookPreferenceKey(audioBook.getId()), null);
        if (bookData != null) {
            try {
                ColourScheme colourScheme = null;
                List<Long> durations = null;

                JSONObject jsonObject = (JSONObject) new JSONTokener(bookData).nextValue();
                JSONObject jsonPosition = jsonObject.getJSONObject(FIELD_POSITION);
                String fileName = jsonPosition.getString(FIELD_POSITION_FILEPATH);
                long seek = jsonPosition.getLong(FIELD_POSITION_SEEK);
                Position position = new Position(fileName, seek);

                String colourSchemeName = jsonObject.optString(FIELD_COLOUR_SCHEME, null);
                if (colourSchemeName != null) {
                    colourScheme = ColourScheme.valueOf(colourSchemeName);
                }

                JSONArray jsonDurations = jsonObject.optJSONArray(FIELD_FILE_DURATIONS);
                if (jsonDurations != null) {
                    final int count = jsonDurations.length();
                    durations = new ArrayList<>(count);
                    for (int i = 0; i < count; ++i)
                        durations.add(jsonDurations.getLong(i));
                }

                audioBook.restore(colourScheme, position, durations);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeAudioBookState(AudioBook audioBook) {
        JSONObject jsonAudioBook = new JSONObject();
        JSONObject jsonPosition = new JSONObject();
        Position position = audioBook.getLastPosition();
        try {
            jsonPosition.put(FIELD_POSITION_FILEPATH, position.filePath);
            jsonPosition.put(FIELD_POSITION_SEEK, position.seekPosition);
            JSONArray jsonDurations = new JSONArray(audioBook.getFileDurations());
            jsonAudioBook.put(FIELD_POSITION, jsonPosition);
            jsonAudioBook.putOpt(FIELD_COLOUR_SCHEME, audioBook.getColourScheme());
            jsonAudioBook.put(FIELD_FILE_DURATIONS, jsonDurations);

            SharedPreferences.Editor editor = preferences.edit();
            String key = getAudioBookPreferenceKey(audioBook.getId());
            editor.putString(key, jsonAudioBook.toString());
            editor.apply();
        } catch (JSONException e) {
            // Should never happen, none of the values is null, NaN nor Infinity.
            e.printStackTrace();
        }
    }

    public String getCurrentAudioBook() {
        return preferences.getString(LAST_AUDIOBOOK_KEY, null);
    }

    public void writeCurrentAudioBook(String id) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_AUDIOBOOK_KEY, id);
        editor.apply();
    }

    @Override
    public void onAudioBookStateUpdated(AudioBook audioBook) {
        writeAudioBookState(audioBook);
    }

    private String getAudioBookPreferenceKey(String id) {
        return AUDIOBOOK_KEY_PREFIX + id;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(CurrentBookChangedEvent event) {
        writeCurrentAudioBook(event.audioBook.getId());
    }
}
