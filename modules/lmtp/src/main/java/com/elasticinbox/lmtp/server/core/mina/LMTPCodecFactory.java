package com.elasticinbox.lmtp.server.core.mina;

import java.nio.charset.Charset;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;

import com.elasticinbox.lmtp.server.LMTPServerConfig;

/**
 * A {@link ProtocolCodecFactory} that performs encoding and decoding between a
 * text line data and a Java string object. This codec is useful especially when
 * you work with a text-based protocols such as LMTP and IMAP.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam aliyev
 */
public class LMTPCodecFactory implements ProtocolCodecFactory
{
	private final TextLineEncoder encoder;
	private final LMTPCodecDecoder decoder;

	/**
	 * Creates a new instance using the configuration.
	 */
	public LMTPCodecFactory(LMTPServerConfig config) {
		this(config.getCharset(), config.getDataDeferredSize());
	}

	/**
	 * Creates a new instance using the configuration.
	 */
	protected LMTPCodecFactory(Charset charset, int dataDeferredSize) {
		encoder = new TextLineEncoder(charset, LineDelimiter.CRLF);
		decoder = new LMTPCodecDecoder(charset, dataDeferredSize);
	}

	public ProtocolEncoder getEncoder(IoSession session) {
		return encoder;
	}

	public ProtocolDecoder getDecoder(IoSession session) {
		return decoder;
	}

	/**
	 * Returns the allowed maximum size of the encoded line. If the size of the
	 * encoded line exceeds this value, the encoder will throw a
	 * {@link IllegalArgumentException}. The default value is
	 * {@link Integer#MAX_VALUE}.
	 * <p>
	 * This method does the same job with
	 * {@link TextLineEncoder#getMaxLineLength()}.
	 */
	public int getEncoderMaxLineLength() {
		return encoder.getMaxLineLength();
	}

	/**
	 * Sets the allowed maximum size of the encoded line. If the size of the
	 * encoded line exceeds this value, the encoder will throw a
	 * {@link IllegalArgumentException}. The default value is
	 * {@link Integer#MAX_VALUE}.
	 * <p>
	 * This method does the same job with
	 * {@link TextLineEncoder#setMaxLineLength(int)}.
	 */
	public void setEncoderMaxLineLength(int maxLineLength) {
		encoder.setMaxLineLength(maxLineLength);
	}

	/**
	 * Returns the allowed maximum size of the line to be decoded. If the size
	 * of the line to be decoded exceeds this value, the decoder will throw a
	 * {@link BufferDataException}. The default value is <tt>1024</tt> (1KB).
	 * <p>
	 * This method does the same job with
	 * {@link TextLineDecoder#getMaxLineLength()}.
	 */
	public int getDecoderMaxLineLength() {
		return decoder.getMaxLineLength();
	}

	/**
	 * Sets the allowed maximum size of the line to be decoded. If the size of
	 * the line to be decoded exceeds this value, the decoder will throw a
	 * {@link BufferDataException}. The default value is <tt>1024</tt> (1KB).
	 * <p>
	 * This method does the same job with
	 * {@link TextLineDecoder#setMaxLineLength(int)}.
	 */
	public void setDecoderMaxLineLength(int maxLineLength) {
		decoder.setMaxLineLength(maxLineLength);
	}
}
