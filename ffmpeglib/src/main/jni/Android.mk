LOCAL_PATH:= $(call my-dir)

#static version of libavcodec
include $(CLEAR_VARS)
LOCAL_MODULE:= libavcodec_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libavcodec.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libavformat
include $(CLEAR_VARS)
LOCAL_MODULE:= libavformat_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libavformat.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libavutil
include $(CLEAR_VARS)
LOCAL_MODULE:= libavutil_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libavutil.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libavfilter
include $(CLEAR_VARS)
LOCAL_MODULE:= libavfilter_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libavfilter.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libswresample
include $(CLEAR_VARS)
LOCAL_MODULE:= libswresample_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libswresample.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libswscale
include $(CLEAR_VARS)
LOCAL_MODULE:= libswscale_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libswscale.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libpostproc
include $(CLEAR_VARS)
LOCAL_MODULE:= libpostproc_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libpostproc.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libavdevice
include $(CLEAR_VARS)
LOCAL_MODULE:= libavdevice_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libavdevice.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of liblsmash
include $(CLEAR_VARS)
LOCAL_MODULE:= liblsmash_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/liblsmash.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
include $(PREBUILT_STATIC_LIBRARY)

#static version of libx264
include $(CLEAR_VARS)
LOCAL_MODULE:= libx264_static
LOCAL_SRC_FILES:= $(TARGET_ARCH_ABI)/libx264.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include
LOCAL_STATIC_LIBRARIES := liblsmash_static
include $(PREBUILT_STATIC_LIBRARY)

#libffmpeg
include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeghelper
LOCAL_C_INCLUDES += /Users/tangchangzhi/repository/ffmpeg/ffmpeg-3.0.2
LOCAL_LDLIBS := -llog -lz -ldl
LOCAL_STATIC_LIBRARIES := libavdevice_static \
    libavfilter_static \
    libswresample_static \
    libavformat_static \
    libavcodec_static \
    libpostproc_static \
    libavutil_static \
    libswscale_static \
    libx264_static \
    liblsmash_static
LOCAL_SRC_FILES := me_s1rius_ffmpeglib_FFmpegHelper.c \
    cmdutils.c \
    ffmpeg.c \
    ffmpeg_filter.c \
    ffmpeg_opt.c
include $(BUILD_SHARED_LIBRARY)