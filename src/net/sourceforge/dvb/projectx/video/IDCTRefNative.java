package net.sourceforge.dvb.projectx.video;

/**
 * JNI java class to access idctref.dll.
 * 
 * @author Peter Storch
 */
public class IDCTRefNative {
	
	/** indicates whether the library is loaded */
	private static boolean libraryLoaded = false;
	
	/* load the native libray */
	static
	{
		try
		{
			System.loadLibrary("idctref");
			libraryLoaded = true;
		}
		catch(Exception e)
		{
			System.out.println(e);
			//e.printStackTrace();
		}
		catch(UnsatisfiedLinkError ule)
		{
			System.out.println(ule);
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
	 * Initializes the IDCT algorithm
	 */
	public native void init();
	
	/**
	 * Performs the IDCT on an short[8][8] array.
	 * 
	 * @param block
	 * @return
	 */
	public native short[][] referenceIDCT(short[][] block);

	/**
	 * Main method for testing.
	 * 
	 * @param args
	 */
	public static void main(String args[])
	{
		IDCTRefNative idct = new IDCTRefNative();
		idct.init();
		short[][] block = new short[8][8];
		for (int i = 0; i < 8; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				block[i][j]=(short)((short)i*(short)j);
			}
		}
		System.out.println("block before");
		printArray(block);
		block = idct.referenceIDCT(block);
		System.out.println("block after");
		printArray(block);
		
	}
	
	/**
	 * Helper method to print the array.
	 * 
	 * @param block
	 */
	public static void printArray(short[][] block)
	{
		for (int i = 0; i < 8; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				System.out.print(block[i][j] + " ");
			}
			System.out.println();
		}
		
	}
}
