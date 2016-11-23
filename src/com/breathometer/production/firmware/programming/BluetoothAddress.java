package com.breathometer.production.firmware.programming;

public class BluetoothAddress
{
	private byte[] mBDA;
	
	
	
	private BluetoothAddress()
	{
		mBDA = new byte[6];
	}
	
	
	
	public static BluetoothAddress fromSerialNumber(String sn)
	{
		if (sn == null || sn.length() != 10)
			return null;
		
		if (!((sn.substring(0, 1)).equals("3")))
			return null;
		
		try
		{
			long num = Long.parseLong(sn.substring(5));
			long week = Long.parseLong(sn.substring(3, 5));
			byte year = (byte) sn.charAt(2);
			byte stage = (byte) sn.charAt(1);
			byte vendor = (byte) sn.charAt(0);
			
			BluetoothAddress bda = new BluetoothAddress();
			bda.mBDA[5] = vendor;
		    bda.mBDA[4] = stage;
		    bda.mBDA[3] = year;
		    bda.mBDA[2] = (byte) ((week << 2) | ((num >> 16) & 0x03));
		    bda.mBDA[1] = (byte) ((num >> 8) & 0x00FF);
		    bda.mBDA[0] = (byte) (num & 0x00FF);
		    
		    return bda;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
	
	
	
	public String toString()
	{
		return String.format("%02X%02X%02X%02X%02X%02X", mBDA[5], mBDA[4], mBDA[3], mBDA[2], mBDA[1], mBDA[0]);
	}
	
	public String toFormattedString()
	{
		return String.format("%02X:%02X:%02X:%02X:%02X:%02X", mBDA[5], mBDA[4], mBDA[3], mBDA[2], mBDA[1], mBDA[0]);
	}
	
	public byte[] get()
	{
		return mBDA;
	}
	
}
