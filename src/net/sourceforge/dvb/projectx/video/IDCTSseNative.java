package net.sourceforge.dvb.projectx.video;

/**
 * JNI java class to access idctsse.dll.
 * 
 * @author Peter Storch
 */
public class IDCTSseNative {
	
	/** indicates whether the library is loaded */
	private static boolean libraryLoaded = false;
	
	/* load the native libray */
	static
	{
		try
		{
			System.loadLibrary("idctsse");
			libraryLoaded = true;
		}
		catch(Exception e)
		{
			System.out.println(e);
			//e.printStackTrace();
		}
	}
	
	/**
	 * Returns true if the library is loaded.
	 * 
	 * @return
	 */
	public static boolean isLibraryLoaded()
	{
		return libraryLoaded;
	}
	
	/**
	 * Performs the IDCT on an short[64] array.
	 * 
	 * @param block
	 * @return
	 */
	public native void referenceIDCT(short[] block);

	/**
	 * Main method for testing.
	 * 
	 * @param args
	 */
	public static void main(String args[])
	{
		IDCTSseNative idct = new IDCTSseNative();

		short[] block = new short[64];

		for (int i = 0, k = 0; i < 8; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				block[k]=(short)(i*j);
				k++;
			}
		}
		System.out.println("block before");
		printArray(block);
		idct.referenceIDCT(block);
		System.out.println("block after");
		printArray(block);
	}
	
	/**
	 * Helper method to print the array.
	 * 
	 * @param block
	 */
	public static void printArray(short[] block)
	{
		for (int i = 0; i < 64; i++)
		{
			if (i % 8 == 0)
			{
				System.out.println();
			}
			System.out.print(block[i] + " ");
		}
		System.out.println();
	}
}
