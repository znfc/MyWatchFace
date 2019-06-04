LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := ClockworkSettingsRoboTests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_RESOURCE_DIRS := config

# Include test libraries.
LOCAL_STATIC_JAVA_LIBRARIES := \
    clockwork-system-robolectric

LOCAL_JAVA_LIBRARIES := \
    com.google.android.wearable \
    wear-service \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_INSTRUMENTATION_FOR = ClockworkSettings

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

###################################################

include $(CLEAR_VARS)

LOCAL_MODULE := clockwork-settings-robotests

LOCAL_STATIC_JAVA_LIBRARIES := \
    ClockworkSettingsRoboTests \
    prebuilt-com.google.android.wearable \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_TEST_PACKAGE := ClockworkSettings

include external/robolectric-shadows/run_robotests.mk
