package com.uab.caiac.bridge.gui;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.uab.caiac.R;
import com.uab.caiac.bridge.api.BluetoothNFCBrigdgeImpl;
import com.uab.caiac.bridge.api.IConstants;
import com.uab.caiac.bridge.business.Utils;

public class NFCBridgeGUI extends Activity{

	private ToggleButton mEnableBTtButton;
	private boolean isBluetoothEnabled = false;
	private ToggleButton mEnablePoolingtButton;
	private boolean isPoolingEnabled = false;

	private static List<String> mReceivedTags;

	private static TextView deviceNameTV;
	private static TextView tagValueTV;
	
	// handlers for callbacks to the UI thread
	private final Handler mBTConnectHandler = new Handler();
	private final Handler mBTDiconnectHandler = new Handler();
	private final Handler mPoolingConnectHandler = new Handler();
	private final Handler mPoolingDiconnectHandler = new Handler();
	private final static Handler mReceivedTagsHandler = new Handler();

	// create runnable for posting
	final Runnable mUpdateBTConnectResults = new Runnable() {
		public void run() {
			connectBluetooth();
		}
	};
	final Runnable mUpdateBTDisconnectResults = new Runnable() {
		public void run() {
			disconnectBluetooth();
			deviceNameTV.setText("---Disconnected---");
		}
	};
	final Runnable mUpdatePoolingConnectResults = new Runnable() {
		public void run() {
			connectPooling();
		}
	};
	final Runnable mUpdatePoolingDisconnectResults = new Runnable() {
		public void run() {
			disconnectPooling();
			tagValueTV.setText("---No Card Detected---");
		}
	};
	final static Runnable mUpdateReceivedTagsResults = new Runnable() {
		public void run() {
			updateReceivedTags();
		}
	};


	/**************************************************************************
	 * This Broadcast Receiver will keep listening of Intents sent by the Pooling
	 * Manager when some new Tags are identified.
	 * By using this approach the activity is only notified when some NFC Tags
	 * are available.
	 * Notes: 
	 * 	  - that this receiver MUST be registered within the activity "OnCreate".
	 *    - the activity MUST declare in the AndroidManifest.xml that it can receive 
	 *      this type of intents. (See the Intent Filter tag in AndroidManifest.xml 
	 *      for details).
	 */
	private final Handler mNFCTAGHandler = new Handler();
	private final BroadcastReceiver mIntentNFCTagsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String mAction = intent.getAction();
			if(mAction.equals(IConstants.INTENT_TRANSFER_NFC_TAGS)) {
				mReceivedTags = Utils.retrieveNFCTagsFromIntent(intent);
				mReceivedTagsHandler.post(mUpdateReceivedTagsResults);   
			}
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		/**********************************************************************
		 * Registration of the Broadcast Receiver to listen for notifications 
		 * from the NFCPoolingHandler listening for NFC tags. 
		 */
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(IConstants.INTENT_TRANSFER_NFC_TAGS);
		this.registerReceiver(mIntentNFCTagsReceiver, intentToReceiveFilter, null, mNFCTAGHandler);


		deviceNameTV = (TextView)findViewById(R.id.label_bt_device_value);
		tagValueTV = (TextView)findViewById(R.id.label_bt_tags_value);
		
		mEnableBTtButton = (ToggleButton)findViewById(R.id.bt_toggle_button);
		mEnableBTtButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if(isBluetoothEnabled){	
					mBTDiconnectHandler.post(mUpdateBTDisconnectResults);
				}
				else{ 
					mBTConnectHandler.post(mUpdateBTConnectResults);
				}
				
				if(!mEnableBTtButton.isChecked()){
					mEnablePoolingtButton.setEnabled(false);
				}
				else{
					mEnablePoolingtButton.setEnabled(true);
				}
			}
		});
		/**********************************************************************/

		mEnablePoolingtButton = (ToggleButton)findViewById(R.id.nfc_pooling_toggle_button);
		mEnablePoolingtButton.setEnabled(false);
		mEnablePoolingtButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if(!isPoolingEnabled){
					mPoolingConnectHandler.post(mUpdatePoolingConnectResults);

				}
				else{
					mPoolingDiconnectHandler.post(mUpdatePoolingDisconnectResults);
				}

				if(!mEnablePoolingtButton.isChecked()){
					mEnablePoolingtButton.setEnabled(true);
					mEnableBTtButton.setEnabled(true);
				}
				else{
					mEnableBTtButton.setEnabled(false);
				}
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void connectBluetooth(){
		try {
			BluetoothNFCBrigdgeImpl.getInstance().startBluetoothRFCommConnection("CAIAC",this);
		} catch (IOException e) {
			Log.d(IConstants.MY_TAG, "*** Bluetooth could not be connected: "+e);
			Toast.makeText(NFCBridgeGUI.this, "Could not connect to Bluetooth device", Toast.LENGTH_LONG).show();
			mEnableBTtButton.setChecked(false);
			return;
		} catch (Exception e) {
			Log.d(IConstants.MY_TAG, "*** Bluetooth could not be connected: "+e);
			Toast.makeText(NFCBridgeGUI.this, "Could not connect to Bluetooth device", Toast.LENGTH_LONG).show();
			mEnableBTtButton.setChecked(false);
			return;
		} 

		isBluetoothEnabled = true;
		deviceNameTV.setText(BluetoothNFCBrigdgeImpl.getInstance().getDeviceDescription());
	}

	private void disconnectBluetooth(){
		try {
			BluetoothNFCBrigdgeImpl.getInstance().stopBluetoothRFCommConnection();
		} catch (IOException e) {
			Log.d(IConstants.MY_TAG, "*** Bluetooth could not be disconnected: "+e);
		} catch (Exception e) {
			Log.d(IConstants.MY_TAG, "*** Bluetooth could not be discconnected: "+e);
		}
		isBluetoothEnabled = false;
	}

	private void connectPooling(){
		try {
			BluetoothNFCBrigdgeImpl.getInstance().startNFCPooling(2000, this);
		} catch (IOException e) {
			Log.d(IConstants.MY_TAG, "*** Pooling could not be started due to connection error: "+e);
		} catch (InterruptedException e) {
			Log.d(IConstants.MY_TAG, "*** Pooling could not be started due thread error: "+e);
		}
		isPoolingEnabled=true;
	}

	private void disconnectPooling(){
		try {
			BluetoothNFCBrigdgeImpl.getInstance().stopNFCPooling();
		} catch (IOException e) {
			Log.d(IConstants.MY_TAG, "*** Pooling could not be started due connection error 2: "+e);
		}
		isPoolingEnabled=false;
	}

	private static void updateReceivedTags(){
		if(mReceivedTags==null){
			return;
		}
		StringBuilder str = new StringBuilder();
		for(int i=0; i<mReceivedTags.size(); i++){
			str.append("Tag -> ").append(mReceivedTags.get(i)).append("\n");
		}

		tagValueTV.setText(str.toString());
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

}
