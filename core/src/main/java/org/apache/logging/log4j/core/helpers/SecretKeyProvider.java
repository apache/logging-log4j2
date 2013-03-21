package org.apache.logging.log4j.core.helpers;

import javax.crypto.SecretKey;

/**
 *
 */
public interface SecretKeyProvider {

    SecretKey getSecretKey();
}
