
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
