package com.android.remotedriving;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by Money on 2017/12/12. xxxxjf
 */

public class Encode {

    private MediaCodec codec;
    private IEncoderListener encoderListener;

    private int videoW;
    private int videoH;
    private int videoBitrate;
    private int videoFrameRate;

    private byte[] yuv420 = null;
    private byte[] rotateYuv420 = null;


    private static final String TAG = "Encode";
    private static final String MIME = "Video/AVC";

    public Encode(int videoW, int videoH, int videoBitrate, int videoFrameRate, IEncoderListener encoderListener) {
        this.videoW = videoW;
        this.videoH = videoH;
        this.videoBitrate = videoBitrate;
        this.videoFrameRate = videoFrameRate;
        this.encoderListener = encoderListener;

        initMediaCodec();
    }

    private void initMediaCodec() {
        try {
            codec = MediaCodec.createEncoderByType(MIME);
            yuv420 = new byte[getYuvBuffer(videoW, videoH)];
//            rotateYuv420 = new byte[getYuvBuffer(videoW, videoH)];
            MediaFormat format = MediaFormat.createVideoFormat(MIME, videoW, videoH);
            format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getYuvBuffer(int width, int height) {
        // stride = ALIGN(width, 16)
        int stride = (int) Math.ceil(width / 16.0) * 16;
        // y_size = stride * height
        int y_size = stride * height;
        // c_stride = ALIGN(stride/2, 16)
        int c_stride = (int) Math.ceil(width / 32.0) * 16;
        // c_size = c_stride * height/2
        int c_size = c_stride * height / 2;
        // size = y_size + c_size * 2
        return y_size + c_size * 2;
    }

    public void encoderYUV420(byte[] input) {
        try {
            NV21ToNV12(input, yuv420, videoW, videoH);
//            YUV420spRotate90Anticlockwise(rotateYuv420, yuv420, videoW, videoH);
            int inputBufferIndex = codec.dequeueInputBuffer(50);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(yuv420);
                codec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, System.currentTimeMillis(), 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                byte[] outData = new byte[outputBuffer.remaining()];
                outputBuffer.get(outData, 0, outData.length);
                if (encoderListener != null) {
                    encoderListener.onH264(outData);
                }
                codec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void YUV420spRotate90Anticlockwise(byte[] src, byte[] des, int width, int height) {
        int wh = width * height;
        //旋转Y
        int k = 0;
        for(int i = 0; i < width; i++) {
            for (int j = height - 1; j >= 0; j--) {
                des[k++] = src[j * width + i]; /// change des[k] = src[nPos + i];
            }
        }

        //旋转UV
        int uvHeight = height >> 1;
        int uvWidth = width >> 1;

        for(int i = 0;i < width; i += 2){
            for(int j = uvHeight - 1;j >= 0; j--){
                des[k] = src[wh + width * j + i];
                des[k+1] = src[wh + width * j + i + 1];
                k += 2;
            }
        }
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    public void releaseMediaCodec() {
        if (codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
    }

    public interface IEncoderListener {
        void onH264(byte[] data);
    }

    //通过mimeType确定支持的格式
    private int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }
}
