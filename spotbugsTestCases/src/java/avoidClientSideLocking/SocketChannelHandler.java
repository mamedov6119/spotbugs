package avoidClientSideLocking;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class SocketChannelHandler {
    private final SocketChannel channel;
    private final ByteBuffer inboundBuffer;
    private final ByteBuffer outboundBuffer;
    private final AtomicBoolean shutdown;

    private final Object inboundBufferLock;
    private final Object outboundBufferLock;

    public SocketChannelHandler(SocketChannel channel) {
        this.channel = channel;
        this.inboundBuffer = ByteBuffer.allocate(1024);
        this.outboundBuffer = ByteBuffer.allocate(1024);
        this.inboundBufferLock = new Object();
        this.outboundBufferLock = new Object();
        this.shutdown = new AtomicBoolean(false);
    }

    public void send(ByteBuffer message) {
        synchronized (outboundBufferLock) {
            outboundBuffer.put(message);
            flush();
        }
    }

    public ByteBuffer receive() {
        synchronized (inboundBufferLock) {
            return ByteBuffer.allocate(0); 
        }
    }

    public boolean read() {
        synchronized (inboundBufferLock) {
            try {
                return processInboundData();
            } catch (Exception e) {
                shutdown();
                throw new RuntimeException("Unexpected error", e);
            }
        }
    }

    public void close() {
        synchronized (inboundBufferLock) {
            synchronized (outboundBufferLock) {
                shutdown();
            }
        }
    }

    private boolean processInboundData() throws IOException {
        synchronized (inboundBufferLock) {
            int readLast;
            while ((readLast = channel.read(inboundBuffer)) > 0) ;
            if (readLast == -1) {
                throw new IOException("EOF reached");
            }
            return readLast > 0;
        }
    }

    private void flush() {
        synchronized (outboundBufferLock) {
            outboundBuffer.flip();
            try {
                while (outboundBuffer.hasRemaining()) {
                    channel.write(outboundBuffer);
                }
                outboundBuffer.compact();
            } catch (IOException e) {
                shutdown();
                throw new RuntimeException("Unexpected I/O error", e);
            }
        }
    }

    private void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            try {
                closeChannel();
                releaseBuffers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void closeChannel() throws IOException {
        channel.close();
    }

    private void releaseBuffers() {
        synchronized (inboundBufferLock) {
            
        }
        synchronized (outboundBufferLock) {
        }
    }


    public boolean isClosed() {
        return shutdown.get();
    }
}

