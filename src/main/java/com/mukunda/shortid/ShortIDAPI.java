
/*
 * ShortID
 * Copyright (c) 2014 mukunda
 * 
 */

package com.mukunda.shortid;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * API for plugins to access ShortID queries
 * 
 * @author mukunda
 *
 */
public interface ShortIDAPI {
	
	/**********************************************************************
	 * Get the Short ID for a player.
	 * 
	 * @param player The player to get the SID for.
	 * @return
	 **********************************************************************/
	public SID getSID( OfflinePlayer player );
	
	/**********************************************************************
	 * Get the Short ID from a player's UUID.
	 * 
	 * @param id The UUID of the player to get the SID for.
	 * @return   SID of the player. This will never be null.
	 * @see      #getSID(OfflinePlayer)
	 **********************************************************************/
	public SID getSID( UUID id );
	
	/**********************************************************************
	 * Get a player's UUID from a Short ID.
	 * 
	 * @param id The SID to lookup a UUID.
	 * @return   UUID of the player. or null if the SID given was invalid.
	 * @see      #getSID(OfflinePlayer)
	 **********************************************************************/
	public UUID getUUID( SID id );

	/**********************************************************************
	 * Get a Player from an SID.
	 * 
	 * @param id The SID of a player.
	 * @return   The player associated with the SID, 
	 *           or null if the player is offline or the SID is invalid.
	 **********************************************************************/
	public Player getPlayer( SID id );
	
	/**********************************************************************
	 * Get an OfflinePlayer from an SID.
	 * 
	 * @param id The SID of a player.
	 * @return   The OfflinePlayer associated with the SID, or null if the
	 *           SID is invalid.
	 **********************************************************************/
	public OfflinePlayer getOfflinePlayer( SID id );
	
}
