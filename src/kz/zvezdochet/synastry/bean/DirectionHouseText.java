package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.DirectionHouseService;

/**
 * Толкование дирекции планеты первого партнёра к дому второго
 * @author Natalie Didenko
 *
 */
public class DirectionHouseText extends SynastryHouseText {
	private static final long serialVersionUID = -4500322613671258450L;

	@Override
	public ModelService getService() {
		return new DirectionHouseService();
	}
}
