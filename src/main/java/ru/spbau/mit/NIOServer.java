package ru.spbau.mit;

import com.google.common.base.Throwables;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Created by Сева on 24.05.2016.
 */
public abstract class NIOServer extends Server {
    private static final Logger LOG = LogManager.getLogger(NIOServer.class);
    private static final int INT_SIZE = 4;

    protected ServerSocketChannel serverChannel;
    protected Selector selector;

    @Override
    public void start(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        this.port = ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();

    }

    protected void runServer(Consumer<QueryData> taskProcessor) {
        try {
            selector = Selector.open();
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (serverChannel.isOpen()) {
                selector.select();
                for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isAcceptable()) {
                        accept();
                    }
                    if (selectionKey.isReadable()) {
                        read(selectionKey, taskProcessor);
                    }
                    if (selectionKey.isWritable()) {
                        write(selectionKey);
                    }
                }
            }
        } catch (ClosedByInterruptException ignored) {

        } catch (Exception e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }

    protected void defaultProcessor(QueryData data, boolean doLock) {
        TimeMeasure.Timer handleTimer = handleTimeMeasure.startNewTimer();
        Protocol.Array array;
        try {
            array = Protocol.Array.parseFrom(data.array.array());
        } catch (InvalidProtocolBufferException e) {
            LOG.error(Throwables.getStackTraceAsString(e));
            return;
        }
        List<Integer> sortedList = new ArrayList<>(array.getValueList());
        Collections.sort(sortedList);
        Protocol.Array response = Protocol.Array.newBuilder().addAllValue(sortedList).build();
        handleTimer.stop();
        if (doLock) {
            data.lock.lock();
        }
        data.arraySize.clear();
        data.arraySize.putInt(response.getSerializedSize());
        data.array.clear();
        data.array.put(response.toByteArray());
        data.arraySize.flip();
        data.array.flip();
        data.readyWrite = true;
        if (doLock) {
            data.lock.unlock();
        }
    }

    private void accept() throws IOException {
        LOG.info("Client connected");
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new QueryData());
    }

    private void read(SelectionKey selectionKey, Consumer<QueryData> taskProcessor) throws IOException {
        QueryData data = (QueryData) selectionKey.attachment();
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (data.readyWrite) {
            return;
        }

        if (!data.lock.tryLock()) {
            return;
        }
        try {
            if (data.arraySize.hasRemaining()) {
                channel.read(data.arraySize);
                if (!data.arraySize.hasRemaining()) {
                    data.arraySize.flip();
                    data.array = ByteBuffer.allocate(data.arraySize.getInt());
                }
            }
            if (data.array != null && data.array.hasRemaining()) {
                channel.read(data.array);
                if (!data.array.hasRemaining()) {
                    data.array.flip();
                    data.responseTimer = responseTimeMeasure.startNewTimer();
                    taskProcessor.accept(data);
                }
            }
        } finally {
            data.lock.unlock();
        }
    }

    private void write(SelectionKey selectionKey) throws IOException {
        QueryData data = (QueryData) selectionKey.attachment();
        if (!data.readyWrite) {
            return;
        }
        if (!data.lock.tryLock()) {
            return;
        }
        try {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            if (data.arraySize.hasRemaining()) {
                channel.write(data.arraySize);
            } else if (data.array.hasRemaining()) {
                channel.write(data.array);
                if (!data.array.hasRemaining()) {
                    data.responseTimer.stop();
                    data.array = null;
                    data.arraySize.flip();
                    data.readyWrite = false;
                }
            }
        } finally {
            data.lock.unlock();
        }
    }

    protected class QueryData {
        final ByteBuffer arraySize = ByteBuffer.allocate(INT_SIZE);
        Lock lock = new ReentrantLock();
        boolean readyWrite = false;
        ByteBuffer array = null;
        TimeMeasure.Timer responseTimer;
    }
}
