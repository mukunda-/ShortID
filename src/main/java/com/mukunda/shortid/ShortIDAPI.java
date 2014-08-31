/*
 * ShortID
 * Copyright (c) 2014 mukunda
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
	// TODO getPlayer, getOfflinePlayer
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
