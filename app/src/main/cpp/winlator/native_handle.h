#ifndef WINLATOR_NATIVE_HANDLE_H
#define WINLATOR_NATIVE_HANDLE_H

/* Minimal copy of Android native_handle_t for NDK builds (not in public NDK). */

#ifdef __cplusplus
extern "C" {
#endif

typedef struct native_handle {
    int version;
    int numFds;
    int numInts;
    int data[0];
} native_handle_t;

#ifdef __cplusplus
}
#endif

#endif
