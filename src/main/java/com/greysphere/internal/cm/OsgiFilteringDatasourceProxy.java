package com.greysphere.internal.cm;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class OsgiFilteringDatasourceProxy implements DataSource
{
	private final BundleContext bundleContext;
	private final String serviceFilter;
	
	public OsgiFilteringDatasourceProxy(final BundleContext bundleContext, final String serviceFilter)
	{
		this.bundleContext = Objects.requireNonNull(bundleContext);
		this.serviceFilter = Objects.requireNonNull(serviceFilter);
	}
	
	private DataSource borrowDataSource()
	{
		try
		{
			for(final ServiceReference<DataSource> ref : 
				bundleContext.getServiceReferences(DataSource.class, serviceFilter))
			{
				return bundleContext.getService(ref);
			}
		}
		catch(InvalidSyntaxException e)
		{
			throw new RuntimeException(e);
		}
		
		return null;
	}

	@Override
	public PrintWriter getLogWriter()
			throws SQLException
	{
		return borrowDataSource().getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out)
			throws SQLException
	{
		borrowDataSource().setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds)
			throws SQLException
	{
		borrowDataSource().setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout()
			throws SQLException
	{
		return borrowDataSource().getLoginTimeout();
	}

	@Override
	public Logger getParentLogger()
			throws SQLFeatureNotSupportedException
	{
		return borrowDataSource().getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface)
			throws SQLException
	{
		return borrowDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface)
			throws SQLException
	{
		return borrowDataSource().isWrapperFor(iface);
	}

	@Override
	public Connection getConnection()
			throws SQLException
	{
		return borrowDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException
	{
		return borrowDataSource().getConnection(username, password);
	}

}
