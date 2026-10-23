package com.carbon.studio.storage;

import java.io.IOException;

/** A readable, user-facing CARBON archive or manifest validation failure. */
public class CarbonFormatException extends IOException {
    private static final long serialVersionUID=1L;
    public CarbonFormatException(String message){super(message);}
    public CarbonFormatException(String message,Throwable cause){super(message,cause);}
}
