package com.uab.caiac.bridge.business;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uab.caiac.bridge.api.IConstants;

public final class NFCPoolingHandler {

	private static NFCPoolingHandler instance;

	/*
	 * 255 bytes is the theoretical buffer size maximum 
	 * supported by a NFC Tag.
	 */
	private byte[] mReadBuffer = new byte[255];
	private InputStream mInputStream;

	//Pooling thread manager
	private Thread mManagerThread; 
	//Flag to warn the thread manager to finish
	private boolean mFinishThread = false;

	private NFCPoolingHandler(){}

	public static NFCPoolingHandler getInstance(){
		if(instance==null){
			instance = new NFCPoolingHandler();
		}
		return instance;
	}

	public boolean startPoolingBridge(BluetoothSocket btSocket, long poolingTime, Activity context) 
	throws IOException, InterruptedException{
		if(btSocket==null || poolingTime <= 0 || context==null){
			return false;
		}
		//Wake up NFC hardware..
		sendMessageViaBluetooth(btSocket, NFCFrameHandler.TX_NFC_WAKE_UP.getNFCCall());

		//Set the flag to NOT STOP the thread!
		mFinishThread = false;

		//Connect pooling here...
		managerNFCPooling(btSocket, poolingTime, context);

		return true;
	}

	public boolean stopPoolingBridge(BluetoothSocket btSocket) throws IOException{
		if(btSocket==null){
			return false;
		}
		//Power-down NFC Hardware...
		sendMessageViaBluetooth(btSocket, NFCFrameHandler.TX_NFC_POWER_DOWN.getNFCCall());

		//set the flag TO STOP the thread and shutdown pooling...
		mFinishThread = true;

		return true;
	}

	private void managerNFCPooling(final BluetoothSocket btSocket, final long poolingTime, final Activity context) 
	throws IOException, InterruptedException{
		if(btSocket == null || poolingTime<=0){
			//arguments invalid
			return;
		}

		if(mManagerThread!=null && mManagerThread.isAlive()){
			//thread is already running!
			return;
		}

		mManagerThread = new Thread() {
			public void run() {
				try {
					mInputStream = btSocket.getInputStream();
				} catch (IOException e) {
					Log.d(IConstants.MY_TAG, "*** Error while getting the input stream...");
					return;
				}

				while(!mFinishThread){
					try {
						Log.d(IConstants.MY_TAG, "*** managerNFCPooling: Pooling NFC Messages..."); 
						//Send message for NFC Tag Reading Command...
						sendMessageViaBluetooth(btSocket, NFCFrameHandler.TX_NFC_SCAN_TAG.getNFCCall());
					} catch (IOException e) {
						Log.d(IConstants.MY_TAG, "*** Error while writing message to the NFC Socket: "+e);
						return;//exit thread
					}

					int numBytesRead = -1;
					try {
						numBytesRead = mInputStream.read(mReadBuffer);
					} catch (IOException e) {
						Log.d(IConstants.MY_TAG, "IOException occured... finishing thread: "+e);
						return;
					}
					Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Bytes read: "+ numBytesRead);
					Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Buffer: "+ Utils.convertToHexString(mReadBuffer, numBytesRead));

					if(!NFCFrameHandler.isNoCardDetectedRXMsg(mReadBuffer, numBytesRead)){
						//enters here if the message IS NOT a "no card" NFC response...

						List<byte[]> list = processNFCMessageExtractTags(mReadBuffer, numBytesRead);
						/*
						 * Send the notification Intent to be catched in the Activity context
						 * that has implemented and registered a BroadcastReceiver for this
						 * intent type.
						 */
						context.sendBroadcast(Utils.buildIntentWithNFCTags(list));
					}
					poolingSleep(poolingTime);
				}//while

				/*
				 * "clean" the input stream buffer, or next time we re-connect the pooling
				 * without closing the bluetooth socket the input stream buffer could
				 * contain "old" data.
				 */
				try {
					mInputStream.read(mReadBuffer);
				} catch (IOException e) {
					Log.d(IConstants.MY_TAG, "IOException occured... finishing thread: "+e);
					return;
				}

				Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Stop Pooling was requested... Finishing thread...");
				//exiting thread...
				return;
			}//run()
		};
		mManagerThread.setName("NFCPoolingHandler");
		mManagerThread.start();
	}

	private void poolingSleep(long poolingTime){
		try {
			Thread.sleep(poolingTime);
		} catch (InterruptedException e) {
			Log.d(IConstants.MY_TAG,"*** Error while sleeping...");
		}
	}

	private void sendMessageViaBluetooth(BluetoothSocket btSocket, byte[] outBuffer) 
	throws IOException {
		if(btSocket==null || outBuffer==null || outBuffer.length==0){
			return;
		}
		btSocket.getOutputStream().write(outBuffer,0,outBuffer.length-1);
	}

	private List<byte[]> processNFCMessageExtractTags(byte[] buffer, int dataSize){
		if(buffer==null || buffer.length<dataSize || dataSize <=0){
			//Should not happen...
			return Collections.emptyList();
		}

		byte[] nfcData = new byte[dataSize];
		System.arraycopy(buffer, 0, nfcData, 0, dataSize);

		if(NFCFrameHandler.isAcknowledge(nfcData)){
			return NFCFrameHandler.extractNFCTagData(nfcData);
		}
		else{
			return Collections.emptyList();	
		}
	}

}
