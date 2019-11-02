LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := NDK2
LOCAL_SRC_FILES := native.c 

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE	:= NDK3
LOCAL_SRC_FILES := new.c

include $(BUILD_SHARED_LIBRARY)
