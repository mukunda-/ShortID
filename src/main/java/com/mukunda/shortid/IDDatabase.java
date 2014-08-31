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
 
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayDeque; 
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;


public class IDDatabase extends SQL implements Runnable {
	
	private static class Job {
		public Object id; 
		
		public Job( UUID id ) {
			this.id = id; 
		}
		
		public Job( SID id ) {
			this.id = id; 
		}
		
		@Override
		public int hashCode() {
			return id.hashCode();
		}
		
		@Override
		public boolean equals( Object other ) {
			return (other instanceof Job) && ((Job)other).id.equals(id);
		}
	}
	
	private final IDMap storage;
	private final ArrayList<Job> jobs;
	 
	private String table;

	private PreparedStatement insertStatement;
	private PreparedStatement selectStatement;
	private PreparedStatement rqueryStatement;
	
	private boolean stopped = true;
	
	
	public IDDatabase( IDMap storage, SQLInfo info, String table ) {
		super(info);
		
		jobs = new ArrayList<Job>();
		this.storage = storage;
		this.table = table;
	}
	
	@Override
	protected void onConnected() throws SQLException {
		
		insertStatement = getConnection().prepareStatement( 
				"INSERT IGNORE INTO " + table + "(`uuid`) VALUES (?)" );
	 
		selectStatement = getConnection().prepareStatement(
				"SELECT `sid` FROM " + table + "WHERE `uuid` = ? LIMIT 1" );
		
		rqueryStatement = getConnection().prepareStatement(
				"SELECT `uuid` FROM " + table + "WHERE `sid` = ? LIMIT 1" );
	}
	 
	
	private synchronized Job waitForWork() throws InterruptedException {
		while( jobs.isEmpty() ) {
			wait();
		}
		return jobs.getFirst();
	}
	
	private synchronized void finishedJob( Job j ) {
		jobs.remove(j);
	}
	
	private byte[] mashUUID( UUID id ) {
		// turn id into 16 bytes
		byte[] bytes = new byte[16];
		long data = id.getLeastSignificantBits();
		for( int i = 0; i < 8; i++ )
			bytes[i] = (byte)(data >> (i<<3));
		data = id.getMostSignificantBits();
		for( int i = 0; i < 8; i++ )
			bytes[8+i] = (byte)(data >> (i<<3));
		return bytes;
	}

	private UUID unmashUUID( byte[] bytes ) {
		// turn 16 bytes into id
		
		long dataL = 0L, dataH = 0L;
		
		for( int i = 0; i < 8; i++ )
			dataL |= ((long)bytes[i])<<(i<<3);
		for( int i = 0; i < 8; i++ )
			dataH |= ((long)bytes[i])<<(i<<3);
		return new UUID( dataH, dataL );
	}
	
	private void executeJob( Job job ) throws SQLException {
		if( job.id instanceof UUID ) {
			
			connect();
			byte[] uuidBytes = mashUUID( (UUID)job.id );
			
			insertStatement.setBytes( 1, uuidBytes );
			insertStatement.executeUpdate();
			selectStatement.setBytes( 1, uuidBytes );
			ResultSet result = selectStatement.executeQuery();
			result.next();
			
			SID id = new SID( result.getInt(1) );
			storage.map( (UUID)job.id, id );
			
			finishedJob(job);
			
		} else if( job.id instanceof SID ) {
			
			rqueryStatement.setInt( 1, ((SID)job.id).getInt() );
			ResultSet result = rqueryStatement.executeQuery();
			
			UUID id;
			if( result.next() ) {
				id = unmashUUID( result.getBytes(1) );
			} else {
				id = new UUID(0,0);
			}
			storage.map( id, (SID)job.id );
			
			finishedJob(job);
			
		} else {
			// what do i do
		}
	}
	
	public void run() {
		
		try {
			while( !stop ) {
				Job job = waitForWork();
				
				executeJob( job );
				
			}
		} catch( InterruptedException e ) {
			
			// we were interrupted..?
			
			
		}
	}
	
	private static class Resolver extends BukkitRunnable {
		
	}
	
	public synchronized void resolve( UUID uuid ) {
		if( jobs.contains( uuid ) ) return;
		
		Bukkit.getScheduler()
		jobs.add( new Job( uuid ) );
		notifyAll();
	}
	
	public synchronized void resolve( SID sid ) {
		if( jobs.contains( sid ) ) return;
		jobs.add( new Job( sid ) );
		notifyAll();
	}
	
	public synchronized void start() {
		if( !stopped ) return;
		
		Thread thread = new Thread(this);
	}
	
	public synchronized void start() {
		
	}
}
