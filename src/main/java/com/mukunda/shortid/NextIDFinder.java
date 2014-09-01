package com.mukunda.shortid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files; 

public final class NextIDFinder {
	
	public static int FindNextID( ShortID context ) throws IOException {
		
		// tbh i think the new file visitor class is fucking stupid
		// using the old functions here.
		
		int nextId = ShortID.INITIAL_SID;
		
		File[] files = new File( context.getDataFolder(), "uuid" ).listFiles();
		for( File file : files ) {

			if( !file.isFile() ) continue;
			if( !file.getName().endsWith(".map") ) continue;
			
			ByteBuffer buffer = ByteBuffer.allocate(4);//[4];
			//byte[] buffer = new byte[4];
			
			try (
				BufferedInputStream input = new BufferedInputStream( 
						Files.newInputStream( file.toPath() ) ) ) {
				 
				for(;;) {
					int size = input.read( buffer.array() );
					if( size != 4 ) break;
					if( buffer.getInt(0) > nextId ) nextId = buffer.getInt(0);
				}
			} catch( IOException e ) {
				throw e;
			} 

		}
		return nextId;
	}
}
