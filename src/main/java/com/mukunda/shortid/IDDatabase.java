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
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException; 
import java.util.ArrayList;
import java.util.UUID;
 
import org.bukkit.scheduler.BukkitRunnable;
 
public class IDDatabase extends SQL {
	
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
	
	private class Resolver extends BukkitRunnable {
		
		private final Job job;
		
		private static final int RETRY_DELAY = 20;
		
		public Resolver( Job job ) {
			this.job = job;
		}
		
		public void run() {
			
			synchronized( jobProcessingLock ) {
				
				try {
					
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
						
					}
				} catch ( SQLTransientException e ) {
					// retry in one second
					context.getLogger().warning( "SQL query failed. retrying... reason = " + e.getMessage() );
					runTaskLaterAsynchronously( context, RETRY_DELAY );
				} catch ( SQLRecoverableException e ) {
					
					context.getLogger().warning( "SQL query failed. retrying... reason = " + e.getMessage() );
					disconnect();  
					runTaskLaterAsynchronously( context, RETRY_DELAY );
					
				} catch ( SQLException e ) {
					disconnect();
					context.getLogger().severe( "SQL encountered a non-recoverable problem: " + e.getMessage() );
					context.Crash();
					return;
				}
			}
			
			finishedJob(job);
		}
	}
 
	private final ShortID context;
	
	private final IDMap storage;
	private final ArrayList<Job> jobs;
	private final Object jobProcessingLock;
	 
	private final String table;

	private PreparedStatement insertStatement;
	private PreparedStatement selectStatement;
	private PreparedStatement rqueryStatement;
	   
	public IDDatabase( ShortID context, IDMap storage, SQLInfo info, String table ) {
		super(info);
		
		jobProcessingLock = new Object();
		this.context = context;
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
	 
	public synchronized void resolve( UUID uuid ) {
		Job job = new Job( uuid );
		if( jobs.contains( job ) ) return;
		jobs.add( job );
		
		new Resolver( job ).runTaskAsynchronously( context );  
	}
	
	public synchronized void resolve( SID sid ) {
		Job job = new Job( sid );
		if( jobs.contains( job ) ) return;
		jobs.add( new Job( sid ) ); 
		
		new Resolver( job ).runTaskAsynchronously( context );
	} 
}
