package org.apache.logging.log4j.core.config.status;

import org.apache.logging.log4j.status.StatusData;

public class StatusStdErrListener extends StatusConsoleListener {

	@Override
	public void log(StatusData data) {
        if (isEnabledFor(data)) {
            System.err.println(data.getFormattedStatus());
        }
	}

}
