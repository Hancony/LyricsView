//
// Created by yanzhennan on 2023/3/3.
//

#ifndef CODECDEMO_LOG_H
#define CODECDEMO_LOG_H

#include <android/log.h>

#define  LOG_TAG  "KaraokeViewNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)


#endif //CODECDEMO_LOG_H
