package org.apache.logging.log4j.core.config.status;

import org.apache.logging.log4j.status.StatusData;

public class StatusStdOutListener extends StatusConsoleListener {

	@Override
	public void log(StatusData data) {
        if (isEnabledFor(data)) {
            System.out.println(data.getFormattedStatus());
        }
	}

}
