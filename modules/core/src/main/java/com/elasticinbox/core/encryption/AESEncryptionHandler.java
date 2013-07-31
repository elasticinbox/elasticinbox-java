/**
 * Copyright (c) 2011-2012 Optimax Software Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Optimax Software, ElasticInbox, nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.elasticinbox.core.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.model.Address;
import com.elasticinbox.core.model.AddressList;
import com.elasticinbox.core.model.Message;

/**
 * This class provides AES encryption/decryption methods.
 * 
 * @author <ul>
 *         <li>Rustam Aliyev</li>
 *         <li>itembase GmbH, John Wiesel <jw@itembase.biz></li>
 *         </ul>
 */
public class AESEncryptionHandler implements EncryptionHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(AESEncryptionHandler.class);

	/**
	 * Use AES-CBC algorithm with PKCS5 padding
	 */
	public static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

	private Cipher cipher;

	private static final int ENCRYPT = 1;
	private static final int DECRYPT = -1;

	private int mode = ENCRYPT;

	public InputStream encrypt(InputStream in, Key key, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {

		cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		this.mode = ENCRYPT;

		return new CipherInputStream(in, cipher);
	}

	public InputStream decrypt(InputStream in, Key key, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {

		cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		this.mode = DECRYPT;

		return new CipherInputStream(in, cipher);
	}

	/*
	 * encrypts most message object attributes, in particular: AddressLists
	 * from, to, cc, bcc and Strings subject plainBody htmlBody
	 */
	public Message encryptMessage(Message message, Key key, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException {

		cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		this.mode = ENCRYPT;

		return cryptMessage(message, key, iv);
	}

	/*
	 * decrypt a message object
	 */
	public Message decryptMessage(Message message, Key key, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException {

		cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		this.cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		this.mode = DECRYPT;

		return cryptMessage(message, key, iv);
	}

	private Message cryptMessage(Message message, Key key, byte[] iv) {

		if (message.getPlainBody() != null) {
			message.setPlainBody(cryptString(message.getPlainBody(), key, iv));
		}
		if (message.getHtmlBody() != null) {
			message.setHtmlBody(cryptString(message.getHtmlBody(), key, iv));
		}
		if (message.getSubject() != null) {
			message.setSubject(cryptString(message.getSubject(), key, iv));
		}
		if (message.getFrom() != null) {
			if (!message.getFrom().isEmpty()) {
				message.setFrom(cryptAddressList(message.getFrom(), key, iv));
			}
		}
		if (message.getTo() != null) {
			if (!message.getTo().isEmpty()) {
				message.setTo(cryptAddressList(message.getTo(), key, iv));
			}
		}

		if (message.getCc() != null) {
			if (!message.getCc().isEmpty()) {
				message.setCc(cryptAddressList(message.getCc(), key, iv));
			}
		}
		if (message.getBcc() != null) {
			if (!message.getBcc().isEmpty()) {
				message.setBcc(cryptAddressList(message.getBcc(), key, iv));
			}
		}
		return message;
	}

	private AddressList cryptAddressList(AddressList from, Key key, byte[] iv) {

		AddressList temp = new AddressList();
		Iterator<Address> addresses = from.iterator();

		if (addresses.hasNext()) {
			Address address = cryptAddress(addresses.next(), key, iv);
			temp = new AddressList(address);
			while (addresses.hasNext()) {
				address = cryptAddress(addresses.next(), key, iv);
				temp.add(address);
			}
		}

		return temp;
	}

	private Address cryptAddress(Address address, Key key, byte[] iv) {

		String name = cryptString(address.getName(), key, iv);
		String addressString = cryptString(address.getAddress(), key, iv);
		address = new Address(name, addressString);
		return address;
	}

	private String cryptString(String toCrypt, Key key, byte[] iv) {
		if (this.mode == ENCRYPT) {
			return symmetricEncrypt(toCrypt, key);
		}
		return symmetricDecrypt(toCrypt, key);
	}

	private String symmetricEncrypt(String text, Key secretKey) {
		String encryptedString = "";
		byte[] encryptText = text.getBytes();

		try {
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			encryptedString = Base64.encodeBase64String(cipher
					.doFinal(encryptText));

		} catch (Exception e) {
			logger.error("Error during symmetric encryption", e);
		}

		return encryptedString;
	}

	public String symmetricDecrypt(String text, Key secretKey) {
		String encryptedString = "";
		byte[] encryptText = null;

		try {
			encryptText = Base64.decodeBase64(text);
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			encryptedString = new String(cipher.doFinal(encryptText));

		} catch (Exception e) {
			logger.error("Error during symmetric decryption", e);
		}
		return encryptedString;
	}

	/**
	 * Generate cipher initialisation vector (IV) from Blob name.
	 * 
	 * IV should be unique but not necessarily secure. Since blob names are
	 * based on Type1 UUID they are unique.
	 * 
	 * @param blobName
	 * @return
	 * @throws IOException
	 */
	public static byte[] getCipherIVFromBlobName(final String blobName)
			throws IOException {
		byte[] iv;

		try {
			byte[] nameBytes = blobName.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			iv = md.digest(nameBytes);
		} catch (Exception e) {
			// should never happen
			throw new IOException(e);
		}

		return iv;
	}
}
