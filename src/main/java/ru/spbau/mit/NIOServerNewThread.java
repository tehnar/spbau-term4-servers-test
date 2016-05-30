package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by Сева on 29.05.2016.
 */
public class NIOServerNewThread extends NIOServer {
    private static final Logger LOG = LogManager.getLogger(NIOServerNewThread.class);
    private Thread serverThread = new Thread(() -> runServer(data -> new Thread(() ->
            defaultProcessor(data, true)).start()));

    @Override
    public void start(int port) throws IOException {
        super.start(port);
        serverThread.start();
        LOG.info("Server started");
    }

    @Override
    public void close() throws IOException {
        LOG.info("Closing server");
        serverChannel.close();
        serverThread.interrupt();
    }

}
