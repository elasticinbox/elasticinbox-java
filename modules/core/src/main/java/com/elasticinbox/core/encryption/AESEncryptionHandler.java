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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import com.elasticinbox.core.model.Message;

/**
 * This class provides AES encryption/decryption methods.
 * 
 * @author Rustam Aliyev
 */
public class AESEncryptionHandler implements EncryptionHandler {
	/**
	 * Use AES-CBC algorithm with PKCS5 padding
	 */
	public static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

	public InputStream encrypt(InputStream in, Key key, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

		return new CipherInputStream(in, cipher);
	}

	public InputStream decrypt(InputStream in, Key key, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

		return new CipherInputStream(in, cipher);
	}

	/*
	 * encrypt a message object
	 */
	public Message encryptMessage(Message message, Key key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		
		//message.setEncryptionKey(key.);
		
		byte[] encrypted = new byte[cipher.getOutputSize(message.getPlainBody().length())];
		int enc_len = cipher.update(message.getPlainBody().getBytes(), 0, message.getPlainBody().length(), encrypted, 0);
		
		enc_len += cipher.doFinal(encrypted, enc_len);
		message.setPlainBody( new String(encrypted) );
		
		return message;

	}
	

	/*
	 * decrypt a message object
	 */
	public Message decryptMessage(Message message,
			Key key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, ShortBufferException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		
		//message.setEncryptionKey(key.);
		
		byte[] decrypted = new byte[cipher.getOutputSize(message.getPlainBody().length())];
		int enc_len = cipher.update(message.getPlainBody().getBytes(), 0, message.getPlainBody().length(), decrypted, 0);
		
		enc_len += cipher.doFinal(decrypted, enc_len);
		message.setPlainBody( new String(decrypted) );
		
		return message;
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
