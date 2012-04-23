package com.uab.caiac.bridge.api;

import java.io.IOException;

import android.app.Activity;

public interface IBluetoothNFCBridge {
	/**
	 * Creates a SSP/RFComm socket and connects it to the device specified.
	 * @param deviceName The name of the device that we want to connect to.
	 * @param context The activity context that is performing the call. 
	 * @return True if everything goes without errors
	 * @throws IOException 
	 * @throws Exception
	 */
	public boolean startBluetoothRFCommConnection(String deviceName, Activity context) throws IOException, Exception;
	
	/**
	 * Activates the NFC pooling to read NFC tags. This should be called only AFTER the 
	 * 		"startBluetoothRFCommConnection" method, since it obviously depends on the
	 * 		bluetooth socket previously created.
	 * @param poolingTime The pooling time in miliseconds.
	 * @param context The activity context caller. This same activity will receive the 
	 * 		intents with the readed tags.
	 * @return True if everything goes without errors.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean startNFCPooling(long poolingTime, Activity context) throws IOException, InterruptedException;
	
	/**
	 * Stops the NFC pooling..
	 * @throws IOException
	 */
	public void stopNFCPooling() throws IOException;
	
	/**
	 * Closes the SPP/RFComm connection. This should be called only AFTER the 
	 * 		"stopNFCPooling" is called.
	 * @throws IOException
	 */
	public void stopBluetoothRFCommConnection() throws IOException;
	
	/**
	 * Gets the device name and bluetooth address that we are connected to.
	 * This can be useful for monitoring and debugging purposes.
	 * @return A string with the format "device_name @ XX:XX:XX:XX:XX:XX"
	 */
	public String getDeviceDescription();
}
