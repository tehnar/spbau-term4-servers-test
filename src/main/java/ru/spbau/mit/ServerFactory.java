package ru.spbau.mit;

/**
 * Created by Сева on 23.05.2016.
 */
public final class ServerFactory {

    public static final String FIXED_POOL_BLOCKING = "Blocking TCP server with fixed thread pool";
    public static final String CACHED_POOL_BLOCKING = "Blocking TCP server with cached thread pool";
    public static final String FIXED_POOL_NIO = "NIO TCP server with fixed thread pool";
    public static final String CACHED_POOL_NIO = "NIO TCP server with cached thread pool";
    public static final String ONE_THREAD_NIO = "NIO TCP one thread server";
    public static final String NEW_THREAD_NIO = "NIO TCP server with new thread per connection";
    public static final String BLOCKING_ONE_THREAD = "One thread blocking server";
    public static final String UDP_FIXED_POOL_BLOCKING = "Blocking UDP server with fixed thread pool";
    public static final String UDP_POOL_BLOCKING = "Blocking UDP server with thread pool";
    public static final String[] SERVER_TYPES = {FIXED_POOL_BLOCKING, CACHED_POOL_BLOCKING,
            FIXED_POOL_NIO, CACHED_POOL_NIO, ONE_THREAD_NIO, NEW_THREAD_NIO,
            BLOCKING_ONE_THREAD, UDP_FIXED_POOL_BLOCKING, UDP_POOL_BLOCKING};

    private static final int THREAD_CNT = 8;

    private ServerFactory() {
    }

    public static Server createServer(String name) {
        switch (name) {
            case FIXED_POOL_BLOCKING:
                return new TCPServerThreadPool(THREAD_CNT);
            case CACHED_POOL_BLOCKING:
                return new TCPServerThreadPool(0);
            case FIXED_POOL_NIO:
                return new NIOServerThreadPool(THREAD_CNT);
            case CACHED_POOL_NIO:
                return new NIOServerThreadPool(0);
            case ONE_THREAD_NIO:
                return new NIOServerOneThread();
            case NEW_THREAD_NIO:
                return new NIOServerNewThread();
            case UDP_FIXED_POOL_BLOCKING:
                return new UDPServer(THREAD_CNT);
            case UDP_POOL_BLOCKING:
                return new UDPServer(0);
            case BLOCKING_ONE_THREAD:
                return new TCPServerOneThread();
            default:
                throw new IllegalArgumentException("Unknown server name: " + name);
        }
    }
}
