#include <string.h>
#include "idct_cli.h"
#define IDCT_REFERENCE_SSE_C
#include "idct_ref.h"
#include "mmintrin.h"

/*  Perform IEEE 1180 reference (64-bit floating point, separable 8x1
 *  direct matrix multiply) Inverse Discrete Cosine Transform
*/

void __stdcall idct_reference_sse(short *block);

static const float ref_dct_matrix_t[8][8] =
{
    {/* [0][0-7] */ 0.353553,  0.490393,  0.461940,  0.415735,  0.353553,  0.277785,  0.191342,  0.097545},
    {/* [1][0-7] */ 0.353553,  0.415735,  0.191342, -0.097545, -0.353553, -0.490393, -0.461940, -0.277785},
    {/* [2][0-7] */ 0.353553,  0.277785, -0.191342, -0.490393, -0.353553,  0.097545,  0.461940,  0.415735},
    {/* [3][0-7] */ 0.353553,  0.097545, -0.461940, -0.277785,  0.353553,  0.415735, -0.191342, -0.490393},
    {/* [4][0-7] */ 0.353553, -0.097545, -0.461940,  0.277785,  0.353553, -0.415735, -0.191342,  0.490393},
    {/* [5][0-7] */ 0.353553, -0.277785, -0.191342,  0.490393, -0.353553, -0.097545,  0.461940, -0.415735},
    {/* [6][0-7] */ 0.353553, -0.415735,  0.191342,  0.097545, -0.353553,  0.490393, -0.461940,  0.277785},
    {/* [7][0-7] */ 0.353553, -0.490393,  0.461940, -0.415735,  0.353553, -0.277785,  0.191342, -0.097545}
};

void __stdcall idct_reference_sse(short *block)
{
	int i, j;
	short tmp_block[64];
	float tmp[64];
	float fblock[64];

	memcpy(tmp_block,block,sizeof(short)*64);

	__asm{
		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block];
		lea edi, [fblock];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +8];
		lea edi, [fblock +16];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;
			
		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +16];
		lea edi, [fblock +32];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +24];
		lea edi, [fblock +48];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +32];
		lea edi, [fblock +64];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;	
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +40];
		lea edi, [fblock +80];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +48];
		lea edi, [fblock +96];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +56];
		lea edi, [fblock +112];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +64];
		lea edi, [fblock +128];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +72];
		lea edi, [fblock +144];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +80];
		lea edi, [fblock +160];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +88];
		lea edi, [fblock +176];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +96];
		lea edi, [fblock +192];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +104];
		lea edi, [fblock +208];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +112];
		lea edi, [fblock +224];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;

		pxor mm1,mm1;
		pxor mm3, mm3;
		lea edx, [tmp_block +120];
		lea edi, [fblock +240];
		movd mm0, dword ptr [edx];
		movd mm2, dword ptr [edx+4];
		pcmpgtw mm1, mm0;
		pcmpgtw mm3, mm2;
		punpcklwd mm0, mm1;
		punpcklwd mm2, mm3;
		cvtpi2ps xmm1, mm0;
		cvtpi2ps xmm2, mm2;
		movlps [edi], xmm1;
		movlps [edi+8], xmm2;
	}
	
	for (i=0; i<8; i++)
	{
		for (j=0; j<8; j++)
		{
			__asm{
				//オフセットの計算と初期化。
				mov eax, dword ptr [i];
				xorps xmm7, xmm7;
				
				mov ebx, dword ptr [j];
				shl eax, 5;
				
				shl ebx, 5;
				lea edx, [fblock +eax]; 
				
				lea edi, [ref_dct_matrix_t +ebx];
				movups xmm1, [edx] ;
				
				movups xmm2, [edi] ;
				movups xmm3, [edx +16] ;

				mulps xmm1, xmm2 ;
				movups xmm4, [edi +16] ;
				;
				mulps xmm3, xmm4 ;
				;
				;
				addps xmm7, xmm1 ;
				;
				addps xmm7, xmm3 ;

				movaps xmm1, xmm7 ;
				mov eax, dword ptr [i];

				shufps xmm7, xmm1, 0x39 ;
				shl eax, 2;

				addps xmm7, xmm1 ;
				add eax, ebx;

				movaps xmm1, xmm7 ;
				lea edi, [tmp +eax];

				shufps xmm7, xmm1,0x2 ;

				addss xmm7, xmm1 ;

				movss [edi], xmm7
			}
		}
	}	
		
	for (j=0; j<8; j++)
	{
		for (i=0; i<8; i++)
		{
			__asm{
				mov eax, dword ptr [i];
				xorps xmm7, xmm7;
				
				shl eax, 5 ;
				mov ebx, dword ptr [j];

				lea edx, [tmp +eax] ;
				shl ebx, 5 ;

				movups xmm1, [edx] ;
				lea edi, [ref_dct_matrix_t +ebx] ;

				movups xmm3, [edx +16] ;
				movups xmm2, [edi] ;

				mulps xmm1, xmm2 ;
				movups xmm4, [edi +16] ;

				mulps xmm3, xmm4 ;
				addps xmm7, xmm1 ;
				
				;
				addps xmm7, xmm3 ;
				
				movaps xmm1, xmm7 ;
				
				shufps xmm7, xmm1, 0x39 ;
				
				addps xmm7, xmm1 ;
				
				movaps xmm1, xmm7 ;
				
				shufps xmm7, xmm1,0x2 ;
				
				addss xmm7, xmm1 ;

				cvtss2si eax, xmm7 ;

				lea ecx, [eax +IDCT_CLIP_TABLE_OFFSET];
				mov eax, dword ptr [j];

				lea ebx, [idct_clip_table +ecx*2];	
				mov ecx, dword ptr [i];

				lea edx, [ecx+eax*8];

				mov eax, dword ptr [block];
				mov cx, word ptr [ebx];

				mov word ptr [eax+edx*2],cx;
			}
		}
	}
	_mm_empty();
}
