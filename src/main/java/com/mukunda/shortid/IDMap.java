

package com.mukunda.shortid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * concurrent mapping between UUIDs and SIDs
 * 
 * @author mukunda
 *
 */
public final class IDMap {
	
	private static class EventRunner extends BukkitRunnable {
		
		private final UUID uuid;
		private final SID sid;
		
		public EventRunner( UUID uuid, SID sid ) {
			this.uuid = uuid;
			this.sid = sid;
		}
		
		public void run() {
			Player player = Bukkit.getPlayer( uuid );
			if( player == null ) return; // player is no longer available.
			
			Bukkit.getServer().getPluginManager().callEvent( 
					new SIDResolvedEvent( player, sid ) );
		}
	} 
	
	private final ShortID context;

	private final HashMap<UUID,SID> toSID;
	private final HashMap<SID,UUID> toUUID;
	
	private final HashSet<UUID> postEvent;
	
	public IDMap( ShortID context ) {
		this.context = context;
		toSID = new HashMap<UUID,SID>();
		toUUID = new HashMap<SID,UUID>();
		postEvent = new HashSet<UUID>();
	}
	
	public synchronized void map( UUID uuid, SID sid ) {
		toSID.put( uuid, sid );
		toUUID.put( sid, uuid );
		
		if( postEvent.contains( uuid ) ) {
			postEvent.remove( uuid );
			new EventRunner( uuid, sid ).runTask( context );
		}
		notifyAll();
	}
	
	public synchronized SID get( UUID uuid ) {
		return toSID.get( uuid );
	}
	
	public synchronized UUID get( SID sid ) {
		return toUUID.get( sid );
	}

	public synchronized SID getWait( UUID uuid ) throws InterruptedException {
		
		SID id = toSID.get( uuid );
		while( id == null ) {
			wait();
			id = toSID.get( uuid );
		} while( uuid == null );
		
		return id;
	}
	
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
	
	public synchronized SID postEventWhenResolved( UUID id ) {
		// if the table has an SID for a UUID, this returns that SID
		//
		// otherwise, it sets a flag that will cause an event to be
		// generated when this UUID is resolved, and this will return null.
		//
		SID sid = toSID.get( id );
		if( id != null ) {
			return sid; 
		}
		
		postEvent.add( id );
		return null;
	}
}
