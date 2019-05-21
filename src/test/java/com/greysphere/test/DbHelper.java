package com.greysphere.test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.sql.DataSource;

import org.mockito.Mockito;

public class DbHelper
{
	static DataSource createDataSource()
	{
		DataSource ds = Mockito.mock(DataSource.class);
		
		try
		{
			Class.forName("org.hsqldb.jdbc.JDBCDriver");
			Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testing");
			
			when(ds.getConnection()).thenReturn(conn);
			when(ds.getConnection(anyString(), anyString())).thenReturn(conn);
		}
		catch(Exception e)
		{
			throw new RuntimeException("failed to initialize datasource", e);
		}
		
		return ds;
	}
}
