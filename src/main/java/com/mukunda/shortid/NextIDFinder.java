/*
 * ShortID
 *
 * Copyright (c) 2014 Mukunda Johnson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mukunda.shortid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files; 

/**
 * Wrapper class for FindNextID
 * 
 * @author mukunda
 */
public final class NextIDFinder {
	
	/**************************************************************************
	 * Scan the data directory and find out what the highest known SID is.
	 * 
	 * @param context
	 * @return
	 * @throws IOException
	 **************************************************************************/
	public static int FindNextID( ShortID context ) throws IOException {
		
		// tbh i think the new file visitor class is fucking stupid
		// using the old functions here.
		
		int nextId = ShortID.INITIAL_SID;
		
		File[] files = new File( context.getDataFolder(), "uuid" ).listFiles();
		for( File file : files ) {

			if( !file.isFile() ) continue;
			if( !file.getName().endsWith(".map") ) continue;
			
			ByteBuffer buffer = ByteBuffer.allocate(4);
			
			try (
				BufferedInputStream input = new BufferedInputStream( 
						Files.newInputStream( file.toPath() ) ) ) {
				 
				for(;;) {
					int size = input.read( buffer.array() );
					if( size != 4 ) break;
					if( buffer.getInt(0) >= nextId ) nextId = buffer.getInt(0)+1;
				}
			} catch( IOException e ) {
				throw e;
			} 

		}
		return nextId;
	}
}
