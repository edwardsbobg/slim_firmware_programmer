package com.breathometer.production.firmware.programming;

public class SerialNumber
{
	
	public static String fromBluetoothAddress(BluetoothAddress bda)
	{
		if (bda == null)
			return null;
		
		byte[] _bda = bda.get();
		
		long num = ((_bda[2] & 0x03) << 16) | (_bda[1] << 8) | (_bda[0]);
		byte week = (byte) ((_bda[2] & 0xFC) >> 2);
		byte year = _bda[3];
		byte stage = _bda[4];
		byte vendor = _bda[5];
		
		char[] sn = new char[10];
		sn[0] = (char) (vendor & 0x00FF);
		sn[1] = (char) (stage & 0x00FF);
		sn[2] = (char) (year & 0x00FF);
		sn[3] = (char) ((((week / 10) % 10) + 48) & 0x00FF);
		sn[4] = (char) (((week % 10) + 48) & 0x00FF);
		sn[5] = (char) ((((num / 10000) % 10) + 48) & 0x00FF);
		sn[6] = (char) ((((num / 1000) % 10) + 48) & 0x00FF);
		sn[7] = (char) ((((num / 100) % 10) + 48) & 0x00FF);
		sn[8] = (char) ((((num / 10) % 10) + 48) & 0x00FF);
		sn[9] = (char) (((num % 10) + 48) & 0x00FF);
		
		return new String(sn);
	}
	
}
