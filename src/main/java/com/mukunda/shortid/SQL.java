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
import java.sql.SQLException; 
import java.util.Properties;

import org.bukkit.Bukkit;

//-------------------------------------------------------------------------------------------------
public class SQL {
	
	private SQLInfo info;
	private Connection connection;
	Properties connectionProperties;
	
	protected final Connection getConnection() {
		return connection;
	}
	
	//-------------------------------------------------------------------------------------------------
	public SQL( SQLInfo info ) {
		this.info = info;
		
		connectionProperties = new Properties();
		connectionProperties.setProperty( "user", info.username );
		connectionProperties.setProperty( "password", info.password );
	}
	
	//-------------------------------------------------------------------------------------------------
	private String buildAddress() {
		return "jdbc:mysql://" + info.address + "/" + info.database;
	}
	
	//-------------------------------------------------------------------------------------------------
	protected final void disconnect() {
		if( connection != null ) {
			
			try {
				connection.close();
			} catch( SQLException e ) {
				Bukkit.getLogger().warning( "Strange SQL exception during disconnect." );
				e.printStackTrace();
			}
			connection = null;
		} 
	}
	
	//-------------------------------------------------------------------------------------------------
	protected final void reconnect() throws SQLException {
		disconnect();
		connect();
	}
	
	//-------------------------------------------------------------------------------------------------
	protected final void connect() throws SQLException {
		if( connection != null ) {
			if( !connection.isClosed() ) {
				return;
			}
		}
		
		connection = DriverManager.getConnection( 
				buildAddress(), 
				connectionProperties );
		
		onConnected(); 
	}
	
	//-------------------------------------------------------------------------------------------------
	public void testConnection() throws SQLException {
		connect();
	}
	
	//-------------------------------------------------------------------------------------------------
	protected void onConnected() throws SQLException {}
}
