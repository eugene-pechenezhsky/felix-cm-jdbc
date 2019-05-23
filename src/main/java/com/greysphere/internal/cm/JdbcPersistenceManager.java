package com.greysphere.internal.cm;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Function;

import javax.sql.DataSource;

import org.apache.felix.cm.PersistenceManager;

@SuppressWarnings("rawtypes")
public class JdbcPersistenceManager	implements PersistenceManager
{
	private final DataSource dataSource;
	private final String columnPid;
	private final String columnProperty;
	private final String columnValue;
	private final String deleteQuery;
	private final String insertQuery;
	private final String selectQuery;
	private final String selectPidsQuery; 
	private Function<String, String> toInternalFormat = x -> x;
	private Function<String, String> toExternalFormat = x -> x;
	
	public JdbcPersistenceManager(
			final DataSource dataSource,
			final String tableSettings,
			final String columnPid,
			final String columnProperty,
			final String columnValue)
	{
		this.dataSource = dataSource;
		this.columnPid = columnPid;
		this.columnProperty = columnProperty;
		this.columnValue = columnValue;
		
		deleteQuery = String.format(
				"delete from %s where %s = ?", 
				tableSettings, columnPid);
		
		insertQuery = String.format(
				"insert into %s (%s, %s, %s) values (?, ?, ?)", 
				tableSettings, columnPid, columnProperty, columnValue);
		
		selectQuery = String.format(
				"select %s, %s from %s where %s = ?", 
				columnProperty, columnValue, tableSettings, columnPid);
		
		selectPidsQuery = String.format(
				"select distinct %s from %s", 
				columnPid,
				tableSettings);
	}
	
	@Override
	public boolean exists(String pid)
	{
		Set<String> allPids;
		
		try
		{
			allPids = queryPidNames();
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}
		
		return allPids.stream().anyMatch(pid::equalsIgnoreCase);
	}

	@Override
	public Dictionary load(String pid) throws IOException
	{
		pid = toInternalFormat.apply(pid);
		
		final Dictionary<String,Object> results = new Hashtable<>();
		
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(selectQuery))
		{
			stmt.setString(1, pid);
			
			try(ResultSet rs = stmt.executeQuery())
			{
				while(rs.next())
				{
					final String property = rs.getString(columnProperty);
					final Object value = rs.getObject(columnValue);
					
					results.put(property, value);
				}
			}
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
		
		//results.put(Constants.SERVICE_PID, toExternalFormat.apply(pid));
		
		return results;
	}
	
	@Override
	public Enumeration getDictionaries() throws IOException
	{
		Set<String> allPids;
		
		try
		{
			allPids = queryPidNames();
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
		
		final Enumeration pids = new StringTokenizer(String.join("\n", allPids), "\n");
		
		return new Enumeration()
		{
			
			@Override
			public boolean hasMoreElements()
			{
				return pids.hasMoreElements();
			}

			@Override
			public Object nextElement()
			{
				try
				{
					return load((String) pids.nextElement());
				}
				catch(final IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		};
	}

	@Override
	public void store(String pid, Dictionary properties) throws IOException
	{
		pid = toInternalFormat.apply(pid);
		
		Enumeration keys = properties.keys();
		
		Map<String, String> propertiesCopy = new HashMap<>();
		
		while(keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			
			//if(Constants.SERVICE_PID.equals(key)) continue;
			
			Object value = properties.get(key);
			
			propertiesCopy.put(Objects.toString(key), Objects.toString(value));
		}
			
		try(Connection conn = dataSource.getConnection())
		{
			boolean originallyAutoCommit = conn.getAutoCommit();
			
			if(originallyAutoCommit) conn.setAutoCommit(false);
			
			try
			{
				try(PreparedStatement stmt = conn.prepareStatement(deleteQuery))
				{
					stmt.setString(1, pid);
					stmt.executeUpdate();
				}
				
				try(PreparedStatement stmt = conn.prepareStatement(insertQuery))
				{
					for(String key : propertiesCopy.keySet())
					{
						int idx = 1;
						String value = propertiesCopy.get(key);
						
						stmt.setString(idx++, pid);
						stmt.setString(idx++, key);
						stmt.setString(idx++, value);
						stmt.addBatch();
					}
					
					stmt.executeBatch();
				}
				
				conn.commit();
			}
			finally
			{
				if(originallyAutoCommit) conn.setAutoCommit(true);
			}
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	public void delete(String pid) throws IOException
	{
		pid = toInternalFormat.apply(pid);
		
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(deleteQuery))
		{
			stmt.setString(1, pid);
			stmt.executeUpdate();
		}
		catch(SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	private Set<String> queryPidNames() throws SQLException
	{
		final Set<String> results = new TreeSet<>();
		
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(selectPidsQuery);
			ResultSet rs = stmt.executeQuery())
		{
			while(rs.next())
			{
				String rawPid = rs.getString(columnPid); 
				results.add(toExternalFormat.apply(rawPid));
			}
		}
		
		return results;
	}
	
	public Function<String, String> getToInternalFormat()
	{
		return toInternalFormat;
	}

	public void setToInternalFormat(Function<String, String> toInternalFormat)
	{
		this.toInternalFormat = Objects
				.requireNonNull(toInternalFormat)
				.andThen(Objects::requireNonNull);
	}

	public Function<String, String> getToExternalFormat()
	{
		return toExternalFormat;
	}

	public void setToExternalFormat(Function<String, String> toExternalFormat)
	{
		this.toExternalFormat = Objects
				.requireNonNull(toExternalFormat)
				.andThen(Objects::requireNonNull);
	}
}
