/**
 * 
 * http://www.digitalekabeltelevisie.nl/dvb_inspector
 * 
 * This code is Copyright 2009-2019 by Eric Berendsen (e_berendsen@digitalekabeltelevisie.nl)
 * 
 * This file is part of DVB Inspector.
 * 
 * DVB Inspector is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * DVB Inspector is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with DVB Inspector. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * The author requests that he be notified of any application, applet, or other binary that makes use of this code, but
 * that's more out of curiosity than anything and is not required.
 * 
 */

package nl.digitalekabeltelevisie.data.mpeg.pes.video26x;

import java.util.*;

import nl.digitalekabeltelevisie.controller.ChartLabel;
import nl.digitalekabeltelevisie.data.mpeg.PesPacketData;
import nl.digitalekabeltelevisie.data.mpeg.pes.GeneralPesHandler;

/**
 * @author Eric
 *
 */
public abstract class H26xHandler<P extends H26xPESDataField<?>, N extends AbstractNALUnit> extends GeneralPesHandler {

	/**
	 * Meta Iterator to iterate over all NALUnits in this PES stream, regardless of grouping in PES Packets
	 *
	 * In general this does not work for streams with no alignment, So every NALUnit should be contained in a PES packet.
	 *
	 * @author Eric
	 *
	 */
	protected class NALUnitIterator{

		Iterator<PesPacketData> pesIterator = null;
		N nextSection = null;
		private Iterator<? extends N> sectionIter;

		public NALUnitIterator() {
			pesIterator = pesPackets.iterator();
			sectionIter = getNextSectionIter();
			if(sectionIter!=null){
				nextSection = sectionIter.next();
			}
		}

		@SuppressWarnings("unchecked")
		private Iterator<N> getNextSectionIter(){

			Iterator<N> result = null;
			do {
				final P pesPacket = (P)pesIterator.next();
				result = (Iterator<N>) pesPacket.getNalUnits().iterator();

			} while (((result==null)||!result.hasNext())&&(pesIterator.hasNext()));
			return result;

		}

		public N next() {
			final N result = nextSection;
			if((sectionIter!=null)&&sectionIter.hasNext()){
				nextSection = sectionIter.next();
			}else if(pesIterator.hasNext()){
				sectionIter= getNextSectionIter();
				if(sectionIter.hasNext()){
					nextSection = sectionIter.next();
				}else{
					nextSection = null;
				}
			}else{
				nextSection = null;
			}

			return result;
		}


	}


	/**
	 * 
	 */
	public H26xHandler() {
		super();
	}

	@Override
	protected void processPesDataBytes(final PesPacketData pesData) {
		pesPackets.add(createH26xPESDataField(pesData));

	}

	/**
	 * @param pesData
	 * @return
	 */
	abstract protected P createH26xPESDataField(final PesPacketData pesData);
	
	
	// helpers for drawing frame size graph
	/**
	 * @param accessUnitData
	 * @return
	 */
	protected static boolean notZero(final int[] accessUnitData) {
		for (final int i : accessUnitData) {
			if(i!=0){
				return true;
			}
		}

		return false;
	}

	protected static int drawBarAccessUnit(final List<int[]> frameSize, final List<ChartLabel> labels, int[] accessUnitData, int c) {
		int count = c;
		if(notZero(accessUnitData)){
			ChartLabel label =new ChartLabel(""+count, (short)count);
			labels.add(label);
			frameSize.add(accessUnitData);
			count++;
		}
		return count;
	}



}