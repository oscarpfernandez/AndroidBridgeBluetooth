package com.uab.caiac.bridge.api;

import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uab.caiac.bridge.business.BluetoothConnectionHandler;
import com.uab.caiac.bridge.business.NFCPoolingHandler;

public final class BluetoothNFCBrigdgeImpl implements IBluetoothNFCBridge{

	private static BluetoothSocket mmBTSocket;
	private static BluetoothNFCBrigdgeImpl instance;

	private BluetoothNFCBrigdgeImpl(){}

	//Singleton design pattern.
	//Only one instance of this class is necessary.
	public static BluetoothNFCBrigdgeImpl getInstance(){
		if(instance==null){
			instance = new BluetoothNFCBrigdgeImpl();
		}
		return instance;
	}

	@Override
	public final boolean startBluetoothRFCommConnection(String deviceName, Activity context) throws Exception {
		Log.i(IConstants.MY_TAG, "*** BEGIN startBluetoothRFCommConnection"); 
		if(deviceName == null || deviceName.length()==0 || context==null){
			Log.d(IConstants.MY_TAG, "*** startBluetoothRFCommConnection - invalid arguments!");
			return false;
		}
		BluetoothDevice device = BluetoothConnectionHandler.getBluetoothDevice(context, deviceName);
		if(device==null){
			throw new Exception("The device does not have Bluetooth hardware...");
		}
		mmBTSocket = BluetoothConnectionHandler.createBluetoothRFCommSocket(device);
		BluetoothConnectionHandler.connectBluetoothRFCommSocket(mmBTSocket);
		Log.i(IConstants.MY_TAG, "*** END startBluetoothRFCommConnection");
		return true;
	}

	@Override
	public final boolean startNFCPooling(long poolingTime, Activity context) throws IOException, InterruptedException {
		if(mmBTSocket==null || poolingTime <=0 || context == null){
			Log.d(IConstants.MY_TAG, "*** startNFCPooling - invalid arguments!");
			return false;
		}
		Log.i(IConstants.MY_TAG, "*** BEGIN startNFCPooling"); 
		NFCPoolingHandler.getInstance().startPoolingBridge(mmBTSocket, poolingTime, context);
		Log.i(IConstants.MY_TAG, "*** END startNFCPooling"); 
		return true;
	}

	@Override
	public final void stopNFCPooling() throws IOException {
		Log.i(IConstants.MY_TAG, "*** BEGIN stopNFCPooling"); 
		NFCPoolingHandler.getInstance().stopPoolingBridge(mmBTSocket);
		Log.i(IConstants.MY_TAG, "*** END stopNFCPooling"); 
	}

	@Override
	public final void stopBluetoothRFCommConnection() throws IOException {
		Log.i(IConstants.MY_TAG, "*** BEGIN stopBluetoothRFCommConnection");
		BluetoothConnectionHandler.diconnectBluetoothRFCommSocket(mmBTSocket);
		Log.i(IConstants.MY_TAG, "*** END stopBluetoothRFCommConnection");
	}

	@Override
	public final String getDeviceDescription() {
		return BluetoothConnectionHandler.getDeviceDescription();
	}
}
