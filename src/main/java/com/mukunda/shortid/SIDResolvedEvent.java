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

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event when an SID is resolved for a player.
 * 
 * @author mukunda
 *
 */
public class SIDResolvedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final SID sid;
 
    /*****************************************************
     * Construct a new event 
     * 
     * @param player Player associated with event
     * @param id     SID of player
     *****************************************************/
    public SIDResolvedEvent( Player player, SID id ) {
    	this.player = player;
    	this.sid = id;
    }
 
    /*****************************************************
     * Get the player who's SID was resolved.
     * 
     * @return Online player who's SID was resolved.
     *****************************************************/
    public Player getPlayer() {
        return player;
    }
    
    /*****************************************************
     * Get the SID of the player that was resolved.
     * 
     * Quick access to ShortID.getAPI().getSID( player )
     * 
     * @return SID of player.
     *****************************************************/
    public SID getSID() {
    	return sid;
    }
 
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
