package com.greysphere.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DbHelper
{
	static DataSource createDataSource()
	{
		DataSource ds = Mockito.mock(DataSource.class);
		
		try
		{
			Class.forName("org.hsqldb.jdbc.JDBCDriver");
			Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testing");

			Connection connWrapped = mock(Connection.class, new Answer<Object>() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					
					if("close".equalsIgnoreCase(invocation.getMethod().getName())) return null;
					
					return invocation.getMethod().invoke(conn, invocation.getArguments());
				}
			});
			
			when(ds.getConnection()).thenReturn(connWrapped);
		}
		catch(Exception e)
		{
			throw new RuntimeException("failed to initialize datasource", e);
		}
		
		return ds;
	}
}
