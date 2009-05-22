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

package org.eclipse.birt.report.model.parser;

import java.util.List;

import org.eclipse.birt.report.model.api.core.IModuleModel;
import org.eclipse.birt.report.model.api.core.IStructure;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.elements.structures.ComputedColumn;
import org.eclipse.birt.report.model.api.elements.structures.DataSetParameter;
import org.eclipse.birt.report.model.api.elements.structures.DateTimeFormatValue;
import org.eclipse.birt.report.model.api.elements.structures.EmbeddedImage;
import org.eclipse.birt.report.model.api.elements.structures.FilterCondition;
import org.eclipse.birt.report.model.api.elements.structures.HighlightRule;
import org.eclipse.birt.report.model.api.elements.structures.MapRule;
import org.eclipse.birt.report.model.api.elements.structures.NumberFormatValue;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.api.elements.structures.OdaDesignerState;
import org.eclipse.birt.report.model.api.elements.structures.ParameterFormatValue;
import org.eclipse.birt.report.model.api.elements.structures.StringFormatValue;
import org.eclipse.birt.report.model.api.metadata.IPropertyDefn;
import org.eclipse.birt.report.model.api.metadata.IPropertyType;
import org.eclipse.birt.report.model.api.metadata.IStructureDefn;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.core.StyledElement;
import org.eclipse.birt.report.model.elements.Cell;
import org.eclipse.birt.report.model.elements.DataSet;
import org.eclipse.birt.report.model.elements.GraphicMasterPage;
import org.eclipse.birt.report.model.elements.GroupElement;
import org.eclipse.birt.report.model.elements.ListGroup;
import org.eclipse.birt.report.model.elements.ListingElement;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.birt.report.model.elements.OdaDataSource;
import org.eclipse.birt.report.model.elements.ReportDesign;
import org.eclipse.birt.report.model.elements.ReportItem;
import org.eclipse.birt.report.model.elements.ScalarParameter;
import org.eclipse.birt.report.model.elements.TableItem;
import org.eclipse.birt.report.model.elements.TableRow;
import org.eclipse.birt.report.model.elements.interfaces.ICellModel;
import org.eclipse.birt.report.model.elements.interfaces.IGroupElementModel;
import org.eclipse.birt.report.model.elements.interfaces.ILevelModel;
import org.eclipse.birt.report.model.elements.interfaces.IListingElementModel;
import org.eclipse.birt.report.model.elements.interfaces.IOdaExtendableElementModel;
import org.eclipse.birt.report.model.elements.interfaces.IReportDesignModel;
import org.eclipse.birt.report.model.elements.interfaces.IReportItemModel;
import org.eclipse.birt.report.model.elements.interfaces.IScalarParameterModel;
import org.eclipse.birt.report.model.elements.interfaces.ISimpleDataSetModel;
import org.eclipse.birt.report.model.elements.interfaces.IStyleModel;
import org.eclipse.birt.report.model.elements.interfaces.IStyledElementModel;
import org.eclipse.birt.report.model.elements.olap.Level;
import org.eclipse.birt.report.model.metadata.ODAExtensionElementDefn;
import org.eclipse.birt.report.model.metadata.PropertyDefn;
import org.eclipse.birt.report.model.util.AbstractParseState;
import org.eclipse.birt.report.model.util.ModelUtil;
import org.eclipse.birt.report.model.util.VersionUtil;
import org.eclipse.birt.report.model.util.XMLParserException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Parses the "property" tag. The tag may give the property value of the element
 * or the member of the structure.
 */

class PropertyState extends AbstractPropertyState
{

	/* cached hash codes of string for comparision */

	private static final int GROUP_START_PROP = ListGroup.GROUP_START_PROP
			.toLowerCase( ).hashCode( );
	private static final int CHEET_SHEET = "cheetSheet".toLowerCase( ).hashCode( ); //$NON-NLS-1$
	private static final int FILTER_OPERATOR_MEMBER = FilterCondition.OPERATOR_MEMBER
			.toLowerCase( ).hashCode( );
	private static final int MAPRULE_OPERATOR_MEMBER = MapRule.OPERATOR_MEMBER
			.toLowerCase( ).hashCode( );
	private static final int DATE_TIME_FORMAT_STRUCT = DateTimeFormatValue.FORMAT_VALUE_STRUCT
			.toLowerCase( ).hashCode( );
	private static final int NUMBER_FORMAT_STRUCT = NumberFormatValue.FORMAT_VALUE_STRUCT
			.toLowerCase( ).hashCode( );
	private static final int STRING_FORMAT_STRUCT = StringFormatValue.FORMAT_VALUE_STRUCT
			.toLowerCase( ).hashCode( );
	private static final int PARAM_FORMAT_STRUCT = ParameterFormatValue.FORMAT_VALUE_STRUCT
			.toLowerCase( ).hashCode( );

	private static final int HEADER_HEIGHT = "headerHeight".toLowerCase( ).hashCode( ); //$NON-NLS-1$
	private static final int FOOTER_HEIGHT = "footerHeight".toLowerCase( ).hashCode( ); //$NON-NLS-1$

	private static final int THUMBNAIL_PROP = IReportDesignModel.THUMBNAIL_PROP
			.toLowerCase( ).hashCode( );
	private static final int DATA_MEMBER = EmbeddedImage.DATA_MEMBER
			.toLowerCase( ).hashCode( );
	private static final int CONTENT_AS_BLOB_MEMBER = OdaDesignerState.CONTENT_AS_BLOB_MEMBER
			.toLowerCase( ).hashCode( );

	private static final int ON_CREATE_METHOD = ICellModel.ON_CREATE_METHOD
			.toLowerCase( ).hashCode( );

	private static final int CACHED_ROW_COUNT_PROP = ISimpleDataSetModel.CACHED_ROW_COUNT_PROP
			.toLowerCase( ).hashCode( );

	private static final int CHOICE_VERTICAL_ALIGN = DesignChoiceConstants.CHOICE_VERTICAL_ALIGN
			.toLowerCase( ).hashCode( );
	private static final int DEFAULT_VALUE_PROP = ScalarParameter.DEFAULT_VALUE_PROP
			.toLowerCase( ).hashCode( );
	private static final int DATA_TYPE_MEMBER = DataSetParameter.DATA_TYPE_MEMBER
			.toLowerCase( ).hashCode( );

	/**
	 * Property defn.
	 */
	protected PropertyDefn propDefn = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.birt.report.model.parser.AbstractPropertyState#
	 * AbstractPropertyState(DesignParserHandler theHandler, DesignElement
	 * element )
	 */

	PropertyState( ModuleParserHandler theHandler, DesignElement element )
	{
		super( theHandler, element );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.birt.report.model.parser.AbstractPropertyState#
	 * AbstractPropertyState(DesignParserHandler theHandler, DesignElement
	 * element, String propName, IStructure struct)
	 */

	PropertyState( ModuleParserHandler theHandler, DesignElement element,
			PropertyDefn propDefn, IStructure struct )
	{
		super( theHandler, element );

		this.propDefn = propDefn;
		this.struct = struct;
	}

	/**
	 * Sets the name in attribute.
	 * 
	 * @param name
	 *            the value of the attribute name
	 */

	protected void setName( String name )
	{
		super.setName( name );

		if ( struct != null )
		{
			propDefn = (PropertyDefn) struct.getDefn( ).getMember( name );
		}
		else
		{
			propDefn = element.getPropertyDefn( name );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.model.parser.AbstractPropertyState#parseAttrs
	 * (org.xml.sax.Attributes)
	 */

	public void parseAttrs( Attributes attrs ) throws XMLParserException
	{
		super.parseAttrs( attrs );

		if ( handler.markLineNumber
				&& IModuleModel.THEME_PROP.equalsIgnoreCase( name ) )
		{
			handler.module.addLineNo( element.getPropertyDefn( name ),
					new Integer( handler.getCurrentLineNo( ) ) );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.util.AbstractParseState#end()
	 */

	public void end( ) throws SAXException
	{
		String value = text.toString( );
		doEnd( value );
	}

	/**
	 * @param value
	 */

	protected void doEnd( String value )
	{
		if ( struct != null )
		{
			setMember( struct, propDefn.getName( ), name, value );
			return;
		}

		IPropertyDefn jmpDefn = null;

		if ( struct != null )
			jmpDefn = struct.getDefn( ).getMember( name );
		else
			jmpDefn = element.getPropertyDefn( name );
		
		if ( IStyledElementModel.STYLE_PROP.equalsIgnoreCase( name ) )
		{
			// Ensure that the element can have a style.

			if ( !element.getDefn( ).hasStyle( ) )
			{
				DesignParserException e = new DesignParserException(
						new String[]{name},
						DesignParserException.DESIGN_EXCEPTION_UNDEFINED_PROPERTY );
				RecoverableError.dealUndefinedProperty( handler, e );
				return;
			}

			( (StyledElement) element ).setStyleName( value );
		}
		else if ( handler.versionNumber >= VersionUtil.VERSION_3_2_16
				&& isXMLorScriptType( jmpDefn )
				&& !ModelUtil.isExtensionPropertyOwnModel( jmpDefn ) )
		{
			// for the old design file, there is not necessary to do this.
			// But for the new design file, it is necessary to escape CDATA
			// related characters.
			value = deEscape( (String) value );
			setProperty( name, value );
		}
		else
		{
			setProperty( name, value );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.model.parser.AbstractPropertyState#generalJumpTo
	 * ()
	 */

	protected AbstractParseState generalJumpTo( )
	{
		IPropertyDefn jmpDefn = null;
		if ( struct != null )
			jmpDefn = struct.getDefn( ).getMember( name );
		else
			jmpDefn = element.getPropertyDefn( name );

		if ( jmpDefn != null && ( (PropertyDefn) jmpDefn ).isElementType( ) )
		{
			ElementPropertyState state = new ElementPropertyState( handler,
					element );
			state.setName( name );
			return state;
		}

		if ( element instanceof ReportDesign && THUMBNAIL_PROP == nameValue )
		{
			Base64PropertyState state = new Base64PropertyState( handler,
					element, IReportDesignModel.CHARSET );
			state.setName( name );
			return state;
		}

		if ( struct instanceof EmbeddedImage && DATA_MEMBER == nameValue )
		{
			Base64PropertyState state = new Base64PropertyState( handler,
					element, propDefn, struct, EmbeddedImage.CHARSET );
			state.setName( name );
			return state;
		}

		if ( struct instanceof OdaDesignerState
				&& CONTENT_AS_BLOB_MEMBER == nameValue )
		{
			Base64PropertyState state = new Base64PropertyState( handler,
					element, propDefn, struct, OdaDesignerState.CHARSET );
			state.setName( name );
			return state;
		}
		if ( ON_CREATE_METHOD == nameValue
				&& ( element instanceof Cell || element instanceof TableRow ) )
		{
			CompatibleMiscExpressionState state = new CompatibleMiscExpressionState(
					handler, element );
			state.setName( name );
			return state;
		}

		return super.generalJumpTo( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.birt.report.model.parser.AbstractPropertyState#
	 * versionConditionalJumpTo()
	 */

	protected AbstractParseState versionConditionalJumpTo( )
	{
		if ( handler.versionNumber <= VersionUtil.VERSION_3_2_7 )
		{
			IPropertyDefn jmpDefn = null;

			if ( struct != null )
				jmpDefn = struct.getDefn( ).getMember( name );
			else
				jmpDefn = element.getPropertyDefn( name );

			if ( element instanceof ListGroup && GROUP_START_PROP == nameValue )
			{
				CompatibleRenamedPropertyState state = new CompatibleRenamedPropertyState(
						handler, element, ListGroup.GROUP_START_PROP );
				state.setName( ListGroup.INTERVAL_BASE_PROP );
				return state;
			}

			if ( element instanceof ReportDesign && CHEET_SHEET == nameValue )
			{
				CompatibleRenamedPropertyState state = new CompatibleRenamedPropertyState(
						handler, element, "cheetSheet" ); //$NON-NLS-1$
				state.setName( ReportDesign.CHEAT_SHEET_PROP );
				return state;
			}

			int jmpDefnValue = -1;
			int jmpStructDefnValue = -1;
			if ( jmpDefn != null )
			{
				jmpDefnValue = jmpDefn.getName( ).toLowerCase( ).hashCode( );
				IStructureDefn structDefn = jmpDefn.getStructDefn( );
				if ( structDefn != null )
					jmpStructDefnValue = structDefn.getName( ).toLowerCase( )
							.hashCode( );
			}

			if ( ( FILTER_OPERATOR_MEMBER == jmpDefnValue && struct instanceof FilterCondition )
					|| ( MAPRULE_OPERATOR_MEMBER == jmpDefnValue && ( struct instanceof MapRule || struct instanceof HighlightRule ) ) )
			{
				CompatibleOperatorState state = new CompatibleOperatorState(
						handler, element, propDefn, struct );
				state.setName( name );
				return state;
			}

			if ( DATE_TIME_FORMAT_STRUCT == jmpStructDefnValue
					|| NUMBER_FORMAT_STRUCT == jmpStructDefnValue
					|| STRING_FORMAT_STRUCT == jmpStructDefnValue
					|| PARAM_FORMAT_STRUCT == jmpStructDefnValue )
			{
				CompatibleFormatPropertyState state = new CompatibleFormatPropertyState(
						handler, element, propDefn, struct );
				state.setName( name );
				state.createStructure( );
				return state;
			}

			if ( element instanceof GraphicMasterPage
					&& ( HEADER_HEIGHT == nameValue || FOOTER_HEIGHT == nameValue ) )
				return new CompatibleIgnorePropertyState( handler, element );

			if ( ( element instanceof ListingElement || element instanceof GroupElement ) )
			{
				// now 'pageBreakInterval' is supported on table/list

				if ( IListingElementModel.PAGE_BREAK_INTERVAL_PROP
						.equalsIgnoreCase( name )
						&& element instanceof GroupElement )
					return new CompatibleIgnorePropertyState( handler, element );

				if ( name.equalsIgnoreCase( "onStart" ) || name //$NON-NLS-1$
						.equalsIgnoreCase( "onFinish" ) ) //$NON-NLS-1$
					return new CompatibleIgnorePropertyState( handler, element );

				if ( "onRow".equalsIgnoreCase( name ) //$NON-NLS-1$
						&& !( element instanceof TableItem ) )
					return new CompatibleIgnorePropertyState( handler, element );

				if ( "onRow".equalsIgnoreCase( name ) )//$NON-NLS-1$
					return new CompatibleOnRowPropertyState( handler, element );
			}

		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_16 )
		{
			if ( element instanceof Module
					&& ( IModuleModel.INCLUDE_RESOURCE_PROP
							.equalsIgnoreCase( name ) || "msgBaseName" //$NON-NLS-1$
					.equalsIgnoreCase( name ) ) )
			{

				CompatibleIncludeResourceState state = new CompatibleIncludeResourceState(
						handler, element );
				state.setName( IModuleModel.INCLUDE_RESOURCE_PROP );

				return state;
			}
		}

		// change 'week' to 'week-of-year' and change 'day' to 'day-of-year'.

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_14
				&& element instanceof Level
				&& ILevelModel.DATE_TIME_LEVEL_TYPE.equalsIgnoreCase( name ) )
		{
			CompatibleDateTimeLevelTypeState state = new CompatibleDateTimeLevelTypeState(
					handler, element );
			state.setName( name );
			return state;
		}

		// change interval property value to none if property value is not
		// 'none,'interval' or 'perfix'.

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_14
				&& element instanceof Level
				&& ILevelModel.INTERVAL_PROP.equalsIgnoreCase( name ) )
		{
			CompatibleIntervalState state = new CompatibleIntervalState(
					handler, element );
			state.setName( name );
			return state;
		}

		// Change 'cachedRowCount' to 'dataSetRowLimit' in DataSet element.

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_12
				&& element instanceof DataSet
				&& CACHED_ROW_COUNT_PROP == nameValue )
		{
			CompatibleRenamedPropertyState state = new CompatibleRenamedPropertyState(
					handler, element, ISimpleDataSetModel.CACHED_ROW_COUNT_PROP );
			state.setName( ISimpleDataSetModel.DATA_SET_ROW_LIMIT );
			return state;
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_11
				&& propDefn == null && element instanceof ScalarParameter
				&& ( "allowNull".equalsIgnoreCase( name ) || "allowBlank" //$NON-NLS-1$ //$NON-NLS-2$
				.equalsIgnoreCase( name ) ) )
		{
			CompatibleParamAllowMumbleState state = new CompatibleParamAllowMumbleState(
					handler, element, name );

			state.setName( ScalarParameter.IS_REQUIRED_PROP );
			return state;
		}

		if ( handler.versionNumber <= VersionUtil.VERSION_3_2_10
				&& propDefn == null
				&& element instanceof IOdaExtendableElementModel )
		{
			ODAExtensionElementDefn elementDefn = null;

			if ( element instanceof OdaDataSet )
				elementDefn = (ODAExtensionElementDefn) ( (OdaDataSet) element )
						.getExtDefn( );
			else if ( element instanceof OdaDataSource )
				elementDefn = (ODAExtensionElementDefn) ( (OdaDataSource) element )
						.getExtDefn( );

			if ( elementDefn != null )
			{
				List privatePropDefns = elementDefn
						.getODAPrivateDriverPropertyNames( );
				if ( privatePropDefns.contains( name ) )
				{
					CompatibleODAPrivatePropertyState state = new CompatibleODAPrivatePropertyState(
							handler, element );
					state.setName( name );
					return state;
				}
			}
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_10 )
		{
			if ( element instanceof ReportItem )
			{
				if ( IReportItemModel.TOC_PROP.equalsIgnoreCase( name ) )
				{
					CompatibleTOCPropertyState state = new CompatibleTOCPropertyState(
							handler, element );
					state.setName( IReportItemModel.TOC_PROP );
					return state;
				}
			}
			if ( element instanceof GroupElement )
			{
				if ( IGroupElementModel.TOC_PROP.equalsIgnoreCase( name ) )
				{
					CompatibleTOCPropertyState state = new CompatibleTOCPropertyState(
							handler, element );
					state.setName( IGroupElementModel.TOC_PROP );
					return state;
				}
			}
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_2
				&& CHOICE_VERTICAL_ALIGN == nameValue )
		{
			CompatibleVerticalAlignState state = new CompatibleVerticalAlignState(
					handler, element );
			state.setName( DesignChoiceConstants.CHOICE_VERTICAL_ALIGN );
			return state;
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_4
				&& ( element instanceof ScalarParameter )
				&& DEFAULT_VALUE_PROP == nameValue )
		{
			CompatiblePropertyTypeState state = new CompatiblePropertyTypeState(
					handler, element );
			state.setName( IScalarParameterModel.DEFAULT_VALUE_PROP );
			return state;
		}
		if ( handler.versionNumber <= VersionUtil.VERSION_3_2_0
				&& struct instanceof DataSetParameter
				&& "isNullable".equals( name ) ) //$NON-NLS-1$
		{
			CompatibleRenamedPropertyState state = new CompatibleRenamedPropertyState(
					handler, element, propDefn, struct, "isNullable" ); //$NON-NLS-1$
			state.setName( DataSetParameter.ALLOW_NULL_MEMBER );
			return state;
		}

		if ( ON_CREATE_METHOD == nameValue
				&& handler.versionNumber < VersionUtil.VERSION_3_2_0 )
		{
			CompatibleMiscExpressionState state = new CompatibleMiscExpressionState(
					handler, element );
			state.setName( name );
			return state;
		}

		if ( struct instanceof ComputedColumn
				&& "aggregrateOn".toLowerCase( ).hashCode( ) == nameValue //$NON-NLS-1$
				&& ( element instanceof ScalarParameter || element instanceof ReportItem )
				&& handler.versionNumber <= VersionUtil.VERSION_3_2_2 )
		{
			CompatibleRenamedPropertyState state = new CompatibleRenamedPropertyState(
					handler, element, propDefn, struct, "aggregrateOn" ); //$NON-NLS-1$
			state.setName( ComputedColumn.AGGREGATEON_MEMBER );
			return state;
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_6
				&& ( struct instanceof DataSetParameter || struct instanceof OdaDataSetParameter )
				&& DATA_TYPE_MEMBER == nameValue )
		{
			CompatibleColumnDataTypeState state = new CompatibleColumnDataTypeState(
					handler, element, propDefn, struct );
			state.setName( DataSetParameter.DATA_TYPE_MEMBER );
			return state;
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_2_9
				&& element instanceof ScalarParameter
				&& IScalarParameterModel.MUCH_MATCH_PROP
						.equalsIgnoreCase( name ) )
		{
			CompatibleMustMatchState state = new CompatibleMustMatchState(
					handler, element );
			state.setName( IScalarParameterModel.MUCH_MATCH_PROP );
			return state;
		}

		if ( handler.versionNumber < VersionUtil.VERSION_3_1_0
				&& ( IStyleModel.PAGE_BREAK_BEFORE_PROP.equalsIgnoreCase( name ) || IStyleModel.PAGE_BREAK_AFTER_PROP
						.equalsIgnoreCase( name ) ) )
		{
			CompatiblePageBreakPropState state = new CompatiblePageBreakPropState(
					handler, element );
			state.setName( name );
			return state;
		}

		return super.versionConditionalJumpTo( );
	}
	
	/**
	 * Checks input property definition is XML or script type.
	 * 
	 * @param jmpDefn
	 *            the property definition.
	 * @return true if the input property definition is XML or script type.
	 */
	private boolean isXMLorScriptType( IPropertyDefn jmpDefn )
	{
		return jmpDefn != null
				&& ( jmpDefn.getTypeCode( ) == IPropertyType.SCRIPT_TYPE || jmpDefn
						.getTypeCode( ) == IPropertyType.XML_TYPE );
	}
	
}