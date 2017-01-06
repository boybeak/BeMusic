package com.nulldreams.media.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;

import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by boybe on 2016/12/26.
 */

public class MediaUtils {

    private static final String TAG = MediaUtils.class.getSimpleName();

    public static final DecimalFormat FORMAT = new DecimalFormat("00");

    public static final String[] AUDIO_KEYS = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TITLE_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST_KEY,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
    };

    public static final String[] ALBUM_COLUMNS = new String[] {
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_KEY,
            MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.LAST_YEAR,
    };

    public static List<Album> getAlbumList (Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_COLUMNS,
                null,
                null,
                null);
        int count = cursor.getCount();
        if (count > 0) {
            List<Album> albumList = new ArrayList<>();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                int id, minYear, maxYear, numSongs;
                String album, albumKey, artist, albumArt;
                id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                minYear = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR));
                maxYear = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR));
                numSongs = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
                artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                Album albumObj = new Album(id, minYear, maxYear, numSongs,
                        album, albumKey, artist, albumArt);
                albumList.add(albumObj);
            }
            return albumList;
        }
        return null;
    }

    public static List<Song> getAudioList(Context context) {
        List<Song> audioList = new ArrayList<Song>();

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AUDIO_KEYS,
                null,
                null,
                null);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Bundle bundle = new Bundle ();
            for (int i = 0; i < AUDIO_KEYS.length; i++) {
                final String key = AUDIO_KEYS[i];
                final int columnIndex = cursor.getColumnIndex(key);
                final int type = cursor.getType(columnIndex);
                switch (type) {
                    case Cursor.FIELD_TYPE_BLOB:
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        float floatValue = cursor.getFloat(columnIndex);
                        bundle.putFloat(key, floatValue);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        int intValue = cursor.getInt(columnIndex);
                        bundle.putInt(key, intValue);
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        String strValue = cursor.getString(columnIndex);
                        bundle.putString(key, strValue);
                        break;
                }
            }
            Song audio = new Song(bundle);
            audioList.add(audio);
        }
        cursor.close();
        return audioList;
    }

    public static String formatTime (int durationInMilliseconds) {
        int seconds = durationInMilliseconds /  1000;
        int minutes = seconds / 60;
        int secondsRemain = seconds % 60;
        return FORMAT.format(minutes) + ":" + FORMAT.format(secondsRemain);
    }
}
