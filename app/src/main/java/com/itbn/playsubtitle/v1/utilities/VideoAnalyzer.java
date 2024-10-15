package com.itbn.playsubtitle.v1.utilities;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import io.github.nailik.androidresampler.Resampler;
import io.github.nailik.androidresampler.ResamplerConfiguration;
import io.github.nailik.androidresampler.data.ResamplerChannel;
import io.github.nailik.androidresampler.data.ResamplerQuality;
import com.itbn.playsubtitle.v1.ActivityHelper;

public class VideoAnalyzer {
    
    private Context context;
    private String videoPath;
    public int channelCount, sampleRate, outputSampleRate, outputChannelCount;
    private byte[] mono, rawPCM = null;
    private MediaExtractor extractor = null;
    public OutputStream output = null;
    public String tempFilePath = "/storage/emulated/0/PlaySubtitle/Audio/raw_data.pcm";
    
    public VideoAnalyzer(Context _context, String _videoPath, int _samplerate, int _channelcount) {
        context = _context;
        videoPath = _videoPath;
        outputSampleRate = _samplerate;
        outputChannelCount = _channelcount;
    }
    
    public void extractAudio() {
        ActivityHelper.displayMessage("Extracting audio...");
        
        try {
            output = new FileOutputStream(new File(tempFilePath));
            extractor = new MediaExtractor();
            extractor.setDataSource(videoPath);
            
            MediaCodec codec = null;
            MediaFormat format = null;
            
            for (int index = 0; index < extractor.getTrackCount(); index++) {
                format = extractor.getTrackFormat(index);
                String mime = format.getString(MediaFormat.KEY_MIME);
                
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(index);
                    codec = MediaCodec.createDecoderByType(mime);
                    codec.configure(format, null, null, 0);
                    break;
                }
            }
            
            if (codec == null) {
                ActivityHelper.prompt(context, "Can't find audio info");
            }
            
            codec.start();
            
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            
            boolean isEndOfStream = false;
            
            while (true) {
                if (!isEndOfStream) {
                    int inputIndex = codec.dequeueInputBuffer(10000);
                    
                    if (inputIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inputIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        
                        if (sampleSize < 0) {
                            //showMessage("InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEndOfStream = true;
                        } else {
                            long timestamp = info.presentationTimeUs;
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, timestamp, 0);
                            extractor.advance();
                        }
                    }
                }
                
                int outputIndex = codec.dequeueOutputBuffer(info, 10000);
                
                switch (outputIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    //showMessage("INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = codec.getOutputBuffers();
                    break;
                    
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    String outputFormat = codec.getOutputFormat().toString();
                    channelCount = getChannelCount(outputFormat);
                    sampleRate = getSampleRate(outputFormat);
                    break;
                    
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                    //showMessage("dequeueOutputBuffer timed out!");
                    break;
                    
                    default:
                    ByteBuffer buffer = outputBuffers[outputIndex];
                    
                    final byte[] chunk = new byte[info.size - info.offset];
                    buffer.get(chunk);
                    buffer.clear();
                    
                    if (chunk.length > 0) {
                        changeStereoToMono(chunk);
                        
                        if (outputSampleRate == 0 && outputChannelCount == 0) {
                            resampleByteArray(mono, sampleRate, sampleRate, channelCount);
                        } else if (outputSampleRate == 0 && outputChannelCount != 0) {
                            resampleByteArray(mono, sampleRate, sampleRate, outputChannelCount);
                        } else if (outputSampleRate != 0 && outputChannelCount == 0) {
                            resampleByteArray(mono, sampleRate, outputSampleRate, channelCount);
                        } else {
                            resampleByteArray(mono, sampleRate, outputSampleRate, outputChannelCount);
                        }
                        
                        output.write(rawPCM);
                        output.flush();
                    }
                    
                    codec.releaseOutputBuffer(outputIndex, true);
                    break;
                }
                
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    //showMessage("OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }
            
            if (output != null) {
                output.close();
                codec.stop();
                codec.release();
                extractor.release();
            }
            
        } catch (IOException e) {
            ActivityHelper.prompt(context, e.getMessage());
        }
    }
    
    private void changeStereoToMono(byte[] buff) {
        if (isStereo()) {
            mono = new byte[buff.length / 2];
            
            int HI = 1;
            int LO = 0;
            
            for (int i = 0 ; i < mono.length/2; ++i) {
                int left = (buff[i * 4 + HI] << 8) | (buff[i * 4 + LO] & 0xff);
                int right = (buff[i * 4 + 2 + HI] << 8) | (buff[i * 4 + 2 + LO] & 0xff);
                int avg = (left + right) / 2;
                mono[i * 2 + HI] = (byte)((avg >> 8) & 0xff);
                mono[i * 2 + LO] = (byte)(avg & 0xff);
            }
        } else {
            mono = buff;
        }
    }
    
    private boolean isStereo() {
        boolean stereo = false;
        
        if (channelCount == 2) {
            stereo = true;
        } else if (channelCount == 1) {
            stereo = false;
        }
        
        return stereo;
    }
    
    private void resampleByteArray(byte[] _buffer, int _inSR, int _outSR, int _channel) {
        Resampler resampler = null;
        
        if (_channel == 1) {
            ResamplerConfiguration configuration = new ResamplerConfiguration(ResamplerQuality.BEST,
            ResamplerChannel.MONO, _inSR, ResamplerChannel.MONO, _outSR);
            resampler = new Resampler(configuration);
        } else if (_channel == 2) {
            ResamplerConfiguration configuration = new ResamplerConfiguration(ResamplerQuality.BEST,
            ResamplerChannel.MONO, _inSR, ResamplerChannel.STEREO, _outSR);
            resampler = new Resampler(configuration);
        }
        
        rawPCM = resampler.resample(_buffer);
    }
    
    private int getChannelCount(String _info) {
        String result = null;
        Pattern pattern = Pattern.compile("\\bchannel-count=\\b[0-9]");
        Matcher matcher = pattern.matcher(_info);
        
        if (matcher.find()) {
            result = matcher.group();
        }
        
        return Integer.parseInt(result.replace("channel-count=", ""));
    }
    
    private int getSampleRate(String _info) {
        String result = null;
        Pattern pattern = Pattern.compile("\\bsample-rate=\\b[0-9]{5}");
        Matcher matcher = pattern.matcher(_info);
        
        if (matcher.find()) {
            result = matcher.group();
        }
        
        return Integer.parseInt(result.replace("sample-rate=", ""));
    }
}