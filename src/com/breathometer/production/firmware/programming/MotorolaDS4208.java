package com.breathometer.production.firmware.programming;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

public class MotorolaDS4208 implements KeyEventDispatcher
{
	private final static int PREFIX = 61450; // F23
	private final static int SUFFIX = 61451; // F24
	private final MotorolaDS4208Interface mMotorolaDS4208Interface;
	private StringBuilder mStringBuilder = new StringBuilder();
	private boolean mPrefixed = false;
	
	
	
	public MotorolaDS4208(MotorolaDS4208Interface motorolaDS4208Interface)
	{
		mMotorolaDS4208Interface = motorolaDS4208Interface;
	}
	
	
	



	@Override
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		if (e.getKeyCode() == PREFIX)
		{
			mPrefixed = true;
		}
		else if ((e.getKeyCode() == SUFFIX) && (mPrefixed))
		{
			mMotorolaDS4208Interface.scannedString(mStringBuilder.toString());
			mStringBuilder = new StringBuilder();
			mPrefixed = false;
		}
		else if ((e.getID() == KeyEvent.KEY_TYPED) && (mPrefixed))
		{
			mStringBuilder.append(e.getKeyChar());
		}
		
		return false;
	}
	
	
	
	public interface MotorolaDS4208Interface
	{
		void scannedString(String string);
	}











	
	
	
}
