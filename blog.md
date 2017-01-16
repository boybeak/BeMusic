# 如何用MediaPlayer写一个正经的音乐播放器

先贴上示例项目的Github地址：

[BePlayer](https://github.com/boybeak/BeMusic)

Demo应用Play Store地址:

<a href="https://play.google.com/store/apps/details?id=com.nulldreams.bemusic">
<img src="https://github.com/boybeak/BeMusic/blob/master/app/en_badge_web_generic.png" width="161" height="62"/>
<a/>

[BePlayer](https://play.google.com/store/apps/details?id=com.nulldreams.bemusic)

按照以下顺序介绍如何用MediaPlayer去构建一个基础的本地音乐播放器。

1. 获取本地音乐数据；
2. 构建PlayService，来执行音乐播放任务；
3. 构建一个UI与PlayService的中间层——PlayManager，用来处理媒体文件的Playback生命周期；
4. 在PlayManager中，加入处理意外情况的方式，所谓意外情况，例如耳机拔出、接到电话、其他播放器播放音乐等；
5. 实现远程控制与PlayService保活，例如Notification与锁屏控制；

以上是已经实现的部分，以后再逐渐完善的有：

- 桌面Widget播放控件以及控制；
- 自定义播放列表支持；
- 视频播放支持；
- 远端媒体播放支持；
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

注意在PlayService中的onStartCommand方法的返回值为Service.START_STICKY。

详见Service中关于[START_STICKY](https://developer.android.google.cn/reference/android/app/Service.html#START_STICKY)的解释，其中有这么一段：

> ### START_STICKY
>
> Added in [API level 5](https://developer.android.google.cn/guide/topics/manifest/uses-sdk-element.html#ApiLevels)
>
> ```
> int START_STICKY
> ```
>
> ……..
>
> This mode makes sense for things that will be explicitly started and stopped to run for arbitrary periods of time, such as a service performing background music playback.

可见返回START_STICKY适用于执行音乐播放的Service。

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

在ensurePlayer这个方法中，状态变更为STATE_IDLE；在MediaPlayer中setDataSource后，状态变更为STATE_INITIALIZED；MediaPlayer执行prepareAsync后，状态变更为STATE_PREPAREING。其余的关键的涉及到播放周期变化的方法如下：

```java
@Override
public void onPrepared(MediaPlayer mp) {
  setPlayerState(STATE_PREPARED);
  doStartPlayer();
}
//state -> STATE_PREPARED

private void doStartPlayer () {
  mPlayer.start();
  setPlayerState(STATE_STARTED);
}
public void resumePlayer () {
  if (isPaused()) {
    doStartPlayer();
  }
}
//state -> STATE_STARTED

public void pausePlayer () {
  if (isStarted()) {
    mPlayer.pause();
    setPlayerState(STATE_PAUSED);
  }
}
//state -> STATE_PAUSED

@Override
public void onCompletion(MediaPlayer mp) {
  setPlayerState(STATE_COMPLETED);
}
//state -> STATE_COMPLETED

public void releasePlayer () {
  if (mPlayer != null) {
    mPlayer.release();
    mPlayer = null;
    setPlayerState(STATE_RELEASED);
  }
}
//state -> STATE_RELEASED

@Override
public boolean onError(MediaPlayer mp, int what, int extra) {
  setPlayerState(STATE_ERROR);
  return false;
}
//state -> STATE_ERROR
```

Service中的其他关键部分

```java
public class PlayBinder extends Binder {
  public PlayService getService () {
  	return PlayService.this;
  }
}
```

用来通过bindService的时候返回PlayService的实例。



### UI与PlayService的中间层——PlayManager

我们将其他的播放逻辑放在这个中间层中，例如下一曲，上一曲，播放规则（单曲循环，列表循环，随机播放等）锁屏显示与Notification显示，还有意外情况的处理，例如失去AudioFocus、耳机插拔、收到电话等。

单例化PlayManager

```java
private static PlayManager sManager = null;

	public synchronized static PlayManager getInstance (Context context) {
  		if (sManager == null) {
    	sManager = new PlayManager(context.getApplicationContext());
  	}
  	return sManager;
}
```

首先我们要在这个中间层[PlayManager](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/PlayManager.java)里获得[PlayService](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/service/PlayService.java)的实例:

```java
private void bindPlayService () {
    mContext.bindService(new Intent(mContext, PlayService.class), mConnection, Context.BIND_AUTO_CREATE);
}
private void startPlayService () {
  mContext.startService(new Intent(mContext, PlayService.class));
}
private ServiceConnection mConnection = new ServiceConnection() {
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    mService = ((PlayService.PlayBinder)service).getService();
    mService.setPlayStateChangeListener(PlayManager.this);
    Log.v(TAG, "onServiceConnected");
    startRemoteControl();
    if (!isPlaying()) {
      dispatch(mSong);
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    Log.v(TAG, "onServiceDisconnected " + name);
    mService.setPlayStateChangeListener(null);
    mService = null;

    startPlayService();
    bindPlayService();
  }
};
```

通常与Service交互，有两种方式，startService和bindService，但是这里要startService与bindService同时进行。这两种方式并不矛盾，详细可以参见[绑定服务](https://developer.android.google.cn/guide/components/bound-services.html)中的相关描述。

> ### 绑定到已启动服务
>
> 正如[服务](https://developer.android.google.cn/guide/components/services.html)文档中所述，您可以创建同时具有已启动和绑定两种状态的服务。 也就是说，可通过调用 `startService()`启动该服务，让服务无限期运行；此外，还可通过调用 `bindService()` 使客户端绑定到服务。
>
> 如果您确实允许服务同时具有已启动和绑定状态，则服务启动后，系统“不会”在所有客户端都取消绑定时销毁服务。 为此，您必须通过调用`stopSelf()` 或 `stopService()` 显式停止服务。
>
> 尽管您通常应该实现 `onBind()` *或*`onStartCommand()`，但有时需要同时实现这两者。例如，**音乐播放器可能发现让其服务无限期运行并同时提供绑定很有用处**。 这样一来，Activity 便可启动服务进行音乐播放，即使用户离开应用，音乐播放也不会停止。 然后，当用户返回应用时，Activity 可绑定到服务，重新获得回放控制权。
>
> 请务必阅读[管理绑定服务的生命周期](https://developer.android.google.cn/guide/components/bound-services.html#Lifecycle)部分，详细了解有关为已启动服务添加绑定时该服务的生命周期信息。

其中特别提到了“音乐播放器可能发现让其服务无限期运行并同时提供绑定很有用处”。

获取到了PlayService的实例后，便可以正式开始音乐的播放了。音乐播放的方法在PlayManager中的dispatch中。

```java
/**
* dispatch a song.If the song is paused, then resume.If the song is not started, then start it.If the song is playing, then pause it.
* {@link PlayService#STATE_COMPLETED}
* @param song the song you want to dispatch, if null, dispatch a song from {@link Rule}.
* @see Song;
* @see com.nulldreams.media.manager.ruler.Rule#next(Song, List, boolean);
*/
public void dispatch(final Song song) {
  Log.v(TAG, "dispatch song=" + song);
  Log.v(TAG, "dispatch getAudioFocus mService=" + mService);
  if (mCurrentList == null || mCurrentList.isEmpty()) {
    return;
  }
  //mCurrentAlbum = null;
  if (mService != null) {
    if (song == null && mSong == null) {
      Song defaultSong = mPlayRule.next(song, mCurrentList, false);
      dispatch(defaultSong);
    } else if (song.equals(mSong)) {
      if (mService.isStarted()) {
        //Do really this action by user
        pause();
      } else if (mService.isPaused()){
        resume();
      } else {
        mService.releasePlayer();
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
          mSong = song;
          mService.startPlayer(song.getPath());
        }
      }
    } else {
      mService.releasePlayer();
      if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
        mSong = song;
        mService.startPlayer(song.getPath());
      }
    }

  } else {
    Log.v(TAG, "dispatch mService == null");
    mSong = song;
    bindPlayService();
    startPlayService();
  }

}
/**
*  dispatch the current song
*/
public void dispatch () {
  dispatch(mSong);
}
```

这个dispatch方法中，会根据播放状态和当前正在进行的歌曲，判断是否开始播放，暂停还是恢复播放。

在这个过程中，还涉及到获取音频焦点AudioFocus，只有当获取到了音频焦点，再开始播放，获取AudioFocus代码如下：

```java
private int requestAudioFocus () {
  AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
  Log.v(TAG, "requestAudioFocus by ");
  return audioManager.requestAudioFocus(
    mAfListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
}

private int releaseAudioFocus () {
  AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
  Log.v(TAG, "releaseAudioFocus by ");
  return audioManager.abandonAudioFocus(mAfListener);
}
```

当失去音频焦点的时候，我们可以进行以下处理：

```java
private AudioManager.OnAudioFocusChangeListener mAfListener = new AudioManager.OnAudioFocusChangeListener() {
  @Override
  public void onAudioFocusChange(int focusChange) {
    Log.v(TAG, "onAudioFocusChange = " + focusChange);
    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
        focusChange == AudioManager.AUDIOFOCUS_LOSS) {
      if (isPlaying()) {
        pause(false);
      }
    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      if (isPaused() && !isPausedByUser()) {
        resume();
      }
    }
  }
};
```

失去AudioFocus的时候，我们暂停播放；重新获得AudioFocus的时候，判断是否为用户主动暂停，若不是主动暂停，则恢复播放。

```java
/**
* resume play
*/
public void resume () {
  if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
    mService.resumePlayer();
  }
}

/**
* pause a playing song by user action
*/
public void pause () {
  pause(true);
}

/**
* pause a playing song
* @param isPausedByUser false if triggered by {@link AudioManager#AUDIOFOCUS_LOSS} or
*                       {@link AudioManager#AUDIOFOCUS_LOSS_TRANSIENT}
*/
private void pause (boolean isPausedByUser) {
  mService.pausePlayer();
  this.isPausedByUser = isPausedByUser;
}
```

其他相关的用户控制方法，如上一曲，下一曲等：

```java
/**
* next song by user action
*/
public void next() {
  next(true);
}

/**
* next song triggered by {@link #onStateChanged(int)} and {@link PlayService#STATE_COMPLETED}
* @param isUserAction
*/
private void next(boolean isUserAction) {
  dispatch(mPlayRule.next(mSong, mCurrentList, isUserAction));
}

/**
* previous song by user action
*/
public void previous () {
  previous(true);
}

private void previous (boolean isUserAction) {
  dispatch(mPlayRule.previous(mSong, mCurrentList, isUserAction));
}
```

其中涉及到的mPlayRule，指上一曲下一曲的规则，例如**单曲循环、列表循环、随机播放**等。库中提供了这样一个接口[Rule](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/ruler/Rule.java)来实现播放规则。

```java
public interface Rule {
    Song previous (Song song, List<Song> songList, boolean isUserAction);
    Song next(Song song, List<Song> songList, boolean isUserAction);
    void clear ();
}
```

同时内置了**单曲循环、列表循环、随机播放**三种播放规则，可以通过[Rulers](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/ruler/Rulers.java)使用这三种规则。

### 处理意外情况的方式

所谓意外状况包括插拔耳机与突然来电，这些处理都可以用一个BroadcastReceiver来处理。只需要这个BroadcastReceiver监听。

```java
private SimpleBroadcastReceiver mNoisyReceiver = new SimpleBroadcastReceiver() {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
      // Pause the playback
      pause(false);
    }
  }
};
private void registerNoisyReceiver () {
  mNoisyReceiver.register(mContext, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
}

private void unregisterNoisyReceiver () {
  mNoisyReceiver.unregister(mContext);
}
```

其中的[SimpleBroadcastReceiver](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/receiver/SimpleBroadcastReceiver.java)简单处理了一下，避免重复注册或者未注册即注销时候产生的崩溃。拔出耳机跟收到来电用这个来处理，就可以了，不需监听拔出耳机和来电，所以说，任何企图获取你电话权限的音乐播放应用，肯定不是为了更好的提供音乐服务，只是为了获取更多隐私。

### 实现远程控制与PlayService保活

#### Notification远程控制与保活

由于安卓系统对于系统资源的一些控制，导致即便是耗时任务放在Service中进行，也不能确保在放置于后台后，能一定存活。这就需要我们使用一些方式确保播放后台一直存活下去。最直接的方式，就是通过Service的startForground方法，去显示一个ONGOING的Notification。

PlayManager中已经做了相关的逻辑处理，不过如果要自定义样式，则需要你设置一个[NotificationAgent](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/notification/NotificationAgent.java)，通过这个接口，返回一个supportV7包中的NotificationCompat.Builder。

```java
public interface NotificationAgent {
    /**
     * custom your notification style
     * @param context
     * @param manager
     * @param state
     * @param song
     * @return
     */
    NotificationCompat.Builder getBuilder (Context context, PlayManager manager, @PlayService.State int state, Song song);

    /**
     * you can recycle a bitmap in this method after the notification is already shown
     */
    void afterNotify();
}
```

具体可以参考示例程序中的[SimpleAgent](https://github.com/boybeak/BeMusic/tree/master/app/src/main/java/com/nulldreams/bemusic/play)类。对于Notification的删除处理，默认的方式是：

Kitkat版本以上（不包括Kitkat），暂停播放后，直接滑动删除，PlayManager就可以释放播放；

Kitkat版本以下（包括Kitkat），右上角显示一个x号，点击直接停止并释放播放。

在[SimpleAgent](https://github.com/boybeak/BeMusic/tree/master/app/src/main/java/com/nulldreams/bemusic/play)中，使用了[MediaStyle](https://developer.android.google.cn/reference/android/support/v7/app/NotificationCompat.MediaStyle.html)能够完美适配各种定制系统，并且配合之后的锁屏控制十分方便。

#### 锁屏控制

锁屏控制的关键类是[MediaSessionCompat](https://developer.android.google.cn/reference/android/support/v4/media/session/MediaSessionCompat.html)，另外还有两个类十分关键[MediaMetadataCompat](https://developer.android.google.cn/reference/android/support/v4/media/MediaMetadataCompat.html)和[PlaybackStateCompat](https://developer.android.google.cn/reference/android/support/v4/media/session/PlaybackStateCompat.html)。

通过[MediaMetadataCompat](https://developer.android.google.cn/reference/android/support/v4/media/MediaMetadataCompat.html)设置锁屏中显示的歌曲信息，例如歌曲名称、歌手名称、专辑、专辑封面等；通过[PlaybackStateCompat](https://developer.android.google.cn/reference/android/support/v4/media/session/PlaybackStateCompat.html)可以设置锁屏的操作，例如上一曲、下一曲、暂停、恢复播放等。

具体使用可以参考[PlayManager](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/PlayManager.java)中的四个方法startRemoteControl、changeMediaSessionMetadata、changeMediaSessionState、stopRemoteControl，以及谷歌教学视频：

YouTube地址:[Media Playback with MediaSessionCompat (Android Development Patterns Ep 4)](https://www.youtube.com/watch?v=FBC1FgWe5X4)

优酷地址:[Media Playback with MediaSessionCompat ](http://v.youku.com/v_show/id_XMTY2NjY0ODQ4NA==.html?spm=a2hzp.8253876.0.0.l2vWcr&f=27790253&from=y1.7-3)



---------------------------------------------------

###### 参考文章

[MediaPlayer开发者文档](https://developer.android.google.cn/reference/android/media/MediaPlayer.html)

[MediaPlayer API Guides](https://developer.android.google.cn/guide/topics/media/mediaplayer.html#)

[Service开发者文档](https://developer.android.google.cn/reference/android/app/Service.html)

[Service API Guides](https://developer.android.google.cn/guide/components/services.html)

[服务绑定](https://developer.android.google.cn/guide/components/bound-services.html#Creating)

[MediaSessionCompat开发者文档](https://developer.android.google.cn/reference/android/support/v4/media/session/MediaSessionCompat.html)

[MediaMetadataCompat开发者文档](https://developer.android.google.cn/reference/android/support/v4/media/MediaMetadataCompat.html)

[PlaybackStateCompat开发者文档](https://developer.android.google.cn/reference/android/support/v4/media/session/PlaybackStateCompat.html)

[Media playback the right way](https://www.youtube.com/watch?v=XQwe30cZffg)