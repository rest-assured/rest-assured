package com.jayway.restassured.authentication;

import java.security.KeyStore;

/**
 * Date: 22/04/2013
 * Time: 13:55
 */
public interface KeystoreProvider {
    /**
     * @return If this KeystoreSpec can build a keystore
     */
    Boolean canBuild();

    /**
     * @return the Keystore represented by this keystore spec
     */
    KeyStore build();
}
