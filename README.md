# BeMusic
It is a local music player, show you how to use MediaPlayer to make a simple MusicPlayer.

Play Store地址:

<a href="https://play.google.com/store/apps/details?id=com.nulldreams.bemusic">
<img src="https://github.com/boybeak/BeMusic/blob/master/app/en_badge_web_generic.png" width="161" height="62"/>
<a/>

[BePlayer](https://play.google.com/store/apps/details?id=com.nulldreams.bemusic)

<img src="https://github.com/boybeak/BeMusic/blob/master/app/play_detail.png" width="180" height="320"/>
<img src="https://github.com/boybeak/BeMusic/blob/master/app/album_list.png" width="180" height="320"/>
<img src="https://github.com/boybeak/BeMusic/blob/master/app/play_list.png" width="180" height="320"/>

<img src="https://github.com/boybeak/BeMusic/blob/master/app/play_detail_land.png" width="320" height="180"/>
<img src="https://github.com/boybeak/BeMusic/blob/master/app/album_list_land.png" width="320" height="180"/>
<img src="https://github.com/boybeak/BeMusic/blob/master/app/play_list_land.png" width="320" height="180"/>

## Getting Start
你只需要关心的就是[PlayManager](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/PlayManager.java)这个类，这个类中，已经有了对播放状态对生命周期的维护，同时包括了对AudioFocus,ACTION_AUDIO_BECOMING_NOISY,锁屏等。具体见下图：
<img src="https://github.com/boybeak/BeMusic/blob/master/app/img.jpeg" width="737" height="370"/>

####实例化[PlayManager](https://github.com/boybeak/BeMusic/blob/master/media/src/main/java/com/nulldreams/media/manager/PlayManager.java)

```
PlayManager.getInstance (context);
```
####几个重要的方法
```
public void dispatch(final Song song);
public void dispatch();
public void setRule (@NonNull Rule rule);
public void next();
public void previous ();
public void pause ();
public void seekTo (int position);
```
[代码解析参见blog.md](https://github.com/boybeak/BeMusic/blob/master/blog.md)