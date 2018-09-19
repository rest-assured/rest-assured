package io.restassured.filter.log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * @author Olga Maciaszek-Sharma
 */
public class UrlDecoder {

	private UrlDecoder() {
	}

	// Copy of  the private method in URLEncodedUtils

	/**
	 * Decode/unescape a portion of a URL, to use with the query part ensure {@code plusAsBlank} is true.
	 *
	 * @param content     the portion to decode
	 * @param charset     the charset to use
	 * @param plusAsBlank if {@code true}, then convert '+' to space (e.g. for www-url-form-encoded content), otherwise leave as is.
	 * @return encoded string
	 */
	public static String urlDecode(final String content, final Charset charset, final boolean plusAsBlank) {
		if (content == null) {
			return null;
		}
		final ByteBuffer bb = ByteBuffer.allocate(content.length());
		final CharBuffer cb = CharBuffer.wrap(content);
		while (cb.hasRemaining()) {
			final char c = cb.get();
			if (c == '%' && cb.remaining() >= 2) {
				final char uc = cb.get();
				final char lc = cb.get();
				final int u = Character.digit(uc, 16);
				final int l = Character.digit(lc, 16);
				if (u != -1 && l != -1) {
					bb.put((byte) ((u << 4) + l));
				} else {
					bb.put((byte) '%');
					bb.put((byte) uc);
					bb.put((byte) lc);
				}
			} else if (plusAsBlank && c == '+') {
				bb.put((byte) ' ');
			} else {
				bb.put((byte) c);
			}
		}
		bb.flip();
		return charset.decode(bb).toString();
	}

}
