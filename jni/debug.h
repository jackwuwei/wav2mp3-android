/*
 * debug.h
 *
 *  Created on: 2013-8-13
 *      Author: wuwei
 */

#ifndef DEBUG_H_
#define DEBUG_H_

#include <android/log.h>

#define LOG_TAG "LAME-JNI"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


#endif /* DEBUG_H_ */
