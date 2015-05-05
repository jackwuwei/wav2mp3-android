#include <jni.h>
#include "debug.h"
#include "lame.h"
#include "get_audio.h"

#define BUFFER_SIZE 8192
#define EVENT_ID_START_ENCODE 0
#define EVENT_ID_ENCODING_PROGRESS 1
#define EVENT_ID_ENCODE_FINISHED 2
#define EVENT_ID_ENCODE_ABORTED 3

void frontend_debugf(const char *format, va_list ap);
void frontend_msgf(const char *format, va_list ap);
void frontend_errorf(const char *format, va_list ap);

/*
 * Class:     me_wuwei_wav2mp3_LibLAME
 * Method:    nativeLameInit
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_me_wuwei_wav2mp3_LibLAME_nativeLameInit
  (JNIEnv *env, jobject thiz) {
	return (jint)lame_init();
}

/*
 * Class:     me_wuwei_wav2mp3_LibLAME
 * Method:    nativeLameClose
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_me_wuwei_wav2mp3_LibLAME_nativeLameClose
  (JNIEnv *env, jobject thiz, jint gf) {
	if (gf != 0) {
		lame_t lame = (lame_t)gf;
		lame_close(lame);
	}
}

/*
 * Class:     me_wuwei_wav2mp3_LibLAME
 * Method:    nativeEncode
 * Signature: (ILjava/lang/String;Ljava/lang/String;ZII)V
 */
JNIEXPORT jint JNICALL Java_me_wuwei_wav2mp3_LibLAME_nativeEncode
  (JNIEnv *env, jobject thiz, jint gf, jstring inputPath, jstring outputPath, jboolean vbr, jint bitrate, jint sampleRate) {
	if (gf == 0)
		return LAME_GENERICERROR;

	lame_t lame = (lame_t) gf;
	const char *psz_inPath = (*env)->GetStringUTFChars(env, inputPath, NULL);
	const char *psz_outPath = (*env)->GetStringUTFChars(env, outputPath, NULL);

	lame_set_msgf(lame, &frontend_msgf);
	lame_set_errorf(lame, &frontend_errorf);
	lame_set_debugf(lame, &frontend_debugf);

	LOGD("Init parameters:");

	lame_set_out_samplerate(lame, sampleRate);
	LOGD("Sample rate is %d", sampleRate);
	if (vbr) {
		lame_set_VBR(lame, vbr_default);
		lame_set_VBR_mean_bitrate_kbps(lame, bitrate);
		LOGD("It is VBR, bitrate is %d", bitrate);
	} else {
		lame_set_VBR(lame, vbr_off);
		lame_set_brate(lame, bitrate);
		LOGD("It is CBR, bitrate is %d", bitrate);
	}

	int result = lame_init_params(lame);
	LOGD("Init returned: %d", result);

	FILE *input_file = NULL;
	FILE *output_file = NULL;
	input_file = fopen(psz_inPath, "rb");
	if (input_file == NULL) {
		result = FRONTEND_READERROR;
		goto failed;
	}

	output_file = fopen(psz_outPath, "wb");
	if (output_file == NULL) {
		result = FRONTEND_WRITEERROR;
		goto failed;
	}

	short input[BUFFER_SIZE*2];
	char output[BUFFER_SIZE];
	int nb_read = 0;
	int nb_write = 0;
	int nb_total = 0;
	long nb_file_size = 0;
	long nb_total_read = 0;
	int nb_percent = 0;

	fseek(input_file, 0L, SEEK_END);
	nb_file_size = ftell(input_file);
	fseek(input_file, 0L, SEEK_SET);

	jclass cls = (*env)->GetObjectClass(env, thiz);
	jmethodID mid_callback = (*env)->GetMethodID(env, cls, "Callback", "(II)V");
	jmethodID mid_is_encoding = (*env)->GetMethodID(env, cls, "isEncoding", "()Z");

	LOGD("Encoding started");
	(*env)->CallVoidMethod(env, thiz, mid_callback, EVENT_ID_START_ENCODE, nb_percent);
	do  {
		if (!(*env)->CallBooleanMethod(env, thiz, mid_is_encoding)) {
			nb_write = lame_encode_flush(lame, output, BUFFER_SIZE);
			(*env)->CallVoidMethod(env, thiz, mid_callback, EVENT_ID_ENCODE_ABORTED, nb_percent);
			break;
		}

		nb_read = fread(input, 2*sizeof(short), BUFFER_SIZE, input_file);
		nb_total_read += nb_read*2*sizeof(short);
		nb_percent = (float) nb_total_read * 100 / nb_file_size;

		if (nb_read == 0) {
			nb_write = lame_encode_flush(lame, output, BUFFER_SIZE);
			(*env)->CallVoidMethod(env, thiz, mid_callback, EVENT_ID_ENCODE_FINISHED, nb_percent);
		} else {
			nb_write = lame_encode_buffer_interleaved(lame, input, nb_read, output,
				BUFFER_SIZE);

			(*env)->CallVoidMethod(env, thiz, mid_callback, EVENT_ID_ENCODING_PROGRESS, nb_percent);
			LOGD("Encode progress %d readed %d total size %d", nb_percent, nb_total_read, nb_file_size);
		}

		fwrite(output, nb_write, 1, output_file);

		nb_total += nb_write;
	} while(nb_read != 0);

	LOGD("Encoded %d bytes", nb_total);

failed:
	if (input_file)
		fclose(input_file);

	if (output_file)
		fclose(output_file);

	(*env)->DeleteLocalRef(env, cls);
	(*env)->ReleaseStringUTFChars(env, inputPath, psz_inPath);
	(*env)->ReleaseStringUTFChars(env, outputPath, psz_outPath);

	return result;
}

void
frontend_debugf(const char *format, va_list ap)
{
    LOGD(format, ap);
}

void
frontend_msgf(const char *format, va_list ap)
{
    LOGI(format, ap);
}

void
frontend_errorf(const char *format, va_list ap)
{
    LOGE(format, ap);
}


