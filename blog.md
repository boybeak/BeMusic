# 如何用MediaPlayer写一个正经的音乐播放器

先贴上示例项目的Github地址：

[BePlayer]: https://github.com/boybeak/BeMusic

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

##获取本地音乐数据