package ru.spbau.mit;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by Сева on 27.05.2016.
 */
public class TCPServerOneThread extends TCPSocketServer {
    private static final Logger LOG = LogManager.getLogger(TCPServerOneThread.class);
    private Thread serverThread = new Thread(this::runServer);

    @Override
    public void start(int port) throws IOException {
        super.start(port);
        serverThread.start();
        LOG.info("Server started");
    }

    @Override
    public void close() throws IOException {

    }

    private void runServer() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                LOG.info("Client connected!");
                processClient(socket);
            } catch (Exception e) {
                LOG.error(Throwables.getStackTraceAsString(e));
            }
        }
    }
}
