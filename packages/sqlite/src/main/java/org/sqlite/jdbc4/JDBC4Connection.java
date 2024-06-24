package org.sqlite.jdbc4;

import org.sqlite.jdbc3.JDBC3Connection;

import java.sql.*;
import java.util.Properties;

public class JDBC4Connection extends JDBC3Connection {

    public JDBC4Connection(String url, String fileName, Properties prop) throws SQLException {
        super(url, fileName, prop);
    }

    public Statement createStatement(int rst, int rsc, int rsh) throws SQLException {
        checkOpen();
        checkCursor(rst, rsc, rsh);

        return new JDBC4Statement(this);
    }

    public PreparedStatement prepareStatement(String sql, int rst, int rsc, int rsh)
            throws SQLException {
        checkOpen();
        checkCursor(rst, rsc, rsh);

        return new JDBC4PreparedStatement(this, sql);
    }

    // JDBC 4
    /** @see java.sql.Connection#isClosed() */
    public boolean isClosed() throws SQLException {
        return super.isClosed();
    }

    public <T> T unwrap(Class<T> iface) throws ClassCastException {
        // caller should invoke isWrapperFor prior to unwrap
        return iface.cast(this);
    }

    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    public Clob createClob() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public Blob createBlob() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public NClob createNClob() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public SQLXML createSQLXML() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isValid(int timeout) throws SQLException {
        if (isClosed()) {
            return false;
        }
        Statement statement = createStatement();
        try {
            return statement.execute("select 1");
        } finally {
            statement.close();
        }
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    public String getClientInfo(String name) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Properties getClientInfo() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
}
