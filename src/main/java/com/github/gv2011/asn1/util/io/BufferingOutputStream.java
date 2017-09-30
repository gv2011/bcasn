package com.github.gv2011.asn1.util.io;

import static com.github.gv2011.util.ex.Exceptions.run;

import java.io.OutputStream;

import com.github.gv2011.asn1.util.Arrays;

/**
 * An output stream that buffers data to be feed into an encapsulated output stream.
 * <p>
 * The stream zeroes out the internal buffer on each flush.
 * </p>
 */
public class BufferingOutputStream
    extends OutputStream
{
    private final OutputStream other;
    private final byte[] buf;

    private int   bufOff;

    /**
     * Create a buffering stream with the default buffer size (4096).
     *
     * @param other output stream to be wrapped.
     */
    public BufferingOutputStream(final OutputStream other)
    {
        this.other = other;
        buf = new byte[4096];
    }

    /**
     * Create a buffering stream with a specified buffer size.
     *
     * @param other output stream to be wrapped.
     * @param bufferSize size in bytes for internal buffer.
     */
    public BufferingOutputStream(final OutputStream other, final int bufferSize)
    {
        this.other = other;
        buf = new byte[bufferSize];
    }

    @Override
    public void write(final byte[] bytes, int offset, int len){
        if (len < buf.length - bufOff)
        {
            System.arraycopy(bytes, offset, buf, bufOff, len);
            bufOff += len;
        }
        else
        {
            final int gap = buf.length - bufOff;

            System.arraycopy(bytes, offset, buf, bufOff, gap);
            bufOff += gap;

            flush();

            offset += gap;
            len -= gap;
            while (len >= buf.length)
            {
                final int off = offset;
                run(()->other.write(bytes, off, buf.length));
                offset += buf.length;
                len -= buf.length;
            }

            if (len > 0)
            {
                System.arraycopy(bytes, offset, buf, bufOff, len);
                bufOff += len;
            }
        }
    }

    @Override
    public void write(final int b){
        buf[bufOff++] = (byte)b;
        if (bufOff == buf.length)
        {
            flush();
        }
    }

    /**
     * Flush the internal buffer to the encapsulated output stream. Zero the buffer contents when done.
     *
     * @throws IOException on error.
     */
    @Override
    public void flush(){
        run(()->other.write(buf, 0, bufOff));
        bufOff = 0;
        Arrays.fill(buf, (byte)0);
    }

    @Override
    public void close(){
        try{flush();}
        finally{run(other::close);}
    }
}
