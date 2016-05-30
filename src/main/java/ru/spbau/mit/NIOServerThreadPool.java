package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Сева on 29.05.2016.
 */
public class NIOServerThreadPool extends NIOServer {
    private static final Logger LOG = LogManager.getLogger(NIOServerThreadPool.class);
    private ExecutorService threadPool;

    NIOServerThreadPool(int threadCnt) {
        if (threadCnt == 0) {
            threadPool = Executors.newCachedThreadPool();
        } else {
            threadPool = Executors.newFixedThreadPool(threadCnt);
        }

    }

    @Override
    public void start(int port) throws IOException {
        super.start(port);
        threadPool.submit(() -> runServer(data -> threadPool.submit(() -> defaultProcessor(data, true))));
        LOG.info("Server started");
    }

    @Override
    public void close() throws IOException {
        LOG.info("Closing server");
        serverChannel.close();
        threadPool.shutdownNow();
    }
}
