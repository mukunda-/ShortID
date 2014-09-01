/*
 * ShortID
 * Copyright (c) 2014 mukunda
 * 
 * API for managing 32-bit IDs that represent player UUIDs
 * 
 */

package com.mukunda.shortid;
 
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException; 
import java.nio.ByteBuffer; 
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files; 
import java.nio.file.Path;
import java.nio.file.StandardOpenOption; 
import java.util.ArrayList; 
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.java.JavaPlugin;

//---------------------------------------------------------------------------------------------
public class ShortID extends JavaPlugin implements Listener, ShortIDAPI {
	
	public static ShortID instance;

	private IDMap idMap;
	private IDDatabase db;
	
	public static final int INITIAL_SID = 0x100;
	
	private int nextLocalID;
	
	//---------------------------------------------------------------------------------------------
	public static ShortIDAPI getAPI() {
		return instance;
	}
	
	//---------------------------------------------------------------------------------------------
	public void onEnable() {
		saveConfig();
		
		try {
			Files.createDirectory( getDataFolder().toPath().resolve( "uuid" ) );
			Files.createDirectory( getDataFolder().toPath().resolve( "sid" ) );
		} catch ( IOException e ) {
			getLogger().severe( "Couldn't create data folders." );
			setEnabled(false);
			return;
		}
		
		idMap = new IDMap( this );

		if( getConfig().getBoolean( "MySQL.enabled", false ) ) {

			SQLInfo info = new SQLInfo();
			try {

				info.address = getConfig().getString( "MySQL.address", "" );
				if( info.address.isEmpty() ) 
					throw new IllegalArgumentException( "Missing MySQL address." );

				info.username = getConfig().getString( "MySQL.username", "" );
				if( info.username.isEmpty() ) 
					throw new IllegalArgumentException( "Missing MySQL username." );

				info.password = getConfig().getString( "MySQL.password", "" );
				if( info.password.isEmpty() ) 
					throw new IllegalArgumentException( "Missing MySQL password." );

				info.database = getConfig().getString( "MySQL.database", "" );
				if( info.database.isEmpty() ) 
					throw new IllegalArgumentException( "Missing MySQL database name." );

			} catch( IllegalArgumentException e ) {
				getLogger().severe( "Invalid SQL setup. " + e.getMessage() );
				setEnabled( false );
				return;
			}
			String table = getConfig().getString( "MySQL.table", "shortid" );

			db = new IDDatabase( this, idMap, info, table );
			
			if( !db.setup() ) {
				setEnabled( false );
				return;
			}
			
		} else {
			Path path = getDataFolder().toPath().resolve( "nextid.dat" );
			if( Files.exists(path) ) {
				String content;
				try {
					content = new String( Files.readAllBytes( path ) );
					
				} catch( IOException e ) {
					getLogger().severe( "Could not read next id. " + e.getMessage() );
					setEnabled(false);
					return;
				}
				
				try {
					if( content == "" ) throw new NumberFormatException();
					nextLocalID = Integer.parseInt( content );
					
				} catch( NumberFormatException e ) {
					getLogger().severe( "Next ID file was corrupted, scanning data files to get next available ID." );
					try {
						nextLocalID = NextIDFinder.FindNextID( this );
					} catch( IOException e2 ) {
						getLogger().severe( "Could not read ID file table. " + e2.getMessage() );
						setEnabled(false);
					}
				}
				
				getLogger().info( "Next ID available = " + nextLocalID );
				
			} else {

				nextLocalID = INITIAL_SID;
			}
		}
		
		instance = this;

	}
	
	//---------------------------------------------------------------------------------------------
	public void onDisable() {
		idMap.notifyAll();
		instance = null;
	} 
	
	//---------------------------------------------------------------------------------------------
	private SID generateID() {
		SID id = new SID(nextLocalID++);
		ArrayList<String> lines = new ArrayList<String>();
		lines.add( Integer.toString(nextLocalID) );
		
		try {
			Files.write( 
					getDataFolder().toPath().resolve("next_sid.dat"), 
					lines, 
					StandardCharsets.US_ASCII, 
					StandardOpenOption.TRUNCATE_EXISTING );
		} catch( IOException e ) {
			getLogger().severe( "Couldn't save next_sid file." );
		}
		return id;
	}
	
	//---------------------------------------------------------------------------------------------
	private void zeroFillChannel( SeekableByteChannel channel , int size) throws IOException {
		if( channel.size() < size ) {
			 ByteBuffer buffer = ByteBuffer.allocateDirect( size - (int)channel.size() );
			 
			 // fill with zero
			 channel.position( channel.size() );
			 channel.write( buffer );
		}
	}
 
	/*******************************************************************
	 * Write an entry to a map file.
	 *  MAP files contain 256 fixed size entries, either UUIDs
	 * or SIDs
	 *
	 * UUID table is indexed by uppermost octet of UUID
	 * SID table is indexed by lowermost octet of SID
	 * 
	 * @param path Path to map file, located in the "uuid" or "sid" folder.
	 * @param entrySize Size of entries in the map file, 16 for SID map or 4 for UUID map
	 * @param index index of entry to write
	 * @param buffer contents of entry to write
	 * @throws IOException
	 *******************************************************************/
	private void writeMapFile( Path path, int entrySize, 
							int index, ByteBuffer buffer ) 
									throws IOException {
		
		try( SeekableByteChannel output =
				Files.newByteChannel( 
						path, 
						StandardOpenOption.WRITE, 
						StandardOpenOption.CREATE ) ) {
			
			// zero-fill file
			zeroFillChannel( output, 256*entrySize );
			output.position( index*entrySize ); 
			output.write( buffer );
			
		} catch( IOException e ) {
			throw e;
		}
	}
	
	//---------------------------------------------------------------------------------------------
	private ByteBuffer readMapFile( Path path, int entrySize, 
							int index ) throws IOException {

		try( SeekableByteChannel input =
				Files.newByteChannel( 
						path, 
						StandardOpenOption.READ ) ) {
			  
			ByteBuffer buffer = ByteBuffer.allocateDirect(entrySize); 
			input.position( index*entrySize );
			int size = input.read( buffer );
			if( size < entrySize ) {
				getLogger().severe( "Map file is corrupt: " + path.toString() );
				return null;
			}
			
			return buffer;
			
		} catch( IOException e ) {
			throw e;
		}
	}
	
	//---------------------------------------------------------------------------------------------
	private Path getMapFilePath( UUID uuid ) {
		return getDataFolder().toPath().resolve( "uuid" )
				.resolve( "xx" + uuid.toString().substring( 2 ) + ".map" );
	}
	
	private Path getMapFilePath( SID sid ) {
		return getDataFolder().toPath().resolve( "sid" )
				.resolve( sid.toString().substring(0,6) + "xx" + ".map" );
	}
	
	//---------------------------------------------------------------------------------------------
	private void writeSIDToDisk( UUID uuid, SID sid ) {
		 
		int index = (int)(uuid.getMostSignificantBits() >>> (64-8));
		
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(4);
			buffer.putInt( sid.getInt() ); 
			writeMapFile( getMapFilePath(uuid), 4, index, buffer );
		} catch( IOException e ) {
			getLogger().severe( "Couldn't write UUID map to disk." );
		}
		  
		index = sid.getInt() & 0xFF;

		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(16);
			buffer.putLong( uuid.getLeastSignificantBits() ); 
			buffer.putLong( uuid.getMostSignificantBits() );
			writeMapFile( getMapFilePath(sid), 16, index, buffer );
		} catch( IOException e ) {
			getLogger().severe( "Couldn't write SID map to disk." );
		}
		
	}
	
	//---------------------------------------------------------------------------------------------	
	private SID readSIDFromDisk( UUID uuid ) {
		Path path = getMapFilePath( uuid );
		if( !Files.exists(path) ) return null;
		
		
		int index = (int)(uuid.getMostSignificantBits() >>> (64-8));
		
		try {
			
			ByteBuffer buffer = readMapFile( path, 4, index );
			if( buffer == null ) return null;
			int value = buffer.getInt( 0 );
			if( value == 0 ) return null;
			return new SID(value);
			
		} catch( IOException e ) {
			getLogger().severe( "Couldn't read UUID table on disk." );
		}

		return null;
	}
	
	//---------------------------------------------------------------------------------------------	
	private UUID readUUIDFromDisk( SID sid ) {
		Path path = getMapFilePath( sid );
		if( !Files.exists(path) ) return null;
		
		int index = sid.getInt() & 0xFF;
		
		try {
			
			ByteBuffer buffer = readMapFile( path, 16, index );
			if( buffer == null ) return null;
			long valueL = buffer.getLong( 0 );
			long valueH = buffer.getLong( 8 );
			if( valueL == 0 && valueH == 0 ) return null;
			return new UUID( valueH, valueL );
			
		} catch( IOException e ) {
			getLogger().severe( "Couldn't read SID table on disk." );
		}

		return null;
	}
	
	//---------------------------------------------------------------------------------------------	
	@Override
	public SID getSID( UUID uuid ) {
		SID sid = idMap.get( uuid );
		if( sid != null ) return sid;
		 
		// try to get from disk
		sid = readSIDFromDisk( uuid );
		if( sid != null )  return sid;

		if( db != null ) {
			// database mode: get from database.
			db.resolve( uuid );
			try {
				sid = idMap.getWait( uuid );
			} catch( InterruptedException e ) {
				getLogger().severe( "Unexpected exception occurred." );
				e.printStackTrace();
				return null; 
			}
			writeSIDToDisk( uuid, sid );
			
		} else {
			sid = generateID();
			writeSIDToDisk( uuid, sid );
			
		}
		
		return sid;
	}
	
	//---------------------------------------------------------------------------------------------
	@Override
	public SID getSID( OfflinePlayer player ) {
		return getSID( player.getUniqueId() );
	}
	
	//---------------------------------------------------------------------------------------------
	@Override
	public UUID getUUID( SID sid ) {
		UUID uuid = idMap.get( sid );
		if( uuid != null ) return uuid;
		
		// try to get from disk
		uuid = readUUIDFromDisk( sid );
		if( uuid != null ) return uuid;

		if( db != null ) {
			// database mode: get from database.
			db.resolve( sid );
			try {
				
				uuid = idMap.getWait( sid );
				
				if( uuid == null ) {
					// invalid SID.
					return null;
				}
				
			} catch( InterruptedException e ) {
				getLogger().severe( "Unexpected exception occurred." );
				e.printStackTrace();
				return null; 
			}
			writeSIDToDisk( uuid, sid );
			
		}
		
		return null; // unknown SID.
		
	}
	
	//---------------------------------------------------------------------------------------------
	public Player getPlayer( SID sid ) {
		return Bukkit.getPlayer( getUUID(sid) );
	}
	
	//---------------------------------------------------------------------------------------------
	public OfflinePlayer getOfflinePlayer( SID sid ) {
		return Bukkit.getOfflinePlayer( getUUID(sid) );
	}
	
	//---------------------------------------------------------------------------------------------
	@EventHandler( priority = EventPriority.MONITOR )
	public void onPlayerLogin( PlayerLoginEvent event ) {
		if( event.getResult() == Result.ALLOWED ) {
			if( db != null ) {
				db.resolve( event.getPlayer().getUniqueId() );
			}
		}
		
	}
	//---------------------------------------------------------------------------------------------
	@EventHandler( priority = EventPriority.MONITOR )
	public void onPlayerJoin( PlayerJoinEvent event ) {
		
		SID id = idMap.get( event.getPlayer().getUniqueId() );
		if( id == null ) {
			
		}
		if( db != null ) {
			db.resolve( event.getPlayer().getUniqueId() );
		}
		
	}
	
	//---------------------------------------------------------------------------------------------
	public void Crash() {
		this.setEnabled(false);
	}
	
	public HashMap<UUID,SID> buildImport() throws IOException {
		HashMap<UUID,SID> result = new HashMap<UUID,SID>();
		
		File[] files = new File( getDataFolder(), "sid" ).listFiles();
		for( File file : files ) {

			if( !file.isFile() ) continue;
			if( !file.getName().endsWith(".map") ) continue;
			
			ByteBuffer buffer = ByteBuffer.allocate(16);//[4];
			//byte[] buffer = new byte[4];
			
			String sidBase = file.getName().substring( 0, 6 );
			
			try (
				BufferedInputStream input = new BufferedInputStream( 
						Files.newInputStream( file.toPath() ) ) ) {
				
				for( int index = 0; index < 256; index++ ) {
					
					int size = input.read( buffer.array() );
					if( size != 16 ) break;
					
					String sidString = sidBase + String.format( "%02X", index );
					
					long dataL, dataH;
					dataL = buffer.getLong(0);
					dataH = buffer.getLong(1);
					if( dataL == 0 && dataH == 0 ) continue;
					
					result.put( new UUID(dataH,dataL), SID.fromString(sidString) );
					
				}
			} catch( IOException e ) {
				throw e;
			} 

		}
		return result;
	}
}
