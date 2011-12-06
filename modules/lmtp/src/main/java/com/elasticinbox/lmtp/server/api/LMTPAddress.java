package com.elasticinbox.lmtp.server.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Email address received in the LMTP dialog
 * 
 * @author tom
 * @author Rustam Aliyev
 */
public class LMTPAddress
{
	private boolean isValid;
	private String localPart;
	private String normalizedLocalPart;
	private String domainPart;
	private Map<String, String> parameters;
	private LMTPReply deliveryReply;
	private String emailAddress;
	private String[] allowedParameters;

	public LMTPAddress(String arg, String[] allowedParameters, String rcptDelim)
	{
		this.allowedParameters = allowedParameters;
		parameters = new HashMap<String, String>();
		isValid = parse(arg);
		deliveryReply = LMTPReply.TEMPORARY_FAILURE;

		if (!isValid) {
			return;
		}

		int delimIndex = -1;
		if (localPart != null && rcptDelim != null
				&& (delimIndex = localPart.indexOf(rcptDelim)) > 0) {
			// NB: > 0 makes sure that if the first character is the extension
			// characater we do try to remove extension.
			normalizedLocalPart = localPart.substring(0, delimIndex);
		} else {
			normalizedLocalPart = localPart;
		}

		int l1 = (localPart != null) ? localPart.length() : 0;
		int l2 = (domainPart != null) ? domainPart.length() : 0;
		StringBuilder sb = new StringBuilder(l1 + l2 + 1);
		if (normalizedLocalPart != null) {
			sb.append(normalizedLocalPart);
		}
		if (domainPart != null) {
			sb.append("@").append(domainPart);
		}
		emailAddress = sb.toString();
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getNormalizedLocalPart() {
		return normalizedLocalPart;
	}

	public boolean isValid() {
		return isValid;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public String getParameter(String key)
	{
		if (parameters.isEmpty()) {
			return null;
		}
		return (String) parameters.get(key.toUpperCase());
	}

	public LMTPReply getDeliveryStatus() {
		return deliveryReply;
	}

	public void setDeliveryStatus(LMTPReply reply) {
		deliveryReply = reply;
	}

	/**
	 * 'offset' is the *index* in the array of the next char that we have not
	 * processed (other than lookahead). Alternately, this is also the length of
	 * the part that we have already processed (think about it whichever way
	 * works for you). For instance if the content is:
	 * 
	 * <x@y.com> 012345678
	 * 
	 * After local part processing, offset should be 2. After domain parsing,
	 * offset should be 8.
	 * 
	 * Yes, the naming convention here takes a break from 'm' prefixing. It's
	 * just more readable this way.
	 */
	private int offset;
	private int length;
	private char[] array;

	private boolean eos() {
		return offset >= length;
	}

	/**
	 * Return the next unprocessed character and advance the offset.
	 */
	private int next() {
		if (offset < length) {
			return array[offset++];
		} else {
			return -1;
		}
	}

	/**
	 * Return the next unprocessed character, but do NOT advance the offset.
	 * 
	 * When checking for the presence of something optional, ie it may or may
	 * not be there, use peek()
	 */
	private int peek() {
		if (offset < length) {
			return array[offset];
		} else {
			return -1;
		}
	}

	/**
	 * Advance the offset by one.
	 * 
	 * If the optional item was there, and you noticed it via peek(), you might
	 * want to skip it.
	 */
	private void skip() {
		offset++;
	}

	private void init(String p) {
		array = p.toCharArray();
		length = array.length;
		offset = 0;
	}

	private boolean parse(String p)
	{
		int ch;
		if (p == null || p.length() < 2) {
			// atleast "<>" (length 2) required
			return false;
		}

		init(p);

		/* Skip any white space, being liberal in what we accept */
		skipSpaces();

		/* Check starts with '<' */
		ch = next();
		if (ch == -1 || ch != '<') {
			return false;
		}

		/* Strip out source routes */
		if (!skipSourceRoutes()) {
			return false;
		}

		/* Parse local part of address (the stuff before '@') */
		if (!parseLocalPart()) {
			return false;
		}

		/* Check for an optional '@' */
		ch = peek();
		if (ch == '@' && !parseDomainPart()) {
			return false;
		}

		/* Check address is finished with a '>' */
		ch = next();
		if (ch != '>') {
			return false;
		}

		/* Check if there are any parameters */
		if (eos()) {
			return true;
		}

		ch = peek();
		if (ch != ' ') {
			return false;
		}

		/* Skip any white space after the address */
		skipSpaces();

		/* Parse parameters */
		while (!eos()) {
			if (!parseParameter()) {
				return false;
			}
			skipSpaces();
		}

		return true;
	}

	private void skipSpaces() {
		while (peek() == ' ')
			skip();
	}

	/**
	 * Skip past source routes (ie, ignore them), return false if invalid source
	 * route(s) present.
	 * 
	 * Example of what is skipped from RFC 2821/Section 4.1.1.3:
	 * 
	 * RCPT TO:<@hosta.int,@jkl.org:userc@d.bar.org>
	 * _________^^^^^^^^^^^^^^^^^^^^________________
	 */
	private boolean skipSourceRoutes()
	{
		int ch = peek();
		if (ch != '@') {
			return true;
		}
		skip(); // the '@'

		while (ch == '@') {
			ch = peek();
			if (ch == '[') {
				if (!skipAddress()) {
					return false;
				}
			} else {
				if (!skipHostname()) {
					return false;
				}
			}

			ch = next();
			if (ch == ',') {
				ch = next();
				continue;
			} else if (ch == ':') {
				ch = peek();
				if (ch == '@') {
					skip(); // the '@'
					continue;
				} else {
					break;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	private boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}

	private boolean isLetter(int c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private boolean skipAddress() {
		int ch = next();
		if (ch != '[') {
			return false;
		}

		do {
			ch = next();
		} while (isDigit(ch) || ch == '.');

		if (ch != ']') {
			return false;
		}

		return true;
	}

	private boolean skipHostname() {
		while (true) {
			int ch = peek();
			if (isDigit(ch) || isLetter(ch) || ch == '.' || ch == '-') {
				skip();
				continue;
			} else {
				break;
			}
		}
		return true;
	}

	private boolean parseLocalPart() {
		int ch = peek();
		if (ch == '"') {
			return parseQuotedLocalPart();
		} else {
			return parsePlainLocalPart();
		}
	}

	private boolean parseQuotedLocalPart() {
		int soffset = offset;

		int ch = next();
		if (ch != '"') {
			return false;
		}

		while (!eos()) {
			ch = next();
			if (ch == '\\') {
				ch = next();
				if (ch == -1) {
					return false;
				}
			} else if (ch == '"') {
				localPart = new String(array, soffset, offset - soffset);
				return true;
			}
		}

		return false;
	}

	private boolean parsePlainLocalPart()
	{
		int soffset = offset;

		while (!eos()) {
			int ch = peek();
			/*
			 * <c> ::= any one of the 128 ASCII characters, but not any
			 * <special> or <SP>
			 * 
			 * <special> ::= "<" | ">" | "(" | ")" | "[" | "]" | "\" | "." | ","
			 * | ";" | ":" | "@" """ | the control characters (ASCII codes 0
			 * through 31 inclusive and 127)
			 */
			if (ch < 33 || ch > 126) { // 32 is ' '
				return false; // any one of the 128 ascii characters
			}

			if ("<()[]\\,;:\"".indexOf(ch) > -1) {
				/*
				 * Left out '>' and '@' which are terminators in this context.
				 * Also '.' is valid - there is more to the grammar than quoted
				 * above.
				 */
				return false;
			}

			if (ch == '@' || ch == '>') {
				localPart = new String(array, soffset, offset - soffset);
				return true;
			}

			skip();
		}

		/*
		 * Only happens if we abruptly reached end of string. Caller's
		 * responsibility to make sure that there is the termination character
		 * of their choice at the end.
		 */
		localPart = new String(array, soffset, offset - soffset);
		return true;
	}

	private boolean parseDomainPart() {
		int soffset;
		int ch;

		ch = next();
		if (ch != '@') {
			return false;
		}

		soffset = offset; // don't do -1 here because we want to skip the @

		ch = peek();
		if (ch == '[') {
			if (!skipAddress()) {
				return false;
			}
		} else {
			if (!skipHostname()) {
				return false;
			}
		}

		domainPart = new String(array, soffset, offset - soffset);
		return true;
	}

	private boolean parseParameter()
	{
		String key = null;
		int koffset = offset;
		while (!eos()) {
			int ch = next();
			if (ch == '=') {
				key = new String(array, koffset, offset - koffset - 1); // -1
				// for
				// the =
				if (allowedParameter(key)) {
					break;
				} else {
					return false;
				}
			}
		}
		if (key == null) {
			return false;
		}

		int voffset = offset;
		while (true) {
			int ch = peek();
			if (ch == -1 || ch == ' ') {
				break;
			}
			skip();
		}
		String value = new String(array, voffset, offset - voffset);
		parameters.put(key.toUpperCase(), value);
		return true;
	}

	private boolean allowedParameter(String key)
	{
		if (allowedParameters == null) {
			return false;
		}
		for (int i = 0; i < allowedParameters.length; i++) {
			if (key.equalsIgnoreCase(allowedParameters[i])) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return emailAddress;
	}
}
