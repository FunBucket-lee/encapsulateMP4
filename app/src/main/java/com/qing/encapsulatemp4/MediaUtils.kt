package com.qing.encapsulatemp4

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.nio.ByteBuffer
import kotlin.math.abs

object MediaUtils {
    private val SDCARD_PATH = Environment.getExternalStorageDirectory().path
    private const val TAG = "MediaUtils"

    /**
     * 分离视频
     */
    @SuppressLint("WrongConstant")
    fun extractorVideo(context: Context) {
        //创建MediaExtractor
        val mediaExtractor = MediaExtractor()
        //初始化MediaMuxer
        val mediaMuxer: MediaMuxer?
        //轨道索引
        var videoIndex = -1

        try {
            //设置数据源
            mediaExtractor.setDataSource("$SDCARD_PATH/input.mp4")
            //设置轨道数
            val trackCount = mediaExtractor.trackCount
            for (i in 0 until trackCount) {
                //视频轨道格式信息
                val trackFormat = mediaExtractor.getTrackFormat(i)
                val mimeType = trackFormat.getString(MediaFormat.KEY_MIME)
                if (mimeType != null && mimeType.startsWith("video/")) {
                    videoIndex = i
                }
            }

            //切换到想要的轨道
            mediaExtractor.selectTrack(videoIndex)

            //视频轨道格式信息
            val mediaFormat = mediaExtractor.getTrackFormat(videoIndex)

            // 创建MediaMuxer实例，通过new MediaMuxer(String path, int format)指定视频文件输出路径和文件格式
            mediaMuxer =
                MediaMuxer(
                    "$SDCARD_PATH/output_video.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )

            //添加媒体通道
            val trackIndex = mediaMuxer.addTrack(mediaFormat)

            val byteBuffer = ByteBuffer.allocate(1024 * 500)
            val bufferInfo = MediaCodec.BufferInfo()

            ///添加完锁头track后调用start()方法，开始音视频合成
            mediaMuxer.start()

            //获取帧之间间隔时间
            val videoSampleTime: Long

            mediaExtractor.readSampleData(byteBuffer, 0)
            // 跳过第一个I帧
            if (mediaExtractor.sampleFlags == MediaExtractor.SAMPLE_FLAG_SYNC) {
                //读取下一帧数据
                mediaExtractor.advance()
            }

            mediaExtractor.readSampleData(byteBuffer, 0)
            val firstVideoPTS = mediaExtractor.sampleTime

            mediaExtractor.advance()
            mediaExtractor.readSampleData(byteBuffer, 0)

            val secondVideoPTS = mediaExtractor.sampleTime

            videoSampleTime = abs(secondVideoPTS - firstVideoPTS)
            Log.d(TAG, "extractorVideo: 视频帧间隔时间为 ===>  $videoSampleTime")

            mediaExtractor.unselectTrack(videoIndex)
            mediaExtractor.selectTrack(videoIndex)

            while (true) {
                //将样本数据存储到字节缓存区
                val readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0)
                if (readSampleSize < 0) {
                    break
                }

                //读取下一帧数据
                mediaExtractor.advance()

                bufferInfo.apply {
                    size = readSampleSize
                    offset = 0
                    flags = mediaExtractor.sampleFlags
                    presentationTimeUs += videoSampleTime
                }
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
            }

            mediaMuxer.stop()
            mediaExtractor.release()
            mediaMuxer.release()

            Toast.makeText(context, "视频分离完成", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "extractorVideo: 视频分离完成===========>finish")
        } catch (e: Exception) {
            Toast.makeText(context, "视频分离失败", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "extractorVideo: 视频分离失败===========>finish")
            e.printStackTrace()
        }
    }

    /**
     * 分类音频
     */
    @SuppressLint("WrongConstant")
    fun extractorAudio(context: Context) {
        //创建mediaExtractor实例
        val mediaExtractor = MediaExtractor()
        //初始化MediaMuxer
        val mediaMuxer: MediaMuxer

        //轨道索引
        var audioIndex = -1

        try {//设置数据源
            mediaExtractor.setDataSource("$SDCARD_PATH/input.mp4")
            //设置轨道数
            val trackCount = mediaExtractor.trackCount
            for (i in 0 until trackCount) {
                //视频轨道格式信息
                val trackFormat = mediaExtractor.getTrackFormat(i)
                val mimeType = trackFormat.getString(MediaFormat.KEY_MIME)
                if (mimeType != null && mimeType.startsWith("audio/")) {
                    audioIndex = i
                }
            }
            mediaExtractor.selectTrack(audioIndex)

            val trackFormat = mediaExtractor.getTrackFormat(audioIndex)
            mediaMuxer = MediaMuxer(
                "$SDCARD_PATH/output_audio.mp3",
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            //添加媒体通道
            val writeAudioIndex = mediaMuxer.addTrack(trackFormat)
            ///开始合成文件
            mediaMuxer.start()

            val byteBuffer = ByteBuffer.allocate(1024 * 500)
            val bufferInfo = MediaCodec.BufferInfo()

            val stampTime: Long

            ///获取帧之间的间隔时间
            mediaExtractor.readSampleData(byteBuffer, 0)
            ///跳过第一个I帧
            if (mediaExtractor.sampleFlags == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance()
            }

            ///获取第二帧
            mediaExtractor.readSampleData(byteBuffer, 0)
            val secondTime = mediaExtractor.sampleTime
            mediaExtractor.advance()

            ///获取第三帧
            mediaExtractor.readSampleData(byteBuffer, 0)
            val thirdTime = mediaExtractor.sampleTime
            stampTime = thirdTime - secondTime

            Log.d(TAG, "extractorAudio: 音频时间差=====>$stampTime")

            mediaExtractor.unselectTrack(audioIndex)
            mediaExtractor.selectTrack(audioIndex)

            while (true) {
                val readSampleData = mediaExtractor.readSampleData(byteBuffer, 0)
                if (readSampleData < 0) {
                    mediaExtractor.unselectTrack(audioIndex)
                    break
                }
                mediaExtractor.advance()
                bufferInfo.apply {
                    size = readSampleData
                    presentationTimeUs += stampTime
                    offset = 0
                    flags = mediaExtractor.sampleFlags
                }
                mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo)
            }

            mediaMuxer.stop()
            mediaExtractor.release()
            mediaMuxer.release()

            Toast.makeText(context, "音频分离成功=====>", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "extractorAudio: 音频分离成功")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "音频分离失败=====>", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "extractorAudio: 音频分离失败....")
        }
    }

    /**
     * 合成音视频
     */
    @SuppressLint("WrongConstant")
    fun muxerVideoAudio(context: Context) {
        try {
            // 以下过程是找到output_video.mp4中视频轨道
            val videoExtractor = MediaExtractor().apply {
                setDataSource("$SDCARD_PATH/output_video.mp4")
            }
            var videoFormat: MediaFormat? = null
            var videoIndex = -1
            val videoTrackCount = videoExtractor.trackCount
            for (i in 0 until videoTrackCount) {
                videoFormat = videoExtractor.getTrackFormat(i)
                //轨道类型
                val mineType = videoFormat.getString(MediaFormat.KEY_MIME)
                if (mineType != null && mineType.startsWith("video/")) {
                    videoIndex = i
                    break
                }
            }


            // 以下过程是找到output_audio.mp3中音频轨道
            val audioExtractor = MediaExtractor().apply {
                setDataSource("$SDCARD_PATH/output_audio.mp3")
            }
            var audioFormat: MediaFormat? = null
            var audioIndex = 1
            val audioTrackCount = audioExtractor.trackCount
            for (i in 0 until audioTrackCount) {
                audioFormat = audioExtractor.getTrackFormat(i)
                val mineType = audioFormat.getString(MediaFormat.KEY_MIME)
                if (mineType != null && mineType.startsWith("audio/")) {
                    audioIndex = i
                    break
                }
            }


            videoExtractor.selectTrack(videoIndex)
            audioExtractor.selectTrack(audioIndex)

            val audioBufferInfo = MediaCodec.BufferInfo()
            val videoBufferInfo = MediaCodec.BufferInfo()

            // 通过new MediaMuxer(String path, int format)指定视频文件输出路径和文件格式
            val mediaMuxer = MediaMuxer(
                "$SDCARD_PATH/output-composite.mp4",
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            // MediaMuxer添加媒体通道(视频)
            val videoAddTrack = mediaMuxer.addTrack(videoFormat!!)
            // MediaMuxer添加媒体通道(音频)
            val audioAddTrack = mediaMuxer.addTrack(audioFormat!!)

            //开始合成
            mediaMuxer.start()
            val byteBuffer = ByteBuffer.allocate(1024 * 500)

            //开始计算帧间隔时间
            val stampTime: Long
            videoExtractor.readSampleData(byteBuffer, 0)
            //第一个I帧跳过
            if (videoExtractor.sampleFlags == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoExtractor.advance()
            }

            videoExtractor.readSampleData(byteBuffer, 0)
            val secondTime = videoExtractor.sampleTime
            videoExtractor.advance()
            val thirdTime = videoExtractor.sampleTime
            stampTime = abs(thirdTime - secondTime)

            videoExtractor.unselectTrack(videoIndex)
            videoExtractor.selectTrack(videoIndex)
            audioExtractor.unselectTrack(audioIndex)
            audioExtractor.selectTrack(audioIndex)

            //开始写入视频轨道到合成MP4
            while (true) {
                val readSampleData = videoExtractor.readSampleData(byteBuffer, 0)
                if (readSampleData < 0) {
                    break
                }
                videoBufferInfo.apply {
                    size = readSampleData
                    presentationTimeUs += stampTime
                    offset = 0
                    flags = videoExtractor.sampleFlags
                }
                mediaMuxer.writeSampleData(videoAddTrack, byteBuffer, videoBufferInfo)
                videoExtractor.advance()
            }

            //开始写入音频轨道合成到MP4
            while (true) {
                val readSampleData = videoExtractor.readSampleData(byteBuffer, 0)
                if (readSampleData < 0) {
                    break
                }
                audioBufferInfo.apply {
                    size = readSampleData
                    presentationTimeUs += stampTime
                    offset = 0
                    flags = videoExtractor.sampleFlags
                }
                mediaMuxer.writeSampleData(audioAddTrack, byteBuffer, audioBufferInfo)
                videoExtractor.advance()

            }

            mediaMuxer.stop()
            mediaMuxer.release()
            videoExtractor.release()
            audioExtractor.release()
            Toast.makeText(context, "音视频合成成功", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "muxerVideoAudio: 音视频合成成功")

        } catch (e: Exception) {
            Toast.makeText(context, "音视频合成失败", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "muxerVideoAudio: 音视频合成失败")
            e.printStackTrace()
        }

    }
}