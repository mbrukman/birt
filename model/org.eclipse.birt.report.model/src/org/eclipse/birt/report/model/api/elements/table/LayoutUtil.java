/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Actuate Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.birt.report.model.api.elements.table;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.CellHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.TableHandle;
import org.eclipse.birt.report.model.api.core.IDesignElement;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.elements.Cell;
import org.eclipse.birt.report.model.elements.TableGroup;
import org.eclipse.birt.report.model.elements.TableItem;

/**
 * The utility class for <code>LayoutTable</code>.
 * 
 */

public class LayoutUtil
{

	/**
	 * Calculates the row number in the table.
	 * 
	 * @param table
	 *            the layout table
	 * @return the row number in the table
	 */

	protected static int getRowCount( LayoutTable table )
	{
		int rowCount = 0;

		LayoutSlot slot = table.getHeader( );
		rowCount += slot.getRowCount( );

		slot = table.getDetail( );
		rowCount += slot.getRowCount( );

		slot = table.getFooter( );
		rowCount += slot.getRowCount( );

		LayoutGroupBand groupSlot = table.getGroupHeaders( );
		for ( int i = 0; i < groupSlot.getGroupCount( ); i++ )
		{
			slot = groupSlot.getLayoutSlot( i );
			rowCount += slot.getRowCount( );
		}

		groupSlot = table.getGroupFooters( );
		for ( int i = 0; i < groupSlot.getGroupCount( ); i++ )
		{
			slot = groupSlot.getLayoutSlot( i );
			rowCount += slot.getRowCount( );
		}

		return rowCount;
	}

	/**
	 * Returns flattern slots of the layout table regardless GROUP/Table slots.
	 * 
	 * @param table
	 *            the layout table
	 * @return a list containing flattern slots
	 */

	protected static List getFlattenedLayoutSlots( LayoutTable table )
	{
		List list = new ArrayList( );
		list.add( table.getHeader( ) );

		LayoutGroupBand band = table.getGroupHeaders( );
		for ( int i = 0; i < band.getGroupCount( ); i++ )
			list.add( band.getLayoutSlot( i ) );

		list.add( table.getDetail( ) );

		band = table.getGroupFooters( );
		for ( int i = 0; i < band.getGroupCount( ); i++ )
			list.add( band.getLayoutSlot( i ) );

		list.add( table.getFooter( ) );

		return list;
	}

	/**
	 * Returns the layout slot in which the given <code>cell</code> resides.
	 * 
	 * @param cell
	 *            the cell element
	 * @return the layout slot
	 */

	private static LayoutSlot getLayoutSlotOfCell( CellHandle cell )
	{
		TableItem table = getTableContainer( cell.getElement( ) );
		if ( table == null )
			return null;

		LayoutTable layoutTable = table.getLayoutModel( cell.getModule( ) );

		LayoutSlot layoutSlot = null;
		DesignElementHandle grandPa = cell.getContainer( ).getContainer( );

		int groupLevel = 0;
		int slotId = cell.getContainer( ).getContainerSlotHandle( ).getSlotID( );

		if ( grandPa instanceof TableHandle )
			layoutSlot = layoutTable.getLayoutSlot( slotId );
		else
		{
			groupLevel = ( (TableGroup) grandPa.getElement( ) ).getGroupLevel( );
			layoutSlot = layoutTable.getLayoutSlot( groupLevel, slotId );
		}

		return layoutSlot;
	}

	/**
	 * Returns the effective column span of the given cell.
	 * 
	 * @param cell
	 *            the cell to find
	 * @return the 1-based effective column span of the given cell. 0 means the
	 *         cell is in the table element but it do not show in the layout.
	 */

	public static int getEffectiveColumnSpan( CellHandle cell )
	{
		LayoutSlot layoutSlot = getLayoutSlotOfCell( cell );

		// if the cell is in the grid, do not call the layout for this method

		if ( layoutSlot == null )
			return cell.getColumnSpan( );

		int rowId = cell.getContainer( ).getContainerSlotHandle( ).findPosn(
				cell.getContainer( ) );
		LayoutRow layoutRow = (LayoutRow) layoutSlot.getLayoutRow( rowId );

		int columnPosn = layoutRow
				.findCellColumnPos( (Cell) cell.getElement( ) );
		if ( columnPosn <= 0 )
			return 0;

		int effectiveColumnSpan = 0;
		for ( int i = columnPosn - 1; i < layoutRow.getColumnCount( ); i++ )
		{
			LayoutCell layoutCell = layoutRow.getLayoutCell( i );
			if ( layoutCell.getContent( ) != cell.getElement( ) )
				break;

			effectiveColumnSpan++;
		}

		return effectiveColumnSpan;
	}

	/**
	 * Returns the effective row span of the given cell.
	 * 
	 * @param cell
	 *            the cell to find
	 * @return the 1-based effective row span of the given cell. 0 means the
	 *         cell is in the table element but it do not show in the layout.
	 */

	public static int getEffectiveRowSpan( CellHandle cell )
	{
		LayoutSlot layoutSlot = getLayoutSlotOfCell( cell );

		// if the cell is in the grid, do not call the layout for this method

		if ( layoutSlot == null )
			return cell.getColumnSpan( );
		
		int rowId = cell.getContainer( ).getContainerSlotHandle( ).findPosn(
				cell.getContainer( ) );

		int effectiveRowSpan = 0;

		for ( int i = rowId; i < layoutSlot.getRowCount( ); i++ )
		{
			LayoutRow layoutRow = (LayoutRow) layoutSlot.getLayoutRow( i );
			LayoutCell layoutCell = layoutRow.getLayoutCell( cell );
			if ( layoutCell == null )
				break;

			if ( layoutCell.isEffectualDrop( ) )
			{
				assert layoutCell.isCellStartPosition( );
				return layoutCell.getRowSpanForDrop( );
			}
			effectiveRowSpan++;
		}

		return effectiveRowSpan;
	}

	/**
	 * Returns a nearest <code>TableItem</code> container for
	 * <code>TableRow</code>, <code>TableGroup</code> and
	 * <code>TableItem</code> if applicable.
	 * <p>
	 * If <code>TableRow</code> is in the <code>GridItem</code>, return
	 * <code>null</code>.
	 * 
	 * @param element
	 *            the element where the search begins
	 * @return a nearest <code>TableItem</code> container
	 */

	public static TableItem getTableContainer( IDesignElement element )
	{

		DesignElement tmpElement = (DesignElement) element;

		// the maximum level for a cell to find the table container is 3.

		int maxLevel = 3;

		for ( int i = 0; i < maxLevel; i++ )
		{
			if ( tmpElement == null || tmpElement instanceof TableItem )
				return (TableItem) tmpElement;

			tmpElement = tmpElement.getContainer( );
		}

		return null;
	}

}
