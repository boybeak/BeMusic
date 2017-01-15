# 如何用MediaPlayer写一个正经的音乐播放器

先贴上示例项目的Github地址：

[BePlayer](https://github.com/boybeak/BeMusic)

按照以下顺序介绍如何用MediaPlayer去构建一个基础的本地音乐播放器。

1. 获取本地音乐数据；
2. 构建PlayService，来执行音乐播放任务；
3. 构建一个UI与PlayService的中间层——PlayManager，用来处理媒体文件的Playback生命周期；
4. 在PlayManager中，加入处理意外情况的方式，所谓意外情况，例如耳机拔出、接到电话、其他播放器播放音乐等；
5. 实现远程控制，例如Notification与锁屏控制；

以上是已经实现的部分，以后再逐渐完善的有：

- 桌面Widget播放控件以及控制；
- 播放的可视化效果；
- 歌词支持；
- MediaCodec支持；

### 获取本地音乐数据

最快的获取本地音乐信息的方式，就是通过ContentProvider获取，我们先构建一个model类[Song.java](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/model/Song.java)去表示音乐文件:

```java
public class Song {
    private String title, titleKey, artist, artistKey,
            album, albumKey, displayName, mimeType, path;
    private int id, albumId, artistId, duration, size, year, track;
    private boolean isRingtone, isPodcast, isAlarm, isMusic, isNotification;

    //private File mCoverFile;

    private Album albumObj;

    public Song (Bundle bundle) {
        id = bundle.getInt(MediaStore.Audio.Media._ID);
        title = bundle.getString(MediaStore.Audio.Media.TITLE);
        titleKey = bundle.getString(MediaStore.Audio.Media.TITLE_KEY);
        artist = bundle.getString(MediaStore.Audio.Media.ARTIST);
        artistKey = bundle.getString(MediaStore.Audio.Media.ARTIST_KEY);
        //mComposer = bundle.getString(MediaStore.Audio.Media.COMPOSER);
        album = bundle.getString(MediaStore.Audio.Media.ALBUM);
        albumKey = bundle.getString(MediaStore.Audio.Media.ALBUM_KEY);
        displayName = bundle.getString(MediaStore.Audio.Media.DISPLAY_NAME);
        year = bundle.getInt(MediaStore.Audio.Media.YEAR);
        mimeType = bundle.getString(MediaStore.Audio.Media.MIME_TYPE);
        path = bundle.getString(MediaStore.Audio.Media.DATA);

        artistId = bundle.getInt(MediaStore.Audio.Media.ARTIST_ID);
        albumId = bundle.getInt(MediaStore.Audio.Media.ALBUM_ID);
        track = bundle.getInt(MediaStore.Audio.Media.TRACK);
        duration = bundle.getInt(MediaStore.Audio.Media.DURATION);
        size = bundle.getInt(MediaStore.Audio.Media.SIZE);
        isRingtone = bundle.getInt(MediaStore.Audio.Media.IS_RINGTONE) == 1;
        isPodcast = bundle.getInt(MediaStore.Audio.Media.IS_PODCAST) == 1;
        isAlarm = bundle.getInt(MediaStore.Audio.Media.IS_ALARM) == 1;
        isMusic = bundle.getInt(MediaStore.Audio.Media.IS_MUSIC) == 1;
        isNotification = bundle.getInt(MediaStore.Audio.Media.IS_NOTIFICATION) == 1;
    }
```

然后从ContentProvider中获得手机上的音乐文件：

```java
public static List<Song> getAudioList(Context context) {


        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AUDIO_KEYS,
                MediaStore.Audio.Media.IS_MUSIC + "=" + 1,
                null,
                null);
        return getAudioList(cursor);
    }

    private static List<Song> getAudioList (Cursor cursor) {
        List<Song> audioList = null;
        if (cursor.getCount() > 0) {
            audioList = new ArrayList<Song>();
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
        }

        cursor.close();
        return audioList;
    }
```

这段代码具体可参考[MediaUtils.java](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/utils/MediaUtils.java)。

经过以上代码，便可以得到手机内的所有音乐文件。

### 构建PlayService，来执行音乐播放任务

我们都知道，长时间的后台任务，需要放在Service中进行，我们称这个用来播放音乐的Service为[PlayService](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/service/PlayService.java)。

在这个Service类中，我们持有MediaPlayer实例，并实现OnPreparedListener，OnCompletionListener，OnErrorListener等。

我们如此实例化MediaPlayer

```java
private void ensurePlayer () {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
        setPlayerState(STATE_IDLE);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnSeekCompleteListener(this);
    }
```

当需要进行音乐播放的时候，再执行此方法

```java
public void startPlayer (String path) {
        //releasePlayer();
        ensurePlayer();
        try {
            mPlayer.setDataSource(path);
            setPlayerState(STATE_INITIALIZED);
            mPlayer.prepareAsync();
            setPlayerState(STATE_PREPARING);
        } catch (IOException e) {
            e.printStackTrace();
            releasePlayer();
        }
    }
```

在PlayService类中，我们声明一系列播放周期的状态。

```java
public static final int STATE_IDLE = 0, STATE_INITIALIZED = 1, STATE_PREPARING = 2,
            STATE_PREPARED = 3, STATE_STARTED = 4, STATE_PAUSED = 5, STATE_STOPPED = 6,
            STATE_COMPLETED = 7, STATE_RELEASED = 8, STATE_ERROR = -1;
@IntDef({STATE_IDLE, STATE_INITIALIZED, STATE_PREPARING,
            STATE_PREPARED, STATE_STARTED, STATE_PAUSED,
            STATE_STOPPED, STATE_COMPLETED, STATE_RELEASED,
            STATE_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    private @State int mState = STATE_IDLE;
```

具体的声明周期图，可以参考谷歌文档中关于[MeidaPlayer](https://developer.android.google.cn/reference/android/media/MediaPlayer.html)部分的说明。主要参见下图：

![https://github.com/boybeak/BeMusic/blob/master/app/mediaplayer_state_diagram.gif](https://github.com/boybeak/BeMusic/blob/master/app/mediaplayer_state_diagram.gif)



