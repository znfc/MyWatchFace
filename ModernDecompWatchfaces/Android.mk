#
# Please note this sample will not build as is, due to missing wearable-support
# library. The codes are purely for reference.
#

LOCAL_PATH:= $(call my-dir)

###############################################
include $(CLEAR_VARS)

LOCAL_MODULE := modern-decomp_default-permissions.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_RELATIVE_PATH := default-permissions
LOCAL_PROPRIETARY_MODULE := true
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_SDK_VERSION := current

include $(BUILD_PREBUILT)
###############################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME = ModernDecompWatchfaces
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_REQUIRED_MODULES := modern-decomp_default-permissions.xml
LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_DEX_PREOPT := false

include vendor/google_clockwork/libs/wearable-support.mk

include $(BUILD_PACKAGE)
###############################################
