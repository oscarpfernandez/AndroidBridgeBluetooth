package com.uab.caiac.bridge.business;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uab.caiac.bridge.api.IConstants;

public final class BluetoothConnectionHandler {

	private static BluetoothAdapter mBluetoothAdapter;
	private static BluetoothDevice mBTdevice;
	
	public final static BluetoothDevice getBluetoothDevice(Activity context, String deviceName) 
	throws IOException{
		if(context == null || deviceName == null || deviceName.length()==0){
			throw new IOException("The parameters are not valid...");
		}
		
		//get the hardware BT adapter handler...
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null){
			//Bluetooth hardware is not present in the device...
			throw new IOException("The device does not have bluetooth hardware!");
		}
		
		//check if BT it's enabled = activated...
		if (!mBluetoothAdapter.isEnabled()) {
			/*
			 * Enable bluetooth adapter hardware.
			 * We wait some time for the adapter state change to STATE_ON
			 * otherwise the it will not be possible to connect the Bluetooth socket!! 
			 */
			mBluetoothAdapter.enable();
			int iter = 0;
			while (mBluetoothAdapter.getState()!=BluetoothAdapter.STATE_ON && iter++<=50) {
				//wait for the Bluetooth adapter gets on STATE_ON a maximum of 10 seconds....
				try {
					//Wait 
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Log.d(IConstants.MY_TAG, "Failed on sleep...");
				}
			}
		}
		ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices());
		
		// Loop through paired devices to find OUR device bridge.
		for (BluetoothDevice device : pairedDevices) {
			if(device.getName().contains(deviceName.toUpperCase())){
				Log.d(IConstants.MY_TAG,"*** Paired device -> "+device.getName() + "@" + device.getAddress());
				mBTdevice = device;
				
				break;
			}
		}
		
		if(mBTdevice == null){
			throw new IOException("The device \""+deviceName+"\""+" was not found!"  );
		}
		
		return mBTdevice;
	}

	public final static BluetoothSocket createBluetoothRFCommSocket(BluetoothDevice device) 
	throws IOException, Exception{
		BluetoothSocket btSocket = null;
		if(device == null){
			throw new IOException("The parameter is not valid...");
		}
		
		Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
		btSocket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1)); 
		
		Log.d(IConstants.MY_TAG,"*** Bluetooth class: "+btSocket.getRemoteDevice().getBluetoothClass().toString());
		
		return btSocket;
	}
	
	public final static boolean connectBluetoothRFCommSocket(BluetoothSocket btSocket) 
	throws IOException{
		if(btSocket == null){
			return false;
		}
		Log.d(IConstants.MY_TAG, "*** Trying to connect the socket...");
		btSocket.connect();
		Log.d(IConstants.MY_TAG, "*** Connected to the socket...");
		return true;
	}
	
	public final static boolean diconnectBluetoothRFCommSocket(BluetoothSocket btSocket) 
	throws IOException{
		if(btSocket == null){
			return false;
		}
		btSocket.close();
		
		//disconnect bluetooth...
		if(mBluetoothAdapter.isEnabled()){
			mBluetoothAdapter.disable();
			int iter = 0;
			while (mBluetoothAdapter.getState()!=BluetoothAdapter.STATE_OFF && iter++<=50) {
				//wait for the Bluetooth adapter gets on STATE_OFF a maximum of 10 seconds....
				try {
					//Wait 
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Log.d(IConstants.MY_TAG, "Failed on sleep...");
				}
			}
		}
		Log.d(IConstants.MY_TAG, "*** Disconnected the socket...");
		return true;
	}
	
	public final static String getDeviceDescription(){
		if(mBTdevice == null){
			return new String("");
		}
		
		return mBTdevice.getName()+" @ "+mBTdevice.getAddress();
	}
		
}

