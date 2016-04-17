package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/**
 * Encoder for StringBuilders that locks on the ByteBufferDestination.
 */
public class LockingStringBuilderEncoder implements Encoder<StringBuilder> {

    private final Charset charset;
    private final CharsetEncoder charsetEncoder;
    private final CharBuffer cachedCharBuffer;

    public LockingStringBuilderEncoder(final Charset charset) {
        this(charset, Constants.ENCODER_CHAR_BUFFER_SIZE);
    }

    public LockingStringBuilderEncoder(final Charset charset, final int charBufferSize) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.charsetEncoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.cachedCharBuffer = CharBuffer.wrap(new char[charBufferSize]);
    }

    private CharBuffer getCharBuffer() {
        return cachedCharBuffer;
    }

    @Override
    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        synchronized (destination) {
            try {
                TextEncoderHelper.encodeText(charsetEncoder, cachedCharBuffer, destination.getByteBuffer(), source,
                        destination);
            } catch (final Exception ex) {
                logEncodeTextException(ex, source, destination);
                TextEncoderHelper.encodeTextFallBack(charset, source, destination);
            }
        }
    }

    private void logEncodeTextException(final Exception ex, final StringBuilder text,
                                        final ByteBufferDestination destination) {
        StatusLogger.getLogger().error("Recovering from LockingStringBuilderEncoder.encode('{}') error", text, ex);
    }
}
