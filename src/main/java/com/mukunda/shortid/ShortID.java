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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ShortID extends JavaPlugin implements Listener, ShortIDAPI {

	public ShortID instance;

	private IDMap idMap;
	private IDDatabase db;

	public void onEnable() {
		saveConfig();
		instance = this;

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

			db = new IDDatabase( idMap, info, table );
			
			try {
				db.testConnection();
			} catch( SQLNonTransientException e ) {
				getLogger().severe( "SQL Error. " + e.getMessage() );
				setEnabled( false );
				return;
			} catch( SQLException e ) {
				getLogger().warning( "Could not connect to database. " + e.getMessage() );
			}
			
		}

	}

	public void onDisable() {

		instance = null;
	}
	
	@Override
	public SID getSID( UUID id ) {
		if( idMap.)
	}

	@Override
	public SID getSID( OfflinePlayer player ) {
		return getSID( player.getUniqueId() );
	}

	@Override
	public UUID getUUID( SID id ) {
		
	}

	@EventHandler( priority = EventPriority.LOW )
	public void onPlayerLogin( PlayerLoginEvent event ) {


	}
}
