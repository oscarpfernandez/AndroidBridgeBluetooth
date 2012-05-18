package com.uab.ofernandez.bridge.business;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uab.ofernandez.bridge.api.IConstants;

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

	//debug flag
	private boolean D = IConstants.DEBUG_ENABLED;

	/*
	 * Establishes the pauses that are required between a RX and TX command.
	 * In other words, enforces the required pause times between a call and
	 * its response. This is mandatory in order to get the call system
	 * synchronized and functioning properly.
	 *
	 * DO NOT change these parameters if you don't know what you're doing!
	 * Otherwise the system may be unresponsive!
	 */
	private static final long PAUSE_WAKE_UP_RX = 50;
	private static final long PAUSE_READ_TAG_RX = 300;
	private static final long PAUSE_GETSTATUS = 300;
	private static final long PAUSE_READ_POWERDOWN_RX = 50;
	private static long POOLING_TIME;

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

		//set the flag TO STOP the thread and shutdown pooling...
		mFinishThread = true;

		//wait until the thread finishes for a maximum of 4 seconds...
		//it will be quicker than this...
		if(mManagerThread != null){
			int iter = 0;
			while(mManagerThread.isAlive() && iter++<200){
				poolingSleep(20);
			}
		}
		return true;
	}

	private void managerNFCPooling(final BluetoothSocket btSocket, final long poolingTime, final Activity context)
	throws IOException, InterruptedException{
		if(btSocket == null || poolingTime<=0){
			//arguments invalid
			return;
		}

		POOLING_TIME = poolingTime - (PAUSE_WAKE_UP_RX+PAUSE_READ_TAG_RX+PAUSE_GETSTATUS+PAUSE_READ_POWERDOWN_RX);

		if(POOLING_TIME < 150){
			//the overall pooling time will be at least of 500ms;
			POOLING_TIME = 150;
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
					Log.e(IConstants.MY_TAG, "*** Error while getting the input stream...");
					return;//exit thread
				}

				while(!mFinishThread){
					int numBytesRead = 0;

					/********************************
					 * Wake up NFC Hardware...
					 ********************************/
					try {
						sendMessageViaBluetooth(btSocket, NFCFrameHandler.TX_NFC_WAKE_UP.getNFCCall());

						//if(D){Log.d(IConstants.MY_TAG, "*** Wake Up len = "+ NFCFrameHandler.TX_NFC_WAKE_UP.getNFCCall().length);}

						poolingSleep(PAUSE_WAKE_UP_RX);//wait for the NFC module build up completely its buffer...
						//without this the messege will be sliced.

						numBytesRead = mInputStream.read(mReadBuffer);

						if(D){
							Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Bytes read - Wake Up RX: "+ numBytesRead);
							Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Buffer - Wake Up RX: "+ Utils.convertToHexString(mReadBuffer, numBytesRead));
						}

					} catch (IOException e1) {
						Log.e(IConstants.MY_TAG, "*** Error while sending message...");
						return;//exit thread
					}


					/********************************
					/* Read NFC Tags message...
					 ********************************/
					try {
						if(D){Log.d(IConstants.MY_TAG, "*** managerNFCPooling: Pooling NFC Messages...");}

						//Send message for NFC Tag Reading Command...
						sendMessageViaBluetooth(btSocket, NFCFrameHandler.TX_NFC_SCAN_TAG.getNFCCall());

						//if(D){Log.d(IConstants.MY_TAG, "*** READ TAG len = "+ NFCFrameHandler.TX_NFC_SCAN_TAG.getNFCCall().length);}
					} catch (IOException e) {
						Log.e(IConstants.MY_TAG, "*** Error while writing message to the NFC Socket: "+e);
						return;//exit thread
					}

					poolingSleep(PAUSE_READ_TAG_RX);//wait for the NFC module build up completely its buffer...
					                  //without this the message will be sliced.

					try {
						numBytesRead = mInputStream.read(mReadBuffer);
					} catch (IOException e) {
						Log.e(IConstants.MY_TAG, "IOException occured... finishing thread: "+e);
						return;
					}

					if(D){
						Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Bytes read: "+ numBytesRead);
						Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Buffer: "+ Utils.convertToHexString(mReadBuffer, numBytesRead));
					}

					if(!NFCFrameHandler.isNoCardDetectedRXMsg(mReadBuffer, numBytesRead)){
						//enters here if the message IS NOT a "no card" NFC response...

						List<byte[]> list = processNFCMessageExtractTags(mReadBuffer, numBytesRead);
						/*
						 * Send the notification Intent to be catched in the Activity context
						 * that has implemented and registered a BroadcastReceiver for this
						 * intent type.
						 */
						if(!list.isEmpty()){
							context.sendBroadcast(Utils.buildIntentWithNFCTags(list));
						}
					}

					/***********************************
					/* Power Down the NFC hardware...
					 ***********************************/
					try {
						sendMessageViaBluetooth(btSocket, NFCFrameHandler.TX_NFC_POWER_DOWN.getNFCCall());

						//if(D){Log.d(IConstants.MY_TAG, "*** Power Down len = "+ NFCFrameHandler.TX_NFC_POWER_DOWN.getNFCCall().length);}

						poolingSleep(PAUSE_READ_POWERDOWN_RX);//wait for the NFC module build up completely its buffer...
						                 //without this the message will be sliced.

						numBytesRead = mInputStream.read(mReadBuffer);

						if(D){
							Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Bytes read - PowerDown RX: "+ numBytesRead);
							Log.d(IConstants.MY_TAG, "*** managerNFCPooling - Buffer - PowerDown RX: "+ Utils.convertToHexString(mReadBuffer, numBytesRead));
						}
					} catch (IOException e) {
						Log.e(IConstants.MY_TAG, "IOException occured... finishing thread: "+e);
						return;
					}

					//pooling pause....
					poolingSleep(POOLING_TIME);

				}///// while //////

				Log.i(IConstants.MY_TAG, "*** managerNFCPooling - Stop Pooling was requested... Finishing thread...");
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
			Log.e(IConstants.MY_TAG,"*** Error while sleeping...");
		}
	}

	private void sendMessageViaBluetooth(BluetoothSocket btSocket, byte[] outBuffer)
	throws IOException {
		if(btSocket==null || outBuffer==null || outBuffer.length==0){
			return;
		}
		btSocket.getOutputStream().write(outBuffer,0,outBuffer.length);
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
