package com.elasticinbox.lmtp.server.core.mina;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.utils.SharedByteArrayInputStream;
import com.elasticinbox.lmtp.utils.SharedTmpFileInputStream;

/**
 * The LMTP protocol decoder context is used when a client 
 * command is split among multiple network packets.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class LMTPDecoderContext 
{
	private final static Logger logger = LoggerFactory
			.getLogger(LMTPDecoderContext.class);

	private final CharsetDecoder charsetDecoder;
	private IoBuffer buf;
	private int matchCount = 0;
	private int overflowPosition = 0;
	private boolean thresholdReached = false;
	private boolean dataMode = false;
	private long dataSize = 0L; 		// current size of the DATA input

	private File outFile;				// if we switch to file output, this is the file
	private FileOutputStream stream; 	// ... and this is the stream to write to the file
	private LMTPCodecDecoder decoder;

	protected LMTPDecoderContext(LMTPCodecDecoder decoder)
	{
		this.decoder = decoder;
		charsetDecoder = decoder.getCharset().newDecoder();
		buf = IoBuffer.allocate(decoder.getMaxLineLength()).setAutoExpand(true);
	}

	/** */
	private static byte[] asArray(IoBuffer b)
	{
		int len = b.remaining();
		byte[] array = new byte[len];
		b.get(array, 0, len);
		return array;
	}

	/** */
	protected CharsetDecoder getDecoder() {
		return charsetDecoder;
	}

	/** */
	protected IoBuffer getBuffer() {
		return buf;
	}

	/** */
	private void compactBuffer()
	{
		if (dataMode && buf.capacity() > decoder.getMaxLineLength()) {
			buf = IoBuffer.allocate(decoder.getMaxLineLength()).setAutoExpand(true);
		} else {
			buf.clear();
		}
	}

	/** */
	protected int getOverflowPosition() {
		return overflowPosition;
	}

	/** */
	protected int getMatchCount() {
		return matchCount;
	}

	/** */
	protected void setMatchCount(int matchCount) {
		this.matchCount = matchCount;
	}

	/** */
	protected void reset() throws IOException
	{
		overflowPosition = 0;
		matchCount = 0;
		dataSize = 0L;
		charsetDecoder.reset();

		if (thresholdReached) {
			thresholdReached = false;
			compactBuffer();
			closeOutputStream();
		}
	}

	/** */
	protected void write(IoBuffer b) throws IOException
	{
		if (dataMode) {
			write(asArray(b));
		} else {
			append(b);
		}
    }
    
    /** */
	private void write(byte[] src) throws IOException
	{
		int predicted = this.thresholdReached ? 0 : this.buf.position() + src.length;
		
		// Checks whether reading count bytes would cross the limit.
		if (this.thresholdReached || predicted > decoder.getThreshold()) {
			// If previously hit, then use the stream.
			if (!this.thresholdReached) {
				thresholdReached();
			}
			this.stream.write(src);
		} else {
			this.buf.put(src);
		}

		this.dataSize += src.length;
	}

	/**
	 * Called when the threshold is about to be exceeded. Once called, it
	 * won't be called again for the current data transfer.
	 */
	private void thresholdReached() throws IOException
	{
		this.outFile = File.createTempFile(LMTPCodecDecoder.TMPFILE_PREFIX,
				LMTPCodecDecoder.TMPFILE_SUFFIX);

		logger.debug("Writing message to file: {}", outFile.getAbsolutePath());

		this.stream = new FileOutputStream(this.outFile);
		this.buf.flip();
		this.stream.write(asArray(this.buf));
		this.thresholdReached = true;
		this.buf.clear();

		logger.debug("ByteBuffer written to stream");
	}

	/** */
	protected void closeOutputStream() throws IOException
	{
		if (this.stream != null) {
			this.stream.flush();
			this.stream.close();
			logger.debug("Temp file writing achieved - closing stream");
		}
	}

	/** */
	protected InputStream getInputStream() throws IOException
	{
		if (this.thresholdReached) {
			return new SharedTmpFileInputStream(this.outFile);
		} else {
			return new SharedByteArrayInputStream(asArray(this.buf));
		}
	}

	/** */
	private void append(IoBuffer in) throws CharacterCodingException 
	{
		if (overflowPosition != 0) {
			discard(in);
		} else {
			int pos = buf.position();
			if ((pos + in.remaining()) > decoder.getMaxLineLength()) {
				overflowPosition = pos;
				buf.clear();
				discard(in);
			} else {
				this.buf.put(in);
			}
		}
    }

	/** */
	private void discard(IoBuffer in)
	{
		if (Integer.MAX_VALUE < (overflowPosition + in.remaining())) {
			overflowPosition = Integer.MAX_VALUE;
		} else {
			overflowPosition += in.remaining();
		}

		in.position(in.limit());
	}

	/** */
	public void setDataMode(boolean dataMode) {
		this.dataMode = dataMode;
	}
	
	/**
	 * Get size of the DATA input
	 * 
	 * @return size
	 */
	public long getDataSize() {
		return this.dataSize;
	}
	
}