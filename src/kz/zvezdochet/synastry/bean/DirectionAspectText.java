package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.DirectionAspectService;

/**
 * Синастрический дирекционный аспект
 * @author Natalie Didenko
 */
public class DirectionAspectText extends SynastryAspectText {
	private static final long serialVersionUID = -8113776302595371079L;

	public DirectionAspectText() {}

	@Override
	public ModelService getService() {
		return new DirectionAspectService();
	}
}
