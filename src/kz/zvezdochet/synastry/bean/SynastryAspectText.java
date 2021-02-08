package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.SynastryAspectService;

/**
 * Синастрический аспект
 * @author Natalie Didenko
 *
 */
public class SynastryAspectText extends PlanetAspectText {
	private static final long serialVersionUID = -7872375252777326071L;

	public SynastryAspectText() {}

	@Override
	public ModelService getService() {
		return new SynastryAspectService();
	}

	/**
	 * Уровень критичности аспекта
	 */
	private int level;

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
