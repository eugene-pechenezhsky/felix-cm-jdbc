package com.greysphere.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.greysphere.internal.cm.JdbcPersistenceManager;
import com.greysphere.internal.cm.JdbcPersistenceManager.PidFormatter;

@RunWith(MockitoJUnitRunner.class)
public class JdbcTests
{
	
	static final String TABLE_NAME = "appSettings";
	static final String PID_COLUMN = "pid";
	static final String PROP_NAME_COLUMN = "propertyName";
	static final String PROP_VALUE_COLUMN = "propertyValue";
	
	static final DataSource ds = DbHelper.createDataSource();
	static final JdbcPersistenceManager jdbcPm = new JdbcPersistenceManager(
			ds, TABLE_NAME, PID_COLUMN, PROP_NAME_COLUMN, PROP_VALUE_COLUMN);;
	
	@BeforeClass
	public static void beforeAllTests() throws Exception
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
	
	@After
	public void afterEachTest() throws Exception
	{
		try(Connection conn = ds.getConnection();
			Statement stmt = conn.createStatement())
		{
			stmt.execute("DELETE FROM ${tableName}".replace("${tableName}", TABLE_NAME));
			
			conn.commit();
		}
		
		jdbcPm.setPidFormatter(null);
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
	
	@Test
	public void canRewritePidForPersistence() throws Exception
	{
		String pid = "pid1";
		
		jdbcPm.setPidFormatter(new PidFormatter()
		{
			@Override
			public String toInternal(String pid)
			{
				return pid.replace("pid", "");
			}
			
			@Override
			public String fromInternal(String pid)
			{
				return "pid" + pid;
			}
		});
		
		jdbcPm.store(pid, new Hashtable(ImmutableMap.of("A", "1")));
		
		assertTrue(jdbcPm.exists(pid));
	}
	
	@Test
	public void canLoadDictionary() throws Exception
	{
		String pid = "pid1";
		
		jdbcPm.store(pid, new Hashtable(ImmutableMap.of("A", "1")));
		
		Dictionary<?,?> d = jdbcPm.load(pid);

		assertEquals(d.get("A"), "1");
	}
	
	@Test
	public void canRemoveDictionary() throws Exception
	{
		String pid = "pid1";
		
		jdbcPm.store(pid, new Hashtable(ImmutableMap.of("A", "1")));
		
		assertTrue(jdbcPm.exists(pid));
		
		jdbcPm.delete(pid);

		assertFalse(jdbcPm.exists(pid));
	}
}
