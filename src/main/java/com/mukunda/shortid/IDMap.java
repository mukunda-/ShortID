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

import java.util.HashMap;
import java.util.UUID;

/**
 * concurrent mapping between UUIDs and SIDs
 * 
 * @author mukunda
 *
 */
public final class IDMap {

	private final HashMap<UUID,SID> toSID;
	private final HashMap<SID,UUID> toUUID;
	
	public IDMap() {
		toSID = new HashMap<UUID,SID>();
		toUUID = new HashMap<SID,UUID>();
	}
	
	public synchronized void map( UUID uuid, SID sid ) {
		toSID.put( uuid, sid );
		toUUID.put( sid, uuid );
		notifyAll();
	}
	
	public synchronized SID get( UUID uuid ) {
		return toSID.get( uuid );
	}
	
	public synchronized UUID get( SID sid ) {
		return toUUID.get( sid );
	}

	public synchronized SID getWait( UUID uuid ) throws InterruptedException {
		
		SID id = toSID.get( uuid );
		while( id == null ) {
			wait();
			id = toSID.get( uuid );
		} while( uuid == null );
		
		return id;
	}
	
	public synchronized UUID getWait( SID sid ) throws InterruptedException {
		UUID id = toUUID.get( sid );
		while( id == null ) {
			wait();
			id = toUUID.get( sid );
		}
		return toUUID.get( sid );
	}
}
