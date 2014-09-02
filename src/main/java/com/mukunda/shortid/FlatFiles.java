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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.plugin.Plugin;

/******************************************************************************
 * Manager for accessing and writing entries to/from the data files.
 * 
 * @author mukunda
 *
 ******************************************************************************/
public class FlatFiles {
	
	private Plugin context;
	
	/**************************************************************************
	 * Constructor
	 * 
	 * @param context Owning plugin.
	 **************************************************************************/
	public FlatFiles( Plugin context ) {
		this.context = context;
	}
	
	/**************************************************************************
	 * Pad a file with zeros up to a specified size
	 * 
	 * @param channel Open byte channel
	 * @param size    Size to pad file to
	 * @throws IOException
	 **************************************************************************/
	private void zeroFillChannel( SeekableByteChannel channel , int size) throws IOException {
		if( channel.size() < size ) {
			 ByteBuffer buffer = ByteBuffer.allocateDirect( size - (int)channel.size() );
			 
			 // fill with zero
			 channel.position( channel.size() );
			 channel.write( buffer );
		}
	}
	 
	/**************************************************************************
	 * Write an entry to an SID map file.
	 * 
	 * If the file doesn't exist yet, it will be created and 
	 * initialized (zero-filled).
	 * 
	 * @param sid  Used to locate an index the SID file.
	 * @param uuid The UUID to write to the table
	 * @throws IOException
	 **************************************************************************/
	private void writeSIDFile( SID sid, UUID uuid ) 
									throws IOException {
		Path path = getSIDFilePath(sid);
		int index = sid.getInt() & 0xFFF;
		
		ByteBuffer buffer = ByteBuffer.allocateDirect( 16 );
		buffer.putLong( 0, uuid.getLeastSignificantBits() );
		buffer.putLong( 8, uuid.getMostSignificantBits() );
		
		try( SeekableByteChannel output =
				Files.newByteChannel( 
						path, 
						StandardOpenOption.WRITE, 
						StandardOpenOption.CREATE ) ) {
			
			// zero-fill file
			zeroFillChannel( output, 4096*16 );
			output.position( index*16 ); 
			output.write( buffer );
			
		} catch( IOException e ) {
			throw e;
		}
	}
	
	/**************************************************************************
	 * Read an SID from a UUID map file.
	 * 
	 * UUID map files are filled with data pairs containing UUIDs and
	 * corresponding SID.
	 * 
	 * @param id UUID to search for
	 * @return   SID found linked with UUID match, or null if no matches.
	 * @throws IOException 
	 **************************************************************************/
	private SID readUUIDFile( UUID id ) throws IOException {
		final Path path = getUUIDFilePath(id);
		if( !Files.exists(path) ) return null;
		
		long testH, testL;
		testH = id.getMostSignificantBits();
		testL = id.getLeastSignificantBits();
		
		// uuid files are a list of UUID,SID pairs
		
		ByteBuffer buffer = ByteBuffer.allocate(20);
		try( @SuppressWarnings("resource") // suppressing eclipse bug.
			BufferedInputStream input = 
				new BufferedInputStream( 
						Files.newInputStream( 
						path, 
						StandardOpenOption.READ ) ) ) {
			
			while( input.read( buffer.array() ) == 20 ) {
				
				
				if( testH != buffer.getLong(8) ) continue;
				if( testL != buffer.getLong(0) ) continue;

				return new SID(buffer.getInt(16));
			}
			
		} catch( IOException e ) {
			throw e;
		}
		return null;
	}

	/**************************************************************************
	 * Read an SID map file entry.
	 * 
	 * An SID map contains 4096 UUID entries, and is indexed by the lower
	 * 12 bits of an SID.
	 * 
	 * @param sid
	 * @return UUID from the map file, or null if the entry was not set.
	 * @throws IOException
	 **************************************************************************/
	private UUID readSIDFile( SID sid ) throws IOException {
		final Path path = getSIDFilePath( sid );
		try( SeekableByteChannel input =
				Files.newByteChannel( 
						path, 
						StandardOpenOption.READ ) ) {
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(16); 
			input.position( (sid.getInt()&0xFFF) * 16 );
			int size = input.read( buffer );
			if( size < 16 ) {
				context.getLogger().severe( "Map file is corrupt: " + path.toString() );
				return null;
			}
			
			long a = buffer.getLong(8);
			long b = buffer.getLong(0);
			if( a == 0L && b == 0L ) return null; // zero entry, no uuid stored here.
			
			return new UUID( a, b );
			
		} catch( IOException e ) {
			throw e;
		}
	}

	/**************************************************************************
	 * Get the path for a UUID map file.
	 * 
	 * The UUID map filename is the first 3 digits of a UUID string.
	 * 
	 * @param uuid
	 * @return Path to the map file that may contain the given UUID. 
	 **************************************************************************/
	private Path getUUIDFilePath( UUID uuid ) {
		int a = (int)(uuid.getMostSignificantBits() >>> (64-12));
		return context.getDataFolder().toPath().resolve( "uuid" )
				.resolve( String.format( "%03X", a ) + ".uuid" );
	}

	/**************************************************************************
	 * Get the path for an SID map file.
	 * 
	 * Converts an SID into a string and replaces the last 3 digits with "xxx"
	 * 
	 * @param sid
	 * @return Path to the file that holds the mapping for the SID given.
	 **************************************************************************/
	private Path getSIDFilePath( SID sid ) {
		return context.getDataFolder().toPath().resolve( "sid" )
				.resolve( sid.toString().substring(0,5) + "xxx" + ".sid" );
	}

	/**************************************************************************
	 * Save a UUID to a UUID map file.
	 * 
	 * The file will be created if it doesn't exist.
	 * 
	 * @param id             ID to save
	 * @param sid            corresponding SID
	 * @param checkExisting  true to check if the file already has the entry 
	 *                       before attempting to add a new one.
	 * @throws IOException   
	 **************************************************************************/
	private void writeUUIDFile( UUID id, SID sid, boolean checkExisting ) throws IOException {
		Path path = getUUIDFilePath( id );
		
		if( checkExisting ) {
			if( readUUIDFile( id ) != null ) return;
		}
		ByteBuffer buffer = ByteBuffer.allocate(20);
		buffer.putLong( 0, id.getLeastSignificantBits() );
		buffer.putLong( 8, id.getMostSignificantBits() );
		buffer.putInt( 16, sid.getInt() );
		
		try( OutputStream output =  
						Files.newOutputStream( 
						path, 
						StandardOpenOption.CREATE,
						StandardOpenOption.WRITE,
						StandardOpenOption.APPEND ) ) {

			output.write( buffer.array() );
			
		} catch( IOException e ) {
			throw e;
		}
	}
	

	/**************************************************************************
	 * Save a pair of IDs to the flat files.
	 * 
	 * This function is thread-safe.
	 * 
	 * @param uuid          UUID to save
	 * @param sid           SID associated with UUID
	 * @param checkExisting If this is true, the UUID file will be checked for
	 *                      an existing entry before attempting to add a new
	 *                      one. If false, the UUID list entry will be added
	 *                      without checking. The SID map isn't a list, so
	 *                      rewriting the value has no effect.
	 **************************************************************************/
	public  synchronized  void writeIDs( UUID uuid, SID sid, boolean checkExisting ) {
		
		try {
			writeUUIDFile( uuid, sid, checkExisting );
		} catch( IOException e ) {
			context.getLogger().severe( "Couldn't write UUID file to disk." );
			e.printStackTrace();
		}
		
		try {
			writeSIDFile( sid, uuid );
		} catch( IOException e ) {
			context.getLogger().severe( "Couldn't write SID map to disk." );
			e.printStackTrace();
		}
		
	}

	/**************************************************************************
	 * Try to read an SID entry from the flat files. (UUID -> SID)
	 * 
	 * This function is thread-safe.
	 * 
	 * @param uuid   UUID to query
	 * @return       null if the entry doesn't exist
	 **************************************************************************/
	public  synchronized  SID readSID( UUID uuid ) {
		 
		try {
			return readUUIDFile( uuid );
			
		} catch( IOException e ) {
			context.getLogger().severe( "Couldn't read UUID table on disk." );
			e.printStackTrace();
		}

		return null;
	}
	
	/**************************************************************************
	 * Try to read a UUID entry from the flat files. (SID -> UUID)
	 * 
	 * This function is thread-safe.
	 * 
	 * @param sid SID to query
	 * @return    null if the entry doesn't exist
	 **************************************************************************/
	public  synchronized  UUID readUUID( SID sid ) {
		try {
			return readSIDFile( sid ); 
			
		} catch( IOException e ) {
			context.getLogger().severe( "Couldn't read SID table on disk." );
			e.printStackTrace();
		}

		return null;
	}
	
	/**************************************************************************
	 * Scan the flatfiles folder and build a map of all known 
	 * UUID -> SID mappings. (for importing to a database)
	 * 
	 * @return UUID->SID map
	 * @throws IOException
	 **************************************************************************/
	public HashMap<UUID,SID> buildImport() throws IOException {
		HashMap<UUID,SID> result = new HashMap<UUID,SID>();

		File[] files = new File( context.getDataFolder(), "uuid" ).listFiles();
		for( File file : files ) {

			if( !file.isFile() ) continue;
			if( !file.getName().endsWith(".uuid") ) continue;

			ByteBuffer buffer = ByteBuffer.allocate(20);

			try (
					BufferedInputStream input = new BufferedInputStream( 
							Files.newInputStream( file.toPath() ) ) ) {

				while( input.read( buffer.array() ) == 20 ) {

					long dataL, dataH;
					dataL = buffer.getLong(0);
					dataH = buffer.getLong(8);
					SID sid = new SID( buffer.getInt(16) );

					if( dataL == 0 && dataH == 0 ) continue;

					result.put( new UUID(dataH,dataL), sid ); 
				}
			} catch( IOException e ) {
				throw e;
			} 
		}
		return result;
	}
	
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
			if( !file.getName().endsWith(".uuid") ) continue;
			
			ByteBuffer buffer = ByteBuffer.allocate(20);
			
			try (
				BufferedInputStream input = new BufferedInputStream( 
						Files.newInputStream( file.toPath() ) ) ) {
				 
				while( input.read( buffer.array() ) == 20 ) {
					if( buffer.getInt(16) >= nextId ) 
						nextId = buffer.getInt(16) + 1;
				}
			} catch( IOException e ) {
				throw e;
			} 

		}
		return nextId;
	}
}
