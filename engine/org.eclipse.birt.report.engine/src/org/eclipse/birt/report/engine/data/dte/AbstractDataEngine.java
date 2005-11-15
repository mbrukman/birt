/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.data.dte;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.archive.IDocumentArchive;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.ISubqueryDefinition;
import org.eclipse.birt.report.engine.adapter.ModelDteApiAdapter;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.data.IDataEngine;
import org.eclipse.birt.report.engine.data.IResultSet;
import org.eclipse.birt.report.engine.executor.ExecutionContext;
import org.eclipse.birt.report.engine.i18n.MessageConstants;
import org.eclipse.birt.report.engine.ir.Report;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DataSourceHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.mozilla.javascript.JavaScriptException;

public abstract class AbstractDataEngine implements IDataEngine
{

	protected DataEngine dataEngine;
	protected ExecutionContext context;
	//protected DataEngineContext dteContext;
	protected HashMap queryMap = new HashMap( );
	protected HashMap queryIDMap = new HashMap();

	protected HashMap mapIDtoQuery = new HashMap( );

	protected ArrayList queryResultRelations = new ArrayList( );

	protected LinkedList queryResultStack = new LinkedList( );

	protected IDocumentArchive archive = null;
	protected String reportArchName = null;

	// protected ArrayList beforeExpressionIDs = new ArrayList();
	// protected ArrayList afterExpressionIDs = new ArrayList();
	// protected ArrayList rowExpressionIDs = new ArrayList();
	// protected LinkedList groupIDList = new LinkedList();
	protected ArrayList queryIDs = new ArrayList( );

	/**
	 * the logger
	 */
	protected static Logger logger = Logger.getLogger( DteDataEngine.class
			.getName( ) );

	/** Used for registering the test expression */
	private static String TEST_EXPRESSION = "test_expression"; //$NON-NLS-1$
	/** Used for registering the first value expression of the rule */
	private static String VALUE1 = "value1"; //$NON-NLS-1$
	/** Used for registering the second value expression of the rule */
	private static String VALUE2 = "value2"; //$NON-NLS-1$

	/**
	 * The first element is uni-operator and the second needs to be added
	 * following the test expression
	 */
	private final static int[] uniOperator = new int[]{
			IConditionalExpression.OP_NULL, IConditionalExpression.OP_NOT_NULL,
			IConditionalExpression.OP_TRUE, IConditionalExpression.OP_FALSE};
	private final static String[] uniOperatorStr = new String[]{"== null", //$NON-NLS-1$
			"!= null", "==true", "==false"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/**
	 * The first element is bi-operator and the second is placed between the two
	 * test expressions
	 */
	private final static int[] biOperator = new int[]{
			IConditionalExpression.OP_LT, IConditionalExpression.OP_LE,
			IConditionalExpression.OP_EQ, IConditionalExpression.OP_NE,
			IConditionalExpression.OP_GE, IConditionalExpression.OP_GT};
	private static String[] biOperatorStr = new String[]{"<", "<=", "==", "!=", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			">=", ">"}; //$NON-NLS-1$ //$NON-NLS-2$

	public void prepare( Report report, Map appContext )
	{
		ReportDesignHandle rptHandle = (ReportDesignHandle) report
				.getReportDesign( );

		// Handling data sources
		List dataSourceList = rptHandle.getAllDataSources( );
		for ( int i = 0; i < dataSourceList.size( ); i++ )
		{
			DataSourceHandle dataSource = (DataSourceHandle) dataSourceList
					.get( i );
			try
			{
				dataEngine.defineDataSource( ModelDteApiAdapter.getInstance( )
						.createDataSourceDesign( dataSource ) );
			}
			catch ( BirtException be )
			{
				logger.log( Level.SEVERE, be.getMessage( ), be );
				context.addException( dataSource, be );
			}
		}

		// Handling data sets
		List dataSetList = rptHandle.getAllDataSets( );
		for ( int i = 0; i < dataSetList.size( ); i++ )
		{
			DataSetHandle dataset = (DataSetHandle) dataSetList.get( i );
			try
			{
				dataEngine.defineDataSet( ModelDteApiAdapter.getInstance( )
						.createDataSetDesign( dataset ) );
			}
			catch ( BirtException be )
			{
				logger.log( Level.SEVERE, be.getMessage( ), be );
				context.addException( dataset, be );
			}
		}

		// build report queries
		new ReportQueryBuilder( ).build( report, context );

		doPrepareQueryID( report, appContext );
	}

	abstract protected void doPrepareQueryID( Report report, Map appContext );

	
	public IResultSet execute( IBaseQueryDefinition query )
	{
		if ( query instanceof IQueryDefinition )
		{
			return doExecute( query );
		}
		else if ( query instanceof ISubqueryDefinition )
		{
			return getSubqueryResult( query );
		}
		return null;
	}

	abstract protected IResultSet doExecute( IBaseQueryDefinition query );

	protected IResultSet getParentResultSet( )
	{
		for ( int i = queryResultStack.size( ) - 1; i >= 0; i-- )
		{
			DteResultSet rs = (DteResultSet) queryResultStack.get( i );
			if ( rs.getQueryResults( ) != null )
			{
				return rs;
			}
		}
		return null;
	}

	protected boolean validateQueryResult( IQueryResults queryResults )
			throws BirtException
	{
		IResultIterator riter = queryResults.getResultIterator( );
		assert riter != null;
		return riter != null;
	}

	protected IResultSet getSubqueryResult( IBaseQueryDefinition query )
	{
		// Extension Item may used to create the query stack, so we must do
		// error handling.
		assert query instanceof ISubqueryDefinition;
		if ( queryResultStack.isEmpty( ) )
		{
			return null;
		}
		assert ( queryResultStack.getLast( ) instanceof DteResultSet );
		DteResultSet resultSet;
		try
		{
			IResultIterator ri = ( (DteResultSet) queryResultStack.getLast( ) )
					.getResultIterator( ).getSecondaryIterator(
							( (ISubqueryDefinition) query ).getName( ),
							context.getSharedScope( ) );
			assert ri != null;
			resultSet = new DteResultSet( ri, this );
			queryResultStack.addLast( resultSet );
			return resultSet;
		}
		catch ( BirtException e )
		{
			logger.log( Level.SEVERE, e.getMessage( ), e );
			context.addException( e );
			return null;
		}
	}

	abstract public void close( );

	abstract public void shutdown( );

	public Object evaluate( IBaseExpression expr )
	{
		if ( expr == null )
		{
			return null;
		}

		if ( !queryResultStack.isEmpty( ) ) // DtE handles evaluation
		{
			try
			{
				Object value = ( (DteResultSet) queryResultStack.getLast( ) )
						.getResultIterator( ).getValue( expr );
				if ( value != null )
				{
					return context.jsToJava( value );
				}
				return null;
			}
			catch ( BirtException e )
			{
				logger.log( Level.SEVERE, e.getMessage( ), e );
				context.addException( e );
				return null;
			}
			catch ( JavaScriptException ee )
			{
				logger.log( Level.SEVERE, ee.getMessage( ), ee );
				context.addException( new EngineException(
						MessageConstants.INVALID_EXPRESSION_ERROR, expr, ee ) );
				return null;
			}
		}

		// Rhino handles evaluation
		if ( expr instanceof IScriptExpression )
		{
			return context.evaluate( ( (IScriptExpression) expr ).getText( ) );
		}
		if ( expr instanceof IConditionalExpression )
		{
			return evaluateCondExpr( (IConditionalExpression) expr );
		}

		// unsupported expression type
		assert ( false );
		return null;
	}

	protected Object evaluateCondExpr( IConditionalExpression expr )
	{
		if ( expr == null )
		{
			EngineException e = new EngineException( "Failed to evaluate: null" );//$NON-NLS-1$
			context.addException( e );
			logger.log( Level.SEVERE, e.getMessage( ), e );
			return new Boolean( false );
		}

		int operator = expr.getOperator( );
		IScriptExpression testExpr = expr.getExpression( );
		IScriptExpression v1 = expr.getOperand1( );
		IScriptExpression v2 = expr.getOperand2( );

		if ( testExpr == null )
			return new Boolean( false );

		context.newScope( );
		Object testExprValue = context.evaluate( testExpr.getText( ) );
		if ( IConditionalExpression.OP_NONE == operator )
		{
			context.exitScope( );
			return testExprValue;
		}

		context.registerBean( TEST_EXPRESSION, testExprValue );
		for ( int i = 0; i < uniOperatorStr.length; i++ )
		{
			if ( uniOperator[i] == operator )
			{
				Object ret = context.evaluate( TEST_EXPRESSION
						+ uniOperatorStr[i] );
				context.exitScope( );
				return ret;
			}
		}

		if ( v1 == null )
		{
			context.exitScope( );
			return new Boolean( false );
		}
		Object vv1 = context.evaluate( v1.getText( ) );
		context.registerBean( VALUE1, vv1 );
		for ( int i = 0; i < biOperatorStr.length; i++ )
		{
			if ( biOperator[i] == operator )
			{
				Object ret = context.evaluate( TEST_EXPRESSION
						+ biOperatorStr[i] + VALUE1 );
				context.exitScope( );
				return ret;
			}
		}
		if ( IConditionalExpression.OP_LIKE == operator )
		{
			Object ret = context.evaluate( "(" + TEST_EXPRESSION //$NON-NLS-1$
					+ ").match(" + VALUE1 + ")!=null" ); //$NON-NLS-1$//$NON-NLS-2$
			context.exitScope( );
			return ret;
		}

		if ( v2 == null )
		{
			context.exitScope( );
			return new Boolean( false );
		}
		Object vv2 = context.evaluate( v2.getText( ) );
		context.registerBean( VALUE2, vv2 );

		if ( IConditionalExpression.OP_BETWEEN == operator )
		{
			Object ret = context
					.evaluate( "(" + TEST_EXPRESSION + " >= " + VALUE1 + ") && (" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							+ TEST_EXPRESSION + " <= " + VALUE2 + ")" ); //$NON-NLS-1$//$NON-NLS-2$
			context.exitScope( );
			return ret;
		}
		else if ( IConditionalExpression.OP_NOT_BETWEEN == operator )
		{
			Object ret = context
					.evaluate( "(" + TEST_EXPRESSION + " < " + VALUE1 + ") || (" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							+ TEST_EXPRESSION + " > " + VALUE2 + ")" ); //$NON-NLS-1$ //$NON-NLS-2$
			context.exitScope( );
			return ret;
		}
		logger.log( Level.SEVERE, "Invalid operator: " + operator ); //$NON-NLS-1$
		context.exitScope( );
		return new Boolean( false );
	}

	public DataEngine getDataEngine( )
	{
		return dataEngine;
	}

	protected class Key implements Serializable
	{

		private static final long serialVersionUID = 42L;
		public String parentRSID = null;
		public String rowid = null;
		public String resultSetID = null;

		public Key( String parentid, String rid, String resultID )
		{
			parentRSID = parentid;
			rowid = rid;
			resultSetID = resultID;
		}

		private void writeObject( ObjectOutputStream out ) throws IOException
		{
			out.writeObject( parentRSID );
			out.writeObject( rowid );
			out.writeObject( resultSetID );
		}

		private void readObject( ObjectInputStream in ) throws IOException,
				ClassNotFoundException
		{
			parentRSID = (String) in.readObject( );
			rowid = (String) in.readObject( );
			resultSetID = (String) in.readObject( );
		}

		public boolean equalTo( String parentID, String rid )
		{
			if ( parentRSID.equals( parentID ) && rowid.equals( rid ) )
			{
				return true;
			}
			return false;
		}
	}

	protected class QueryID implements Serializable
	{
		private static final long serialVersionUID = 5750799978535272737L;
		public ArrayList beforeExpressionIDs;
		public ArrayList afterExpressionIDs;
		public ArrayList rowExpressionIDs;

		public ArrayList subqueryIDs;
		public ArrayList groupIDs;

		public QueryID( )
		{
			beforeExpressionIDs = new ArrayList( );
			afterExpressionIDs = new ArrayList( );
			rowExpressionIDs = new ArrayList( );
			groupIDs = new ArrayList( );
			subqueryIDs = new ArrayList( );
		}

		private void writeObject( ObjectOutputStream out ) throws IOException
		{
			out.writeObject( beforeExpressionIDs );
			out.writeObject( afterExpressionIDs );
			out.writeObject( rowExpressionIDs );
			out.writeObject( groupIDs );
			out.writeObject( subqueryIDs );
		}

		private void readObject( ObjectInputStream in ) throws IOException,
				ClassNotFoundException
		{
			beforeExpressionIDs = (ArrayList) in.readObject( );
			afterExpressionIDs = (ArrayList) in.readObject( );
			rowExpressionIDs = (ArrayList) in.readObject( );
			groupIDs = (ArrayList) in.readObject( );
			subqueryIDs = (ArrayList) in.readObject( );
		}
	}

	protected String getResultID( String parentID, String rowid )
	{
		Iterator iter = queryResultRelations.iterator( );
		while ( iter.hasNext( ) )
		{
			Key key = (Key) iter.next( );
			if ( key.equalTo( parentID, rowid ) )
			{
				return key.resultSetID;
			}
		}
		return null;
	}
}
