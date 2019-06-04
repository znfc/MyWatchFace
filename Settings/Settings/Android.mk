LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    $(call all-Iaidl-files-under, src) \
    src/com/google/android/clockwork/settings/EventLogTags.logtags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ClockworkSettings
LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_JAVA_LIBRARIES := \
    telephony-common \
    ims-common \

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-constraint-layout \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v13 \
    android-support-wear \


LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-constraint-layout-solver \
    clockwork-adboverbluetooth \
    clockwork-battery \
    clockwork-battery-wear \
    clockwork-common \
    clockwork-common-logging \
    clockwork-common-setup-wearable \
    clockwork-gkeys \
    clockwork-keyguard-lib \
    clockwork-phone-lib \
    clockwork-power-lib \
    clockwork-services \
    clockwork-settings \
    clockwork-storage \
    clockwork-systemui \
    clockwork-companion-relay-lib \
    clockwork-wifi \
    gsf-client \
    guava \
    libprotobuf-java-nano \
    jsr305 \
    volley \
    wear-service \

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# For GmsCore.
include vendor/unbundled_google/packages/ClockworkPrebuilts/libs/GmsCore/sdk/gms_1p_sdk.mk

LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

include vendor/google_clockwork/libs/wearable-support.mk

include vendor/google_clockwork/libs/clockwork-views.mk

# For SettingsLib
include frameworks/base/packages/SettingsLib/common.mk

include vendor/google_clockwork/libs/wearable-datetimepicker.mk

include vendor/google_clockwork/libs/clockwork-telephony.mk

# TODO: b/35788202
ifndef LOCAL_JACK_ENABLED
  LOCAL_PROGUARD_ENABLED := disabled
  LOCAL_DX_FLAGS := --multi-dex
endif

# b/73904640
LOCAL_ERROR_PRONE_FLAGS := -Xep:CheckReturnValue:WARN

# Perform speed pre-opt using profile (com.google.android.apps.wearable.settings)
LOCAL_DEX_PREOPT_GENERATE_PROFILE := true
LOCAL_DEX_PREOPT_PROFILE_CLASS_LISTING := vendor/google_clockwork/packages/Settings/primary.prof

include $(BUILD_PACKAGE)

#######################################################

include $(call all-makefiles-under,$(LOCAL_PATH))
