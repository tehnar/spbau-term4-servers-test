package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Сева on 23.05.2016.
 */
public class TCPServerThreadPool extends TCPSocketServer {
    private static final Logger LOG = LogManager.getLogger(TCPServerThreadPool.class);
    private final ExecutorService threadPool;

    TCPServerThreadPool(int threadCnt) {
        if (threadCnt == 0) {
            threadPool = Executors.newCachedThreadPool();
        } else {
            threadPool = Executors.newFixedThreadPool(threadCnt);
        }
    }

    @Override
    public void start(int port) throws IOException {
        super.start(port);
        threadPool.submit(this::runServer);
        LOG.info("Server started");
    }

    @Override
    public void close() throws IOException {
        LOG.info("Closing server");
        serverSocket.close();
        threadPool.shutdownNow();
    }

    private void runServer() {
        while (!Thread.interrupted() && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                LOG.info("Client connected!");
                threadPool.submit(() -> processClient(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
