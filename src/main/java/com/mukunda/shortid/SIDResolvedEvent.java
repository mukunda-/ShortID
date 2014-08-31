
package com.mukunda.shortid;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SIDResolvedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final SID sid;
 
    public SIDResolvedEvent( Player player, SID id ) {
    	this.player = player;
    	this.sid = id;
    }
 
    public Player getPlayer() {
        return player;
    }
    
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
