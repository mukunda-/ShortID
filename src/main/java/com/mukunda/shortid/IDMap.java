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

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/******************************************************************************
 * concurrent mapping between UUIDs and SIDs
 * 
 * @author mukunda
 *
 ******************************************************************************/
public final class IDMap {
	
	/**************************************************************************
	 * task to call an "on-resolved" bukkit event.
	 * 
	 * @author mukunda
	 *
	 **************************************************************************/
	private static class EventRunner extends BukkitRunnable {
		
		private final UUID uuid;
		private final SID sid;
		
		/**********************************************************************
		 * construct an event to be fired, uuid and sid must not be null
		 * 
		 * @param uuid
		 * @param sid
		 **********************************************************************/
		public EventRunner( UUID uuid, SID sid ) {
			this.uuid = uuid;
			this.sid = sid;
		}
		
		/**********************************************************************
		 * call runTask to schedule this object, and not this function.
		 **********************************************************************/
		public void run() {
			Player player = Bukkit.getPlayer( uuid );
			if( player == null ) return; // player is no longer available.
			
			Bukkit.getServer().getPluginManager().callEvent( 
					new SIDResolvedEvent( player, sid ) );
		}
	} 
	
	// owning plugin
	private final ShortID context;

	// map of resolved UUIDs to SIDs
	private final HashMap<UUID,SID> toSID;
	
	// reverse map, this should always contain a reverse entry for
	// any entries in toSID. and may also contain mappings to UUID(0,0) objects
	// for signaling that an SID was resolved 
	private final HashMap<SID,UUID> toUUID;
	
	// if this is set for a UUID when it is map()'d, then an event will be fired
	// with bukkit 
	private final HashSet<UUID> postEvent;
	  
	/**************************************************************************
	 * constructor
	 * 
	 * @param context owning plugin
	 **************************************************************************/
	public IDMap( ShortID context ) {
		this.context = context;
		toSID = new HashMap<UUID,SID>();
		toUUID = new HashMap<SID,UUID>();
		postEvent = new HashSet<UUID>();
		
	}
	
	/**************************************************************************
	 * map a new resolved entry into the table
	 *  
	 * @param uuid UUID of a player
	 * @param sid  corresponding SID
	 **************************************************************************/ 
	public synchronized boolean map( UUID uuid, SID sid ) {
		if( toSID.containsKey( uuid ) ) return false;
		toSID.put( uuid, sid );
		toUUID.put( sid, uuid );
		
		// if postEvent is set for this, fire the event task.
		if( postEvent.contains( uuid ) ) {
			postEvent.remove( uuid );
			new EventRunner( uuid, sid ).runTask( context );
		}
		
		// wake up waiting threads.
		notifyAll();
		return true;
	}
	
	/**************************************************************************
	 * convert a UUID into an SID
	 * 
	 * @param uuid  UUID to convert
	 * @return      null if the SID has not been resolved.
	 **************************************************************************/
	public synchronized SID get( UUID uuid ) {
		return toSID.get( uuid );
	}
	
	/**************************************************************************
	 * convert an SID into a UUID
	 * @param sid  SID to convert
	 * @return     null if the UUID has not been resolved.
	 **************************************************************************/
	public synchronized UUID get( SID sid ) {
		return toUUID.get( sid );
	}

	/**************************************************************************
	 * convert a UUID into an SID, and wait for it to be mapped if it isn't
	 * 
	 * warning: this will wait forever if nothing is working to resolve
	 * the value.
	 * 
	 * @param  uuid UUID to convert
	 * @return      SID result, this will never be null.
	 * 
	 * @throws InterruptedException
	 **************************************************************************/
	public synchronized SID getWait( UUID uuid ) throws InterruptedException {
		
		SID id = toSID.get( uuid );
		while( id == null ) {
			wait();
			id = toSID.get( uuid );
		} 
		
		return id;
	}
	
	/**************************************************************************
	 * convert an SID into a UUID, and wait for it to be mapped if it isn't
	 * 
	 * warning: this will wait forever if nothing is working to resolve
	 * the value.
	 * 
	 * @param sid SID to convert
	 * @return    UUID result, or null if the given SID was invalid.
	 * 
	 * @throws InterruptedException
	 **************************************************************************/
	public synchronized UUID getWait( SID sid ) throws InterruptedException {
		UUID id = toUUID.get( sid );
		while( id == null ) {
			wait();
			id = toUUID.get( sid ); 
		}
		
		// catch invalid query result:
		if( id.getMostSignificantBits() == 0 && 
				id.getLeastSignificantBits() == 0 ) return null;
		
		return id;
	}
	
	/**************************************************************************
	 * if the table has an SID for a UUID, this will fire an "on-resolved"
	 * event and return an SID.
	 * 
	 * otherwise, this sets a flag that will cause an event to be generated
	 * when this UUID is resolved, and this will return null
	 * 
	 * @param uuid  UUID to flag
	 * @return      see description
	 **************************************************************************/
	public synchronized SID postEventWhenResolved( UUID uuid ) {

		SID sid = toSID.get( uuid );
		if( uuid != null ) {
			// uuid is mapped, post event.
			new EventRunner( uuid, sid ).runTask( context );
			return sid; 
		}
		
		// post event when the database (assuming) is done resolving the SID.
		postEvent.add( uuid );
		return null;
	}
}
