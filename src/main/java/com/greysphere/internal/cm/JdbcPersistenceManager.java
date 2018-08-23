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
	private final String tableSettings;
	private final String columnPid;
	private final String columnProperty;
	private final String columnValue;
	private final Function<String, String> toInternalFormat = x -> x;
	private final Function<String, String> toExternalFormat = x -> x;
	
	public JdbcPersistenceManager(
			final DataSource dataSource,
			final String tableSettings,
			final String columnPid,
			final String columnProperty,
			final String columnValue/*,
			final Function<String, String> toInternalFormat,
			final Function<String, String> toExternalFormat*/)
	{
		this.dataSource = dataSource;
		this.tableSettings = tableSettings;
		this.columnPid = columnPid;
		this.columnProperty = columnProperty;
		this.columnValue = columnValue;
		//this.toInternalFormat = toInternalFormat;
		//this.toExternalFormat = toExternalFormat;
	}
	
	@Override
	public boolean exists(String pid)
	{
		pid = toInternalFormat.apply(pid);
		
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
			PreparedStatement stmt = conn.prepareStatement(
				String.format(
					"select %s, %s from %s where %s = ?", 
					columnProperty, 
					columnValue,
					tableSettings, 
					columnPid)))
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
		
		StringBuilder newDataQuery = new StringBuilder(
				"select null as ").append(columnProperty)
					.append(", ").append("null as ").append(columnValue)
					.append(", ").append("null as ").append(columnPid);
		
		Enumeration keys = properties.keys();
		
		Map<String, String> propertiesCopy = new HashMap<>();
		
		while(keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			
			//if(Constants.SERVICE_PID.equals(key)) continue;
			
			Object value = properties.get(key);
			
			newDataQuery.append("\n union all select ?, ?, ?");
			
			propertiesCopy.put(Objects.toString(key), Objects.toString(value));
		}
		
		StringBuilder mergeQuery = new StringBuilder(
			"merge ").append(tableSettings).append(" as target\n")
			.append("using (\n ").append(newDataQuery).append("\n) as source\n")
			.append("on source.").append(columnPid).append(" = target.").append(columnPid)
			.append(" and source.").append(columnProperty).append(" = target.").append(columnProperty).append("\n")
			.append("when not matched by target and source.").append(columnPid).append(" is not null then insert values (source.").append(columnPid).append(", source.").append(columnProperty).append(", source.").append(columnValue).append(")\n")
			.append("when not matched by source and target.").append(columnPid).append(" = ? then delete\n")
			.append("when matched then update set target.").append(columnValue).append(" = source.").append(columnValue).append(';')
			;
			
		int idx = 1;
		
		final String finalQuery = mergeQuery.toString();
		
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(finalQuery))
		{
			for(String key : propertiesCopy.keySet())
			{
				String value = propertiesCopy.get(key);
				
				stmt.setString(idx++, key);
				stmt.setString(idx++, value);
				stmt.setString(idx++, pid);
			}
			
			stmt.setString(idx++, pid);
			
			stmt.execute();
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
			PreparedStatement stmt = conn.prepareStatement(
				String.format(
					"delete from %s where %s = ?", 
					tableSettings, 
					columnPid)))
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
			PreparedStatement stmt = conn.prepareStatement(
				String.format(
					"select distinct %s from %s", 
					columnPid,
					tableSettings));
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
}
