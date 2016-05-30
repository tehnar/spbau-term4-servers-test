package ru.spbau.mit;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Сева on 23.05.2016.
 */
public class UDPServer extends Server {
    private static final Logger LOG = LogManager.getLogger(UDPServer.class);
    private DatagramSocket serverSocket;
    private ExecutorService threadPool;

    UDPServer(int threadCnt) {
        if (threadCnt == 0) {
            threadPool = Executors.newCachedThreadPool();
        }
        else {
            threadPool = Executors.newFixedThreadPool(threadCnt);
        }
    }

    @Override
    public void start(int port) throws IOException {
        serverSocket = new DatagramSocket(port);
        this.port = serverSocket.getLocalPort();
        threadPool.submit(this::runServer);
        LOG.info("Server started");
    }

    @Override
    public void close() throws IOException {

    }

    private void runServer() {
        try {
            while (!serverSocket.isClosed()) {
                byte[] buffer = new byte[Short.MAX_VALUE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                threadPool.submit(() -> process(packet));
            }
        } catch (Exception e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }

    private void process(DatagramPacket packet) {
        byte[] data = packet.getData();
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
        try {
            List<Integer> array = readArray(inputStream);
            TimeMeasure.Timer responseTimer = responseTimeMeasure.startNewTimer();
            TimeMeasure.Timer handleTimer = handleTimeMeasure.startNewTimer();
            Collections.sort(array);
            handleTimer.stop();
            byte[] sendData = prepareArray(array);

            DatagramPacket response = new DatagramPacket(sendData, sendData.length,
                    packet.getAddress(), packet.getPort());
            serverSocket.send(response);
            responseTimer.stop();
        } catch (Exception e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }
}
