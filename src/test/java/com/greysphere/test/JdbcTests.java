package com.greysphere.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.greysphere.internal.cm.JdbcPersistenceManager;

@RunWith(MockitoJUnitRunner.class)
public class JdbcTests
{
	JdbcPersistenceManager jdbcPm;
	
	static final String TABLE_NAME = "appSettings";
	static final String PID_COLUMN = "pid";
	static final String PROP_NAME_COLUMN = "propertyName";
	static final String PROP_VALUE_COLUMN = "propertyValue";
	
	static final DataSource ds = DbHelper.createDataSource();
	
	@BeforeClass
	public static void initTable() throws Exception
	{
		try(Connection conn = ds.getConnection();
			Statement stmt = conn.createStatement())
		{
			stmt.execute(new StringBuilder()
				.append("CREATE TABLE ${tableName}")
				.append(" (${pidCol} VARCHAR(200) NOT NULL,")
				.append(" ${propNameCol} VARCHAR(200) NOT NULL, ${propValCol} VARCHAR(200) NOT NULL,")
				.append(" PRIMARY KEY (${pidCol}, ${propNameCol}))")
				.toString()
				.replace("${tableName}", TABLE_NAME)
				.replace("${pidCol}", PID_COLUMN)
				.replace("${propNameCol}", PROP_NAME_COLUMN)
				.replace("${propValCol}", PROP_VALUE_COLUMN)
			);
			
            conn.commit();
		}
	}
	
	@Before
	public void init() throws Exception
	{
		jdbcPm = new JdbcPersistenceManager(ds, TABLE_NAME, PID_COLUMN, PROP_NAME_COLUMN, PROP_VALUE_COLUMN);
	}
	
	@After
	public void cleanup() throws Exception
	{
		try(Connection conn = ds.getConnection();
			Statement stmt = conn.createStatement())
		{
			stmt.execute("DELETE FROM ${tableName}".replace("${tableName}", TABLE_NAME));
			
			conn.commit();
		}
	}
	
	@Test
	public void canGetDictionaries() throws Exception
	{
		Enumeration e = jdbcPm.getDictionaries();
		
		assertFalse(e.hasMoreElements());
	}
	
	@Test
	public void canStoreDictionary() throws Exception
	{
		String pid = "pid1";
		
		assertFalse(jdbcPm.exists(pid));
		
		jdbcPm.store(pid, new Hashtable(ImmutableMap.of("A", "1")));
		
		assertTrue(jdbcPm.exists(pid));
	}
}