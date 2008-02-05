#ifndef IDCT_REFERENCE_SSE_H
#define IDCT_REFERENCE_SSE_H

#ifdef __cplusplus
extern "C" {
#endif

#ifndef IDCT_REFERENCE_SSE_C
extern void __stdcall idct_reference_sse(short *block);
#endif

#ifdef __cplusplus
}
#endif
	
#endif
