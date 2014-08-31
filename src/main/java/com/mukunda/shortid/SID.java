
package com.mukunda.shortid;

public class SID {
	private final int id;
	
	public SID( int id ) {
		this.id = id;
	}
	
	public SID( byte[] bytes ) {
		if( bytes.length != 4 ) throw new IllegalArgumentException( "Input must be 4 bytes." );
		id = (((int)(bytes[0]))&0xFF) | 
				((((int)(bytes[1]))&0xFF)<<8) | 
				((((int)(bytes[2]))&0xFF)<<16) |
				((((int)(bytes[3]))&0xFF)<<24);
	}
	
	public boolean valid() {
		return id != 0;
	}
	
	public int getInt() {
		return id;
	} 
	
	public int hashCode() {
		return id;
	}
	
	public boolean equals( Object other ) {
		return (other instanceof SID) && 
				(id == ((SID)other).id);
	}
	
	public String toString() {
		return String.format( "%08X", id );
	}
}
