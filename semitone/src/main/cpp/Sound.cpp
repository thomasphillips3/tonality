/*
 * Semitone - tuner, metronome, and piano for Android
 * Copyright (C) 2019  Andy Tockman <andy@tck.mn>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "Sound.h"

#include <oboe/Oboe.h>

extern "C" {
#include <libswresample/swresample.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/opt.h>
}

#define MP3_BLOCKSIZE 1152

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "semitone", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "semitone", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "semitone", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "semitone", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "semitone", __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,   "semitone", __VA_ARGS__)

int read(void *ptr, uint8_t *buf, int bufsize) {
    return AAsset_read((AAsset*)ptr, buf, (size_t)bufsize);
}

int64_t seek(void *ptr, int64_t offset, int whence) {
    // See https://www.ffmpeg.org/doxygen/3.0/avio_8h.html#a427ff2a881637b47ee7d7f9e368be63f
    if (whence == AVSEEK_SIZE) return AAsset_getLength((AAsset*)ptr);
    if (AAsset_seek((AAsset*)ptr, offset, whence) == -1) {
        return -1;
    } else {
        return 0;
    }
}

// TODO check for errors here
Sound::Sound(AAssetManager &am, const char *path, int concert_a, int channels) {
    AAsset *a = AAssetManager_open(&am, path, AASSET_MODE_UNKNOWN);
    if (a == nullptr) {
        LOGE("Failed to open asset: %s", path);
        nSamples = 0;
        return;
    }

    // we're guessing it won't be compressed more than 12x
    const long sizeGuess = 12 * AAsset_getLength(a) * sizeof(float);
    uint8_t *decoded = nullptr;
    uint8_t *buf = nullptr;
    
    try {
        decoded = new uint8_t[sizeGuess];
        buf = reinterpret_cast<uint8_t*>(av_malloc(MP3_BLOCKSIZE));
        if (!buf) {
            throw std::runtime_error("Failed to allocate buffer");
        }

        // obtain AVIOContext (with deleter)
        std::unique_ptr<AVIOContext, void(*)(AVIOContext*)> ioc {
            nullptr, [](AVIOContext *c) { if (c) { av_free(c->buffer); avio_context_free(&c); } }
        };
        AVIOContext *iocTmp = avio_alloc_context(buf, MP3_BLOCKSIZE, 0, a, read, nullptr, seek);
        if (!iocTmp) {
            throw std::runtime_error("Failed to allocate AVIOContext");
        }
        ioc.reset(iocTmp);

        // obtain AVFormatContext (with deleter)
        std::unique_ptr<AVFormatContext, decltype(&avformat_free_context)> fc {
            nullptr, &avformat_free_context
        };
        AVFormatContext *fcTmp = avformat_alloc_context();
        if (!fcTmp) {
            throw std::runtime_error("Failed to allocate AVFormatContext");
        }
        fcTmp->pb = ioc.get();
        fc.reset(fcTmp);

        // initialize AVFormatContext
        AVFormatContext *fcptr = fc.get();
        int ret = avformat_open_input(&fcptr, "", nullptr, nullptr);
        if (ret < 0) {
            throw std::runtime_error("Failed to open input");
        }
        ret = avformat_find_stream_info(fcptr, nullptr);
        if (ret < 0) {
            throw std::runtime_error("Failed to find stream info");
        }

        // find stream and codec
        int stream_idx = av_find_best_stream(fc.get(), AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0);
        if (stream_idx < 0) {
            throw std::runtime_error("Failed to find audio stream");
        }
        AVStream *stream = fc->streams[stream_idx];
        const AVCodec *codec = avcodec_find_decoder(stream->codecpar->codec_id);
        if (!codec) {
            throw std::runtime_error("Failed to find decoder");
        }

        // obtain AVCodecContext (with deleter)
        std::unique_ptr<AVCodecContext, void(*)(AVCodecContext*)> cc {
            avcodec_alloc_context3(codec), [](AVCodecContext *c) { avcodec_free_context(&c); }
        };
        if (!cc) {
            throw std::runtime_error("Failed to allocate codec context");
        }

        // initialize AVCodecContext
        ret = avcodec_parameters_to_context(cc.get(), stream->codecpar);
        if (ret < 0) {
            throw std::runtime_error("Failed to copy codec params");
        }

        // Initialize channel layout if not set
        if (cc->ch_layout.nb_channels == 0) {
            av_channel_layout_default(&cc->ch_layout, stream->codecpar->ch_layout.nb_channels);
        }

        ret = avcodec_open2(cc.get(), codec, nullptr);
        if (ret < 0) {
            throw std::runtime_error("Failed to open codec");
        }

        // initialize software resampler
        std::unique_ptr<SwrContext, void(*)(SwrContext*)> swr {
            swr_alloc(), [](SwrContext *s) { swr_free(&s); }
        };
        if (!swr) {
            throw std::runtime_error("Failed to allocate resampler");
        }

        // Set up input parameters
        ret = av_opt_set_chlayout(swr.get(), "in_chlayout", &cc->ch_layout, 0);
        if (ret < 0) {
            throw std::runtime_error("Failed to set input channel layout");
        }

        ret = av_opt_set_int(swr.get(), "in_sample_rate", (concert_a/440.0)*cc->sample_rate, 0);
        if (ret < 0) {
            throw std::runtime_error("Failed to set input sample rate");
        }

        ret = av_opt_set_sample_fmt(swr.get(), "in_sample_fmt", cc->sample_fmt, 0);
        if (ret < 0) {
            throw std::runtime_error("Failed to set input sample format");
        }

        // Set up output parameters
        AVChannelLayout out_ch_layout;
        if (channels == 1) {
            av_channel_layout_from_mask(&out_ch_layout, AV_CH_LAYOUT_MONO);
        } else if (channels == 2) {
            av_channel_layout_from_mask(&out_ch_layout, AV_CH_LAYOUT_STEREO);
        } else {
            av_channel_layout_default(&out_ch_layout, channels);
        }

        ret = av_opt_set_chlayout(swr.get(), "out_chlayout", &out_ch_layout, 0);
        if (ret < 0) {
            throw std::runtime_error("Failed to set output channel layout");
        }

        ret = av_opt_set_int(swr.get(), "out_sample_rate", oboe::DefaultStreamValues::SampleRate, 0);
        if (ret < 0) {
            throw std::runtime_error("Failed to set output sample rate");
        }

        ret = av_opt_set_sample_fmt(swr.get(), "out_sample_fmt", AV_SAMPLE_FMT_FLT, 0);
        if (ret < 0) {
            throw std::runtime_error("Failed to set output sample format");
        }

        ret = swr_init(swr.get());
        if (ret < 0) {
            throw std::runtime_error("Failed to initialize resampler");
        }

        // do the actual decoding
        size_t nBytes = 0;
        std::unique_ptr<AVPacket, void(*)(AVPacket*)> packet {
            av_packet_alloc(), [](AVPacket *p) { av_packet_free(&p); }
        };
        std::unique_ptr<AVFrame, void(*)(AVFrame*)> frame {
            av_frame_alloc(), [](AVFrame *f) { av_frame_free(&f); }
        };
        
        if (!packet || !frame) {
            throw std::runtime_error("Failed to allocate packet/frame");
        }

        while (av_read_frame(fc.get(), packet.get()) == 0) {
            if (packet->stream_index != stream_idx) continue;
            
            ret = avcodec_send_packet(cc.get(), packet.get());
            if (ret < 0) {
                throw std::runtime_error("Failed to send packet");
            }

            while (ret >= 0) {
                ret = avcodec_receive_frame(cc.get(), frame.get());
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                    break;
                } else if (ret < 0) {
                    throw std::runtime_error("Failed to receive frame");
                }

                // resample
                int32_t samples = (int32_t) av_rescale_rnd(
                        swr_get_delay(swr.get(), frame->sample_rate) + frame->nb_samples,
                        oboe::DefaultStreamValues::SampleRate,
                        frame->sample_rate,
                        AV_ROUND_UP);
                uint8_t *swrbuf = nullptr;
                ret = av_samples_alloc(&swrbuf, nullptr, channels, samples, AV_SAMPLE_FMT_FLT, 0);
                if (ret < 0) {
                    throw std::runtime_error("Failed to allocate resample buffer");
                }

                int frame_count = swr_convert(swr.get(), &swrbuf, samples, 
                    (const uint8_t **) frame->data, frame->nb_samples);
                if (frame_count < 0) {
                    av_freep(&swrbuf);
                    throw std::runtime_error("Failed to convert samples");
                }

                size_t bytesize = frame_count * sizeof(float) * channels;
                if (nBytes + bytesize > sizeGuess) {
                    av_freep(&swrbuf);
                    throw std::runtime_error("Buffer overflow");
                }

                memcpy(decoded + nBytes, swrbuf, bytesize);
                nBytes += bytesize;
                av_freep(&swrbuf);
            }
        }

        nSamples = nBytes / sizeof(float);
        data = std::make_unique<float[]>(nSamples);
        memcpy(data.get(), decoded, nBytes);
        delete[] decoded;
        AAsset_close(a);
        offset = 0;

    } catch (const std::exception& e) {
        LOGE("Error in Sound constructor: %s", e.what());
        delete[] decoded;
        if (buf) av_free(buf);
        AAsset_close(a);
        nSamples = 0;
        data.reset();
    }
}
