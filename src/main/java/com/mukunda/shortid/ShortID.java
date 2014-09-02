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
  
import java.io.IOException;  
import java.nio.charset.StandardCharsets;
import java.nio.file.Files; 
import java.nio.file.Path;
import java.nio.file.StandardOpenOption; 
import java.util.ArrayList;  
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
public final class ShortID extends JavaPlugin implements Listener, ShortIDAPI {
	
	public static ShortID instance;

	private FlatFiles flatfiles;
	private IDMap idMap;
	private IDDatabase db;
	
	public static final int INITIAL_SID = 0x100;
	
	private int nextLocalID;
	
	//---------------------------------------------------------------------------------------------
	public static ShortIDAPI getAPI() {
		return instance;
	}
	
	public FlatFiles getFlatFiles() {
		return flatfiles;
	}
	
	//---------------------------------------------------------------------------------------------
	public void onEnable() {
		saveDefaultConfig();
		
		try {
			
			Files.createDirectories( getDataFolder().toPath().resolve( "uuid" ) );
			Files.createDirectories( getDataFolder().toPath().resolve( "sid" ) );
		} catch ( IOException e ) {
			getLogger().severe( "Couldn't create data folders." );
			setEnabled(false);
			return;
		}
		
		idMap = new IDMap( this );
		flatfiles = new FlatFiles( this );

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
			Path path = getDataFolder().toPath().resolve( "next_sid.dat" );
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
					nextLocalID = Integer.parseInt( content.trim() );
					
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
		
		getServer().getPluginManager().registerEvents( this, this );
		
		instance = this;

	}
	
	//---------------------------------------------------------------------------------------------
	@Override
	public void onDisable() {
		if( db != null ) {
			db.waitUntilFinished();
		}
		instance = null;
	} 
	
	/**************************************************************************
	 * Get the next SID, and increment the counter
	 * 
	 * The counter is also saved to disk.
	 * 
	 * This function is not, and should not, be used when using a database.
	 * 
	 * @return New Unique SID
	 **************************************************************************/
	private SID generateID() {
		SID id = new SID(nextLocalID++);
		ArrayList<String> lines = new ArrayList<String>();
		lines.add( Integer.toString(nextLocalID) );
		
		try {
			Files.write( 
					getDataFolder().toPath().resolve("next_sid.dat"), 
					lines, 
					StandardCharsets.US_ASCII, 
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE );
		} catch( IOException e ) {
			getLogger().severe( "Couldn't save next_sid file. "+ e.getMessage() );
			e.printStackTrace();
		}
		return id;
	}
	
	/**************************************************************************
	 * {@inheritDoc}
	 **************************************************************************/
	@Override
	public SID getSID( UUID uuid ) {
		SID sid = idMap.get( uuid );
		if( sid != null ) return sid;
		 
		// try to get from disk
		sid = flatfiles.readSID( uuid );
		if( sid != null ) {
			idMap.map( uuid, sid );
			return sid;
		}

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
			//writeIDToDisk( uuid, sid );
			
		} else {
			sid = generateID();
			
			getLogger().info( "Generated new ID: " + uuid + " -> " + sid );
			idMap.map( uuid, sid );
			flatfiles.writeIDs( uuid, sid, false ); 
			
		}
		
		return sid;
	}
	
	/**************************************************************************
	 * {@inheritDoc}
	 **************************************************************************/
	@Override
	public SID getSID( OfflinePlayer player ) {
		return getSID( player.getUniqueId() );
	}
	
	/**************************************************************************
	 * {@inheritDoc}
	 **************************************************************************/
	@Override
	public UUID getUUID( SID sid ) {
		UUID uuid = idMap.get( sid );
		if( uuid != null ) return uuid;
		
		// try to get from disk
		uuid = flatfiles.readUUID( sid );
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
			flatfiles.writeIDs( uuid, sid, true );
			
		}
		
		return null; // unknown SID.
		
	}
	
	/**************************************************************************
	 * {@inheritDoc}
	 **************************************************************************/
	@Override
	public Player getPlayer( SID sid ) {
		return Bukkit.getPlayer( getUUID(sid) );
	}
	
	//---------------------------------------------------------------------------------------------
	@Override
	public OfflinePlayer getOfflinePlayer( SID sid ) {
		return Bukkit.getOfflinePlayer( getUUID(sid) );
	}
	
	//---------------------------------------------------------------------------------------------
	@EventHandler( priority = EventPriority.MONITOR )
	public void onPlayerLogin( PlayerLoginEvent event ) {
		if( event.getResult() == Result.ALLOWED ) {
			
			UUID uuid = event.getPlayer().getUniqueId();
			SID sid = idMap.get( uuid ); 
			if( sid == null ) {
				if( flatfiles.readSID( uuid ) == null ) {
					if( db != null ) {
						
						// start resolving early so when they try to read the value
						// later on there's less chance of a stall.
						
						db.resolve( event.getPlayer().getUniqueId() );
					} else {
						// non database mode, the id is generated later.
						
					}
				}
			}
			
		}
		
	}
	//---------------------------------------------------------------------------------------------
	@EventHandler( priority = EventPriority.MONITOR )
	public void onPlayerJoin( PlayerJoinEvent event ) {
		
		UUID uuid = event.getPlayer().getUniqueId();
		SID sid = idMap.get( uuid );
		if( sid == null ) {
			if( db != null ) {
				// it is already resolving, but just make sure...?
				db.resolve( uuid );
				
				idMap.postEventWhenResolved( uuid );
			} else {
				sid = getSID( uuid );
				Bukkit.getServer().getPluginManager().callEvent( 
						new SIDResolvedEvent( event.getPlayer(), sid ) );

			}
		} else {
			Bukkit.getServer().getPluginManager().callEvent( 
					new SIDResolvedEvent( event.getPlayer(), sid ) );
		}
	}
	
	/**************************************************************************
	 * shut down this plugin due to a critical error.
	 * 
	 **************************************************************************/
	public void Crash() {
		this.setEnabled(false);
	}
	
	
}
