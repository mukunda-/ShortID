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

/**
 * ShortID container
 * 
 * @author mukunda
 *
 */
public class SID {
	private final int id;
	
	/*********************************************************
	 * Wrap a raw Short ID.
	 * 
	 * @param id 32-bit SID value.
	 *********************************************************/
	public SID( int id ) {
		this.id = id;
	}
	
	/*********************************************************
	 * Construct the ID from bytes.
	 * 
	 * @param bytes Array of bytes, length must be 4.
	 *********************************************************/
	public SID( byte[] bytes ) {
		if( bytes.length != 4 ) throw new IllegalArgumentException( "Input must be 4 bytes." );
		id = (((int)(bytes[0]))&0xFF) | 
				((((int)(bytes[1]))&0xFF)<<8) | 
				((((int)(bytes[2]))&0xFF)<<16) |
				((((int)(bytes[3]))&0xFF)<<24);
	}
	
	/*********************************************************
	 * Checks if this SID is valid (not zero)
	 * 
	 * @return false if this SID cannot represent a player.
	 *********************************************************/
	public boolean valid() {
		return id != 0;
	}
	
	/*********************************************************
	 * Unwraps this SID.
	 * 
	 * @return SID number.
	 *********************************************************/
	public int getInt() {
		return id;
	} 
	
	/*********************************************************
	 * Hash code function
	 * 
	 * @return hash code
	 *********************************************************/
	public int hashCode() {
		return id;
	}
	
	/*********************************************************
	 * Equality function
	 * 
	 * @return true if the other object is an SID 
	 *              instance and contains the same ID
	 *********************************************************/
	public boolean equals( Object other ) {
		return (other instanceof SID) && 
				(id == ((SID)other).id);
	}
	
	/*********************************************************
	 * Convert to a string
	 * 
	 * Format: "XXXXXXXX", 8 upper-case hex digits.
	 * 
	 * @return String representation of SID.
	 *********************************************************/
	public String toString() {
		return String.format( "%08X", id );
	}

	/*********************************************************
	 * Convert a hex string into an SID.
	 * 
	 * @param  String of 8 hexadecimal digits.
	 * @return Converted SID.
	 *********************************************************/
	public static SID fromString( String string ) {
		return new SID(Integer.parseInt( string, 16 ));
	}
}
