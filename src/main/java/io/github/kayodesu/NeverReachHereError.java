package io.github.kayodesu;

/**
 * @author Yo Ka
 */
public class NeverReachHereError extends Error {

    public NeverReachHereError() {
        super();
    }

    public NeverReachHereError(String message) {
        super(message);
    }
}
