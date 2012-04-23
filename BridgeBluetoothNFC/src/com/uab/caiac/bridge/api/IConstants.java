package com.uab.caiac.bridge.api;

public interface IConstants {

	public static final String MY_TAG = "Bridge_Bluetooth_NFC_API";
	
	/**
	 * IMPORTANT NOTE - README!
	 * This defines the intent action string for sending new captured NFC tags.
	 * The activity that uses the NFC tags should register this Intent action
	 * in the Intent action filter (in Android.manifest) to receive this data on 
	 * the BroadcastReceiver also implemented by this activity.
	 */
	public static final String INTENT_TRANSFER_NFC_TAGS = "com.uab.caiac.bridge.TRANSFER_NFC_TAGS";
	
	public static final String ID_NFC_TAG_1 = "TAG1";
	public static final String ID_NFC_TAG_2 = "TAG2";
	
}
