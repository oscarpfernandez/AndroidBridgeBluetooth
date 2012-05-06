package com.uab.caiac.bridge.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.uab.caiac.bridge.api.IConstants;

public final class NFCFrameHandler {

	private String callName;
	private byte[] callValue;

	private NFCFrameHandler(String name, byte[] value){
		callName = name;
		callValue = value;
	}

	public String getCallName() {
		return callName;
	}

	public byte[] getNFCCall() {
		return callValue;
	}

	public int getCallValueLength(){
		return callValue.length;
	}

	/**************************************************************************
	 * NFC Request Commands
	 **************************************************************************/
	public static final NFCFrameHandler TX_NFC_WAKE_UP    = new NFCFrameHandler("TX_NFC_WakeUp", new byte[]{0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF, 0x03, (byte) 0xFD, (byte) 0xD4, 0x14, 0x01, 0x17, 0x00});						
	public static final NFCFrameHandler TX_NFC_POWER_DOWN = new NFCFrameHandler("TX_NFC_PowerDown", new byte[]{0x00, 0x00, (byte)0xFF, 0x03, (byte)0xFD, (byte)0xD4, 0x16, 0x10, 0x06, 0x00});
	public static final NFCFrameHandler TX_NFC_SCAN_TAG   = new NFCFrameHandler("TX_NFC_ScanTag", new byte[]{0x00, 0x00, (byte)0xFF, 0x06, (byte)0xFA, (byte)0xD4, 0x60, 0x01, 0x01, 0x00, 0x04, (byte)0xC6, 0x00});
	public static final NFCFrameHandler TX_NFC_GET_STATUS = new NFCFrameHandler("TX_NFC_GetStatus", new byte[]{0x00, 0x00, (byte)0xFF, 0x02, (byte)0xFE, (byte)0xD4, 0x04, 0x28, 0x00});
	/**************************************************************************
	 * "No card" response
	 **************************************************************************/
	public static final NFCFrameHandler RX_NFC_NOCARD = new NFCFrameHandler("RX_NFC_NOCARD", new byte[]{0x00, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, 0x00, 0x00, (byte)0xFF, 0x03, (byte)0xFD, (byte)0xD5, 0x61, 0x00, (byte)0xCA, 0x00 });

	/**************************************************************************
	 * NFC Acknowlegde / Not Acknowledge
	 **************************************************************************/
	public static final NFCFrameHandler NFC_ACK = new NFCFrameHandler("NFC_ACK", new byte[]{0x00, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00});
	public static final NFCFrameHandler NFC_NACK = new NFCFrameHandler("NFC_NACK", new byte[]{0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0x00, 0x00});

	/**
	 * Checks if the received data is valid, meaning that it begins with ACK. 
	 * @param receivedData
	 * @return True is the data begins with ACK byte data.
	 */
	public static boolean isAcknowledge(byte[] dataBuffer){
		if(dataBuffer==null || dataBuffer.length<=6){
			return false;
		}

		//Copy the 6 initial bytes of the received data array...
		byte[] copy = new byte[6];
		System.arraycopy(dataBuffer, 0, copy, 0, copy.length);

		return Arrays.equals(copy, NFC_ACK.getNFCCall());
	}

	/**
	 * Checks if the NFC response message refers to a "No Card" detected
	 * response.
	 * @param buffer The read buffer.
	 * @param readedBytes Number of bytes to consider.
	 * @return True if the buffer contains a "No Card" NFC response message.
	 * False otherwise.
	 */
	public static boolean isNoCardDetectedRXMsg(byte[] buffer, int readedBytes){
		if(buffer==null || buffer.length==0 || readedBytes==0 || readedBytes > buffer.length){
			//Parameters are wrong. Discard message.
			return true;
		}

		byte[] data = new byte[readedBytes];
		System.arraycopy(buffer, 0, data, 0, readedBytes);

		return Arrays.equals(data, NFCFrameHandler.RX_NFC_NOCARD.getNFCCall());
	}

	/**
	 * Extracts the NFC Tags existing in the data frame. It can retrieve one or two
	 * NFC tags. Only JEWEL and MIFARE Tags are supported right now.
	 * @param dataBuffer
	 * @return A list of retrieved tags.
	 */
	public static List<byte[]> extractNFCTagData(byte[] dataBuffer){
		if(dataBuffer == null){
			return Collections.emptyList();
		}
		
		/*
		 * DO NOT change the order of this method if you don't know what you're
		 * doing! The sequence is relevant to be robust against all types of messages
		 * specially those generated specially during the first NFC calls, that carry
		 * a lot of weird junk data...
		 */

		ArrayList<byte[]> tagsNFC = new ArrayList<byte[]>();

		//get size of the frame data byte #10 of the message
		//from the frame we ignore the "FrameID" and the "CommandCode"
		int frameSize = ((int)dataBuffer[9])-2;

		if(frameSize==0 || dataBuffer.length<frameSize+13){
			//just received an acknowledge without more data
			//nothing to do...
			return Collections.emptyList();
		}

		//Check CRC of the data received...
		if(!isDataCRCsOk(dataBuffer)){
			//CRC check failed... data is corrupted. Ignore frame.
			Log.d(IConstants.MY_TAG, "*** Data Frame CRC check failed!");
			return Collections.emptyList();
		}
		
		logger("*** FrameSize = "+frameSize);

		byte[] data = new byte[frameSize];
		
		//Keep only the information from "# of tags field"...
		System.arraycopy(dataBuffer, 13, data, 0, frameSize);
	
		logger("*** data[] = "+ Utils.convertToHexString(data, data.length));

		//First byte data tells us the number of readed tags...
		int readedTags = (int)data[0];
		
		if(readedTags == 0){
			return Collections.emptyList();
		}

		logger("*** Number of Tags = "+readedTags);

		/****************************
		 * Read the NFC Tag #1...
		 ****************************/
		int lengthDataFrame1 = (int)data[2];
		//extract frame of the Tag#1 from "Tag Type" field...
		byte[] dataFrame1 = new byte[lengthDataFrame1 + 2];
		System.arraycopy(data, 1, dataFrame1, 0, dataFrame1.length);

		byte[] tagNFC1 = readTagInformation(dataFrame1);

		logger("*** dataFrame1[] = "+ Utils.convertToHexString(dataFrame1, dataFrame1.length));
		logger("*** Read TAG1 = "+ Utils.convertToHexString(tagNFC1, tagNFC1.length));

		tagsNFC.add(tagNFC1);

		/****************************
		 * Read the NFC Tag #2...
		 ****************************/
		if(readedTags==2){
			byte[] dataFrame2 = new byte[data.length - dataFrame1.length - 1];
			//extract frame of the Tag#2 from "Tag Type" field...
			System.arraycopy(data, dataFrame1.length+1, dataFrame2, 0, dataFrame2.length);
			byte[] tagNFC2 = readTagInformation(dataFrame2);

			logger("*** dataFrame2[] = "+ Utils.convertToHexString(dataFrame2, dataFrame2.length));
			logger("*** Read TAG2 = "+ Utils.convertToHexString(tagNFC2, tagNFC2.length));

			tagsNFC.add(tagNFC2);
		}

		return tagsNFC;
	}

	private static byte[] readTagInformation(byte[] dataFrame){
		switch ((int)dataFrame[0]) {
		case (int)(NFCCardType.MIFARE_TYPE):
			return readTagMifare(dataFrame);
		case (int)(NFCCardType.JEWEL_TYPE):
			return readTagJewel(dataFrame);
		default:
			//this should not happen... just in case...
			return new byte[0];
		}
	}

	private static byte[] readTagMifare(byte[] dataFrame){
		byte tag[] = new byte[dataFrame.length - 7];
		System.arraycopy(dataFrame, 7, tag, 0, tag.length);
		return tag;
	}

	private static byte[] readTagJewel(byte[] dataFrame){
		byte tag[] = new byte[dataFrame.length - 5];
		System.arraycopy(dataFrame, 5, tag, 0, tag.length);
		return tag;
	}

	private static boolean isDataCRCsOk(byte[] dataBuffer){
		if(dataBuffer==null || dataBuffer.length<13){
			return false;
		}

		//Check#1 - NFC length CRC. 
		if((int)(dataBuffer[9]+dataBuffer[10])!=0){
			//NFC frame has a incorrect Len frame.
			return false;
		}

		//Check#2 the "global" CRC.
		byte sum = 0;
		for(int i=11; i<=dataBuffer.length-3; i++){
			sum += dataBuffer[i];
		}
		
		if(sum + dataBuffer[dataBuffer.length-2] != 0){
			return false;
		}

		return true;
	}
	
	public static boolean isFirmwareRXOk(byte[] dataBuffer, int readedBytes){
		if(dataBuffer==null || readedBytes<=18){
			return false;
		}
		
		//extract the relevant bytes...
		byte[] buffer = new byte[readedBytes];
		System.arraycopy(dataBuffer, 0, buffer, 0, readedBytes);
		
		//get acknowledge info...
		byte[] ack = new byte[6];
		System.arraycopy(dataBuffer, 0, ack, 0, 6);
		if(!Arrays.equals(ack, NFCFrameHandler.NFC_ACK.getNFCCall())){
			return false;
		}
		
		//compute length CRC...
		if(buffer[9]+buffer[10] != 0){
			//lan CRC failed
			return false;
		}
		
		//compute global CRC...
		byte crcSum = 0;
		for(int i=11; i<=buffer.length-3;i++){
			crcSum += buffer[i];
		}
		if(crcSum + buffer[buffer.length-2] != 0){
			return false;
		}
		
		return true;
	}

	private static void logger(String message){
		Log.d(IConstants.MY_TAG, message);
	}

}
