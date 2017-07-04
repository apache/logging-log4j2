import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner {
    private static final Logger logger = LogManager.getLogger(Runner.class.getName());

    public static void main(String[] args) {
        System.out.println("@@@ Start @@@");

        // "properties" node in resources/log4j2.json config file is the last node (not regular json nodes order)
        // due to this, log4j2 framework doesn't act normally
        // this log msg is not printed correctly - "${layout}" is printed.
        // if we set "properties" node to be before "appenders" node, everything will work as expected.
        // why would json-node order matter? the nodes order in a JSON shouldn't matter ...
        logger.log(Level.INFO, "*** My Testing Msg 1");

        System.out.println("@@@ Stop @@@");
    }
}
