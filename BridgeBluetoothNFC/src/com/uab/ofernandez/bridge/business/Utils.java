package com.uab.ofernandez.bridge.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Intent;

import com.uab.ofernandez.bridge.api.IConstants;

public final class Utils {

	/**
	 * Converts an array of bytes to its hexadecimal string representation.
	 * @param buffer The byte array that we want to convert.
	 * @param numBytes The initial number of bytes we want to consider.
	 * @return The string representation or and empty string in case of
	 * 		invalid parameters.
	 */
	public static String convertToHexString(byte[] buffer, int numBytes){
		if(buffer==null || numBytes<=0 || numBytes>buffer.length){
			return new String("");
		}

		StringBuilder hexString = new StringBuilder();

		for(int i=0; i<numBytes; i++){
			String hex = Integer.toHexString(0xFF & buffer[i]);
			if(hex.length()==2){
				hexString.append(hex);
			}
			else{//length 1
				hexString.append("0").append(hex);
			}
		}
		return hexString.toString();
	}

	/**
	 * Builds an Intent containing the NFC tags detected, for later broadcast
	 * to the Broadcast receiver implemented by some Activity.
	 * @param tagsList The list of detected NFC tags
	 * @return Intent containing tag information.
	 */
	public static Intent buildIntentWithNFCTags(List<byte[]> tagsList){
		Intent intent = new Intent();
		intent.setAction(IConstants.INTENT_TRANSFER_NFC_TAGS);
		if(tagsList.size()>0){
			byte[] tag1 = tagsList.get(0);
			intent.putExtra(IConstants.ID_NFC_TAG_1, Utils.convertToHexString(tag1, tag1.length));
		}
		if(tagsList.size()==2){
			byte[] tag1 = tagsList.get(1);
			intent.putExtra(IConstants.ID_NFC_TAG_2, Utils.convertToHexString(tag1, tag1.length));
		}
		return intent;
	}

	/**
	 * Extracts the NFC tags from a received Intent.
	 * @param intent Intent instance.
	 * @return A list of NFC Tag strings.
	 */
	public static List<String> retrieveNFCTagsFromIntent(Intent intent){
		if(intent==null || !intent.getAction().equals(IConstants.INTENT_TRANSFER_NFC_TAGS)){
			return Collections.emptyList();
		}

		ArrayList<String> tags = new ArrayList<String>();

		String tag1 = intent.getStringExtra(IConstants.ID_NFC_TAG_1);
		String tag2 = intent.getStringExtra(IConstants.ID_NFC_TAG_2);

		if(tag1!=null){ tags.add(tag1);}
		if(tag2!=null){ tags.add(tag2);}

		return tags;
	}

}
