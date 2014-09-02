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
 
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet; 
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException; 
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
 

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 
 * IDDatabase, manages resolving IDs from a MySQL database
 * 
 * @author mukunda
 *
 */
public class IDDatabase extends SQL {
	
	// delay in ticks for a database operation to retry after a non-fatal exception
	private static final int DB_RETRY_DELAY = 20;
	
	/**************************************************************************
	 * job instance, represents a UUID or SID waiting to be resolved.
	 * 
	 * @author mukunda
	 *
	 **************************************************************************/
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
	
	/**************************************************************************
	 * work module to be ran asynchronously.
	 * 
	 * @author mukunda
	 *
	 **************************************************************************/
	private class Resolver extends BukkitRunnable {
		
		private final Job job;
		
		/**********************************************************************
		 * wrap a job to be executed
		 * 
		 * @param job
		 **********************************************************************/
		public Resolver( Job job ) {
			this.job = job;
		}
		
		/**********************************************************************
		 * async execution function
		 * 
		 **********************************************************************/
		public void run() {
			
			synchronized( jobProcessingLock ) {
				
				try {
					
					if( job.id instanceof UUID ) {
						// UUID -> SID resolving job
						
						connect();
						byte[] uuidBytes = mashUUID( (UUID)job.id );
						
						// insert if not there already
						insertStatement.setBytes( 1, uuidBytes );
						insertStatement.executeUpdate();
						
						// and select the entry, this should never fail unless something
						// is seriously wrong (such as the table being maxed out)
						selectStatement.setBytes( 1, uuidBytes );
						ResultSet result = selectStatement.executeQuery();
						if( !result.next() ) throw new SQLTransientException( "Unexpected error." );
							
						SID sid = new SID( result.getInt(1) );
						
						storage.map( (UUID)job.id, sid );
						context.getFlatFiles().writeIDs( (UUID)job.id, sid, true );
						
					} else if( job.id instanceof SID ) {
						// SID -> UUID resolving job
						
						connect();
						rqueryStatement.setInt( 1, ((SID)job.id).getInt() );
						ResultSet result = rqueryStatement.executeQuery();
						
						UUID uuid;
						if( result.next() ) {
							uuid = unmashUUID( result.getBytes(1) );
							context.getFlatFiles().writeIDs( uuid, (SID)job.id, true );
						} else {
							// we need to map SOMETHING so the waiting functions
							// don't wait forever, we use UUID(0,0) to signal a
							// failed result
							uuid = new UUID(0,0);
						}
						storage.map( uuid, (SID)job.id );
						context.getFlatFiles().writeIDs( uuid, (SID)job.id, true );
						
					}
					
				} catch( SQLTransientException|SQLRecoverableException e ) {
					
					// start a new connection if it is a "recoverable" exception
					if( e instanceof SQLRecoverableException ) disconnect();
					
					// stall for a little bit and retry.
					context.getLogger().warning( ChatColor.YELLOW + "SQL query failed. retrying... reason = " + e.getMessage() );
					runTaskLaterAsynchronously( context, DB_RETRY_DELAY );
				} catch( SQLException e ) {
					
					// severe exception, program cannot continue.
					disconnect();
					context.getLogger().severe( ChatColor.RED + "SQL encountered a non-recoverable problem: " + e.getMessage() );
					e.printStackTrace();
					context.Crash();
				}
			}

			// remove the job from the queue
			finishedJob(job);
		}
	}
	
	// parent plugin
	private final ShortID context;
	
	// where resolved results are stored
	private final IDMap storage;
	
	// list of jobs, accessed concurrently
	private final ArrayList<Job> jobs;
	
	// lock for the Resolver class to synchronize usage of the database
	private final Object jobProcessingLock;
	
	// sql table name in database
	private final String table;
	
	// prepared statements to:
	//   *try* to insert a new user into the database
	//   read the sid from a uuid
	//   read a uuid from an sid (reverse-query)
	private PreparedStatement insertStatement;
	private PreparedStatement selectStatement;
	private PreparedStatement rqueryStatement;
	   
	/**************************************************************************
	 * Construct an IDDatabase instance
	 * 
	 * @param context  Plugin that owns this instance.
	 * @param storage  IDMap that will be filled with resolved IDs.
	 * @param info     Connection info and credentials.
	 * @param table    SQL table name to use when accessing the database.
	 **************************************************************************/
	public IDDatabase( ShortID context, IDMap storage, SQLInfo info, String table ) {
		super(info);
		
		jobProcessingLock = new Object();
		this.context = context;
		jobs = new ArrayList<Job>();
		this.storage = storage;
		this.table = table;
	}
	
	/**************************************************************************
	 * Called when an sql connection is established;
	 * this prepares the statements to be ran during resolves
	 * 
	 **************************************************************************/
	@Override
	protected void onConnected() throws SQLException {
		
		insertStatement = getConnection().prepareStatement( 
				"INSERT IGNORE INTO " + table + " (`uuid`) VALUES (?)" );
	 
		selectStatement = getConnection().prepareStatement(
				"SELECT `sid` FROM " + table + " WHERE `uuid` = ? LIMIT 1" );
		
		rqueryStatement = getConnection().prepareStatement(
				"SELECT `uuid` FROM " + table + " WHERE `sid` = ? LIMIT 1" );
	}
	  
	/**************************************************************************
	 * Remove a job from the queue, sending a notify signal on the jobs
	 * object if it is empty to wake up waitUntilFinished.
	 * 
	 * @param j job to be removed
	 **************************************************************************/
	private synchronized void finishedJob( Job j ) {
		jobs.remove(j);
		if( jobs.isEmpty() ) {
			synchronized (jobProcessingLock) {
				jobProcessingLock.notifyAll();
			}
		}
	}
	
	/**************************************************************************
	 * Convert a UUID into "data".
	 * 
	 * @param id  UUID to mash up
	 * @return    16-byte array with uuid contents
	 **************************************************************************/
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

	/************************************************************************** 
	 * Convert raw data into a UUID.
	 * 
	 * @param bytes 16-byte array of data
	 * @return      UUID built from data
	 **************************************************************************/
	private UUID unmashUUID( byte[] bytes ) {
		// turn 16 bytes into id
		
		long dataL = 0L, dataH = 0L;
		
		for( int i = 0; i < 8; i++ )
			dataL |= ((long)bytes[i])<<(i<<3);
		for( int i = 0; i < 8; i++ )
			dataH |= ((long)bytes[i])<<(i<<3);
		return new UUID( dataH, dataL );
	}
	 
	/**************************************************************************
	 * Post a job to resolve a UUID to an SID
	 * if the job is already pending or in progress, this does nothing.
	 * 
	 * @param uuid
	 **************************************************************************/
	public synchronized void resolve( UUID uuid ) {
		Job job = new Job( uuid );
		if( jobs.contains( job ) ) return;
		jobs.add( job );
		
		new Resolver( job ).runTaskAsynchronously( context );  
	}

	/**************************************************************************
	 * Post a job to resolve an SID to a UUID.
	 * If the job is already pending or in progress, this does nothing.
	 * 
	 * @param sid
	 **************************************************************************/
	public synchronized void resolve( SID sid ) {
		Job job = new Job( sid );
		if( jobs.contains( job ) ) return;
		jobs.add( new Job( sid ) ); 
		
		new Resolver( job ).runTaskAsynchronously( context );
	} 
	
	/**************************************************************************
	 * Wait until all pending async tasks have been completed
	 * 
	 **************************************************************************/
	public synchronized void waitUntilFinished() {
		try {
			synchronized (jobProcessingLock) {
				while( !jobs.isEmpty() ) {	
					jobProcessingLock.wait();
				}
			}
			
		} catch( InterruptedException e ) {
			context.getLogger().warning( "SQL flush was forcibly cancelled." );
		}
	}
	
	/* ******************************************************************
	 * the following functions are not thread safe and can only be called
	 * before any real work begins.
	 ********************************************************************/
	 
	
	/**************************************************************************
	 * Import data from the local flat files into the database.
	 * 
	 * This is called if the sql table has been created for the first time.
	 * 
	 * @return false if the import failed
	 **************************************************************************/
	public boolean importData() {
		context.getLogger().info( ChatColor.YELLOW + "Importing data..." );
		try {
			final HashMap<UUID,SID> data = context.getFlatFiles().buildImport();
			
			int total = data.size();
			if( total == 0 ) {
				context.getLogger().info( ChatColor.YELLOW + "Nothing to import." );
				return true;
			}
			int done = 0;
			long progressTime = System.currentTimeMillis();
			
			while( true ) {
				try {
					connect();
					
					PreparedStatement statement = getConnection().prepareStatement(
							"INSERT INTO " + table + " (`uuid`,`sid`) VALUES(?,?)" );
					
					Iterator<Map.Entry<UUID,SID>> iter = data.entrySet().iterator();
					while( iter.hasNext() ) {
						Map.Entry<UUID,SID> entry = iter.next();
						byte[] uuidBytes = mashUUID( entry.getKey() );
						
						statement.setBytes( 1, uuidBytes );
						statement.setInt( 2, entry.getValue().getInt() );
						statement.executeUpdate();
						iter.remove();
						
						done++;
						if( System.currentTimeMillis() >= progressTime + 3000 ) {
							progressTime += 3000;
							context.getLogger().info( String.format( ChatColor.YELLOW + "  %d%%...", done*100/total ) );
						}
					}

					context.getLogger().info( String.format( ChatColor.GREEN + "Import complete. %d IDs transferred.", total ) );
					statement.close();
					break;
					
				} catch( SQLTransientException|SQLRecoverableException e ) {
					// retry in one second
					if( e instanceof SQLRecoverableException ) disconnect();
					
					context.getLogger().warning( "Database fault during import: " + e.getMessage() + " -- retrying..." );
					try {
						Thread.sleep( 50*DB_RETRY_DELAY );
					} catch( InterruptedException e2 ) { return false; } // yum.
					
				} catch( SQLException e ) {
					disconnect();
					context.getLogger().severe( ChatColor.RED + "SQL encountered a non-recoverable problem." );
					context.getLogger().severe( ChatColor.RED + "IDs have **NOT** been imported!" );
					e.printStackTrace();
					return false;
				}
			}
			
		} catch (IOException e) {
			context.getLogger().severe( 
					ChatColor.RED + "IOException while trying to import data! IDs have **NOT** been imported!" );
			
			e.printStackTrace();
			
			return false;
		}
		return true;
		
	}
	
	/**************************************************************************
	 * Initialize the database
	 * 
	 * Checks if the database table has been created, and if not, it creates
	 * it and imports data.
	 * 
	 * @return false if the database could not be initialized; the program
	 *         should then terminate.
	 **************************************************************************/
	public boolean setup() {
		
		boolean importData = false;
		
		while( true ) {
			
			try {
				connect();
				DatabaseMetaData dbm = getConnection().getMetaData();
				ResultSet tables = dbm.getTables(null, null, table, null);
				if( !tables.next() ) {
					Statement statement = getConnection().createStatement();
					statement.executeUpdate( "CREATE TABLE "+table+" (" +
							"sid INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY," +
							"uuid BINARY(16) NOT NULL UNIQUE" +
							" ) " +
							"AUTO_INCREMENT = " + String.format( "%d", ShortID.INITIAL_SID ) );
					
					context.getLogger().info( ChatColor.YELLOW + "Created SQL table." );
					importData = true;
					break;
				}
				
				break;
				
			} catch ( SQLTransientException|SQLRecoverableException e ) {
				// retry in one second
				if( e instanceof SQLRecoverableException ) disconnect();
				context.getLogger().warning( "Database setup failure: " + e.getMessage() + " -- retrying..." );
				try {
					Thread.sleep( 50*DB_RETRY_DELAY );
				} catch( InterruptedException e2 ) {
					return false;
				}
				
			} catch( SQLException e ) {
				disconnect();
				context.getLogger().severe( ChatColor.RED  + "SQL encountered a non-recoverable problem: " + e.getMessage() );
				e.printStackTrace();
				return false;
			}
		}
		
		if( importData ) {
			return importData();
		}
		
		return true;
	}
}
