#include "com_diozero_util_EpollNative.h"

#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <jni_md.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <sys/epoll.h>
#include <unistd.h>

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    epollCreate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_epollCreate(
		JNIEnv * env, jclass clazz) {
	return epoll_create(1);
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    addFile
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_addFile(
		JNIEnv * env, jclass clazz, jint epollFd, jstring filename) {
	jsize len = (*env)->GetStringLength(env, filename);
	printf("len=%d\n", len);
	char c_filename[len];
	(*env)->GetStringUTFRegion(env, filename, 0, len, c_filename);
	printf("c_filename=%s\n", c_filename);

	int fd = open(c_filename, O_RDONLY);
	if (fd < 0) {
		printf("open: file %s could not be opened, %s\n", c_filename, strerror(errno));
		return -1;
	}

	struct epoll_event ev;
	ev.events = EPOLLPRI;
	ev.data.fd = fd;

	if (epoll_ctl(epollFd, EPOLL_CTL_ADD, fd, &ev) == -1) {
		printf("epoll_ctl: error in add, %s\n", strerror(errno));
		return -1;
	}

	return fd;
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    removeFile
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_removeFile(
		JNIEnv * env, jclass clazz, jint epollFd, jint fileFd)
{
	if (close(fileFd) == -1) {
		printf("close: error closing fd %d, %s\n", fileFd, strerror(errno));
	}
	return epoll_ctl(epollFd, EPOLL_CTL_DEL, fileFd, NULL);
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    waitForEvents
 * Signature: (I)[Lcom/diozero/util/NativePollEvent;
 */
JNIEXPORT jobjectArray JNICALL Java_com_diozero_util_EpollNative_waitForEvents(
		JNIEnv * env, jclass clazz, jint epollFd) {
	char* class_name = "com/diozero/util/EpollEvent";
	jclass clz = (*env)->FindClass(env, class_name);
	if (clz == NULL) {
		// TODO Throw exception
		printf("Could not find class '%s'\n", class_name);
		return NULL;
	}
	char* signature = "(IIZ)V";
	jmethodID constructor = (*env)->GetMethodID(env, clz, "<init>", signature);
	if (constructor == NULL) {
		// TODO Throw exception
		printf("Could not find constructor with signature '%s' in class '%s'\n", signature, class_name);
		return NULL;
	}

	int max_events = 1;
	struct epoll_event epoll_events[max_events];
	int num_fds = epoll_wait(epollFd, epoll_events, max_events, -1);
	//unsigned long long epoch_time = getEpochTime();
	printf("Got %d fds\n", num_fds);
	if (num_fds < 0) {
		printf("epoll_wait failed! %s\n", strerror(errno));
		return NULL;
	}

	jobjectArray poll_events = (*env)->NewObjectArray(env, num_fds, clz, NULL);
	//char buf;
	int i;
	for (i=0; i<num_fds; i++) {
		//lseek(epoll_events[i].data.fd, 0, SEEK_SET);
		//read(epoll_events[i].data.fd, &buf, 1);
		//printf("read %c\n", buf);
		jobject aPollResult = (*env)->NewObject(env, clz, constructor, epoll_events[i].data.fd, epoll_events[i].events);
		(*env)->SetObjectArrayElement(env, poll_events, i, aPollResult);
	}

	return poll_events;
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    stopWait
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_diozero_util_EpollNative_stopWait(
		JNIEnv * env, jclass clazz, jint epollFd) {
	 int pipefds[2] = {};
	 struct epoll_event ev = {};
	 pipe(pipefds);
	 int read_pipe = pipefds[0];
	 int write_pipe = pipefds[1];

	 // make read-end non-blocking
	 int flags = fcntl(read_pipe, F_GETFL, 0);
	 fcntl(write_pipe, F_SETFL, flags|O_NONBLOCK);

	 // add the read end to the epoll
	 ev.events = EPOLLIN;
	 ev.data.fd = read_pipe;
	 epoll_ctl(epollFd, EPOLL_CTL_ADD, read_pipe, &ev);

	 char* terminate = "terminate";
	 write(write_pipe, terminate, 1);
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    shutdown
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_diozero_util_EpollNative_shutdown(
		JNIEnv * env, jclass clazz, jint epollFd) {
	close(epollFd);
}
