package org.apache.commons.vfs.provider.webdav;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import java.io.IOException;
import java.io.InputStream;

/**
 * A connection manager that provides access to a single HttpConnection.  This
 * manager makes no attempt to provide exclusive access to the contained
 * HttpConnection.
 *
 * ThreadLocal connection.
 *
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author Eric Johnson
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Laura Werner
 * @since 2.0
 */
public class WebdavConnectionManager implements HttpConnectionManager
{
    /**
     * Since the same connection is about to be reused, make sure the
     * previous request was completely processed, and if not
     * consume it now.
     *
     * @param conn The connection
     */
    static void finishLastResponse(HttpConnection conn)
    {
        InputStream lastResponse = conn.getLastResponseInputStream();
        if (lastResponse != null)
        {
            conn.setLastResponseInputStream(null);
            try
            {
                lastResponse.close();
            }
            catch (IOException ioe)
            {
                //FIXME: badness - close to force reconnect.
                conn.close();
            }
        }
    }

    /**
     * The thread data
     */
    protected ThreadLocal localHttpConnection = new ThreadLocal()
    {
        protected Object initialValue()
        {
            return new Entry();
        }
    };

    /**
     * Collection of parameters associated with this connection manager.
     */
    private HttpConnectionManagerParams params = new HttpConnectionManagerParams();

    private static class Entry
    {
        /**
         * The http connection
         */
        private HttpConnection conn = null;

        /**
         * The time the connection was made idle.
         */
        private long idleStartTime = Long.MAX_VALUE;
    }

    public WebdavConnectionManager()
    {
    }

    protected HttpConnection getLocalHttpConnection()
    {
        return ((Entry) localHttpConnection.get()).conn;
    }

    protected void setLocalHttpConnection(HttpConnection conn)
    {
        ((Entry) localHttpConnection.get()).conn = conn;
    }

    protected long getIdleStartTime()
    {
        return ((Entry) localHttpConnection.get()).idleStartTime;
    }

    protected void setIdleStartTime(long idleStartTime)
    {
        ((Entry) localHttpConnection.get()).idleStartTime = idleStartTime;
    }

    /**
     * @see HttpConnectionManager#getConnection(org.apache.commons.httpclient.HostConfiguration)
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration)
    {
        return getConnection(hostConfiguration, 0);
    }

    /**
     * Gets the staleCheckingEnabled value to be set on HttpConnections that are created.
     *
     * @return <code>true</code> if stale checking will be enabled on HttpConections
     * @see HttpConnection#isStaleCheckingEnabled()
     * @deprecated Use {@link HttpConnectionManagerParams#isStaleCheckingEnabled()},
     *             {@link HttpConnectionManager#getParams()}.
     */
    public boolean isConnectionStaleCheckingEnabled()
    {
        return this.params.isStaleCheckingEnabled();
    }

    /**
     * Sets the staleCheckingEnabled value to be set on HttpConnections that are created.
     *
     * @param connectionStaleCheckingEnabled <code>true</code> if stale checking will be enabled
     *                                       on HttpConections
     * @see HttpConnection#setStaleCheckingEnabled(boolean)
     * @deprecated Use {@link HttpConnectionManagerParams#setStaleCheckingEnabled(boolean)},
     *             {@link HttpConnectionManager#getParams()}.
     */
    public void setConnectionStaleCheckingEnabled(boolean connectionStaleCheckingEnabled)
    {
        this.params.setStaleCheckingEnabled(connectionStaleCheckingEnabled);
    }

    /**
     * @see HttpConnectionManager#getConnectionWithTimeout(HostConfiguration, long)
     * @since 3.0
     */
    public HttpConnection getConnectionWithTimeout(
        HostConfiguration hostConfiguration, long timeout)
    {

        HttpConnection httpConnection = getLocalHttpConnection();
        if (httpConnection == null)
        {
            httpConnection = new HttpConnection(hostConfiguration);
            setLocalHttpConnection(httpConnection);
            httpConnection.setHttpConnectionManager(this);
            httpConnection.getParams().setDefaults(this.params);
        }
        else
        {

            // make sure the host and proxy are correct for this connection
            // close it and set the values if they are not
            if (!hostConfiguration.hostEquals(httpConnection)
                || !hostConfiguration.proxyEquals(httpConnection))
            {

                if (httpConnection.isOpen())
                {
                    httpConnection.close();
                }

                httpConnection.setHost(hostConfiguration.getHost());
                httpConnection.setPort(hostConfiguration.getPort());
                httpConnection.setProtocol(hostConfiguration.getProtocol());
                httpConnection.setLocalAddress(hostConfiguration.getLocalAddress());

                httpConnection.setProxyHost(hostConfiguration.getProxyHost());
                httpConnection.setProxyPort(hostConfiguration.getProxyPort());
            }
            else
            {
                finishLastResponse(httpConnection);
            }
        }

        // remove the connection from the timeout handler
        setIdleStartTime(Long.MAX_VALUE);

        return httpConnection;
    }

    /**
     * @see HttpConnectionManager#getConnection(HostConfiguration, long)
     * @deprecated Use #getConnectionWithTimeout(HostConfiguration, long)
     */
    public HttpConnection getConnection(
        HostConfiguration hostConfiguration, long timeout)
    {
        return getConnectionWithTimeout(hostConfiguration, timeout);
    }

    /**
     * @see HttpConnectionManager#releaseConnection(org.apache.commons.httpclient.HttpConnection)
     */
    public void releaseConnection(HttpConnection conn)
    {
        if (conn != getLocalHttpConnection())
        {
            throw new IllegalStateException("Unexpected release of an unknown connection.");
        }

        finishLastResponse(getLocalHttpConnection());

        // track the time the connection was made idle
        setIdleStartTime(System.currentTimeMillis());
    }

    /**
     * Returns {@link HttpConnectionManagerParams parameters} associated
     * with this connection manager.
     *
     * @see HttpConnectionManagerParams
     * @since 2.1
     */
    public HttpConnectionManagerParams getParams()
    {
        return this.params;
    }

    /**
     * Assigns {@link HttpConnectionManagerParams parameters} for this
     * connection manager.
     *
     * @see HttpConnectionManagerParams
     * @since 2.1
     */
    public void setParams(final HttpConnectionManagerParams params)
    {
        if (params == null)
        {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }

    /**
     * @since 3.0
     */
    public void closeIdleConnections(long idleTimeout)
    {
        long maxIdleTime = System.currentTimeMillis() - idleTimeout;
        if (getIdleStartTime() <= maxIdleTime)
        {
            getLocalHttpConnection().close();
        }
    }
}