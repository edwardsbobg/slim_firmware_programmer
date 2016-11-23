package com.breathometer.production.firmware.programming;

public class DeviceInfo
{
	private String mBDA;
	private String mSN;
	private String mSNPrefix;
	private static final String DELIMINATOR = "[,]";
	private static final int NUM_TOKENS = 2;
	private static final int SN_LENGTH = 11;
	private static final int BDA_LENGTH = 12;
	private static final String BDA_PREFIX = "A81559";

	public DeviceInfo(String info, String snPrefix) throws Exception {

		if (info == null || info.length() < SN_LENGTH + BDA_LENGTH + 1 || snPrefix == null || snPrefix.length() != 1) {
			throw new Exception("Invalid input args");
		}

		String[] tokens = info.split(DELIMINATOR);

		if ( tokens.length != NUM_TOKENS) {
			throw new Exception("Invalid Info Format");
		}

		mSN = tokens[0];
		mBDA = tokens[1];
		mSNPrefix = snPrefix;

		if ( !isSerialNumberValid() )
			throw new Exception("Serial Number Invalid");
		if ( !isBDAValid())
            throw new Exception("Bluetooth Device Invalid");
	}

	private boolean isSerialNumberValid(){
		if ( mSN == null || mSN.length() != SN_LENGTH) {
			return false;
		}

		if ( !mSN.startsWith(mSNPrefix) ) {
			return false;
		}

		boolean hasLowercase = !mSN.equals(mSN.toUpperCase());
		if (hasLowercase) {
			return false;
		}

		return true;
	}

	private boolean isBDAValid(){
		if ( mBDA == null || mBDA.length() != BDA_LENGTH) {
			return false;
		}

		if ( !mBDA.startsWith(BDA_PREFIX) ) {
			return false;
		}
		return true;
	}

	public String getSerialNumber() {
		return mSN;
	}

	public String getBluetoothDeviceAddress() {
		return mBDA;
	}
}
