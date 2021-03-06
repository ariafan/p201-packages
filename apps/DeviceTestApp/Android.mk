ifeq ($(MTK_NFC_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)

#$(info $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/AT4/*),$(f):data/misc/nfc_conformance/DTA_Config/AT4/$(notdir $(f))))

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
#LOCAL_STATIC_JAVA_LIBRARIES := guava android-support-v4

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := DeviceTestApp
LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := libdta_mt6605_jni libdta_dynamic_load_jni
#LOCAL_JAVA_LIBRARIES += mediatek-framework

#LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/emma_filter.txt

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

endif
