package players.gr9;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerUtility {
    private static final Logger logger = Logger.getLogger(LoggerUtility.class.getName());

    static {
        // Disable default console output
        logger.setUseParentHandlers(false);

        // Set the logging level:
        //DEBUG, INFO, WARNING, ERROR, CRITICAL
        //SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST
        logger.setLevel(Level.WARNING);

        //Add ConsoleHandler explicitly for logging to the console

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.WARNING);
        // Add the handler to the logger
        logger.addHandler(consoleHandler);


//        Logger rootLogger = Logger.getLogger("");
//        for (Handler h : rootLogger.getHandlers()) {
//            h.setLevel(Level.ALL);
//        }
//        rootLogger.setLevel(Level.ALL);

    }

    public static Logger getLogger() {
        return logger;
    }
}
