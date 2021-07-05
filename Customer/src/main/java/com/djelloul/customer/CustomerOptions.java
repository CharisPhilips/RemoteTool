
package com.djelloul.customer;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.djelloul.core.AbstractOptions;
import com.djelloul.core.common.Constants;

/**
 * Options of the customer application.
 *
 * @author Djelloul
 */
@SuppressWarnings("WeakerAccess")
public class CustomerOptions extends AbstractOptions {
    @SuppressWarnings("unused")
    private final static Logger LOGGER = LoggerFactory.getLogger(CustomerOptions.class);

    public CustomerOptions(File location) {
        super(location);
    }

    public String getHost() {
        String defaultValue = StringUtils.trimToEmpty(""); //settings.getString("default.host")
        return StringUtils.defaultIfBlank(getProperty("host"), defaultValue);
    }

    public void setHost(String value) {
        if (StringUtils.isBlank(value))
            remove("host");
        else
            setProperty("host", StringUtils.trimToEmpty(value));
    }

    public Integer getPort() {
        Integer defaultValue;
        try {
            defaultValue = Constants.default_localPort;
        } catch (NumberFormatException ex) {
            defaultValue = 5900;
        }
        return getPropertyAsInteger("port", defaultValue);
    }

    public void setPort(Integer value) {
        if (value == null)
            remove("port");
        else
            setProperty("port", value.toString());
    }

    public String getScreenId() {
        return StringUtils.trimToNull(getProperty("screenId"));
    }

    public void setScreenId(String value) {
        if (StringUtils.isBlank(value))
            remove("screenId");
        else
            setProperty("screenId", StringUtils.trimToEmpty(value));
    }

    public Boolean getSsl() {
        Boolean defaultValue = Constants.default_ssl;
        return getPropertyAsBoolean("ssl", defaultValue);
    }

    public boolean isSsl() {
        return Boolean.TRUE.equals(getSsl());
    }

    public void setSsl(Boolean value) {
        if (value == null)
            remove("ssl");
        else
            setProperty("ssl", value.toString());
    }
}
