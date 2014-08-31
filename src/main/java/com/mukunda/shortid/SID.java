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
