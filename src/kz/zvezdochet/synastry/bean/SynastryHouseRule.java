package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.analytics.bean.PlanetHouseRule;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.SynastryHouseRuleService;

/**
 * Толкование планет в астрологических домах
 * @author Natalie Didenko
 */
public class SynastryHouseRule extends PlanetHouseRule {
	private static final long serialVersionUID = -2467771886090316969L;

	@Override
	public ModelService getService() {
		return new SynastryHouseRuleService();
	}

	@Override
	public String toString() {
		String res = planet.getCode() + " in " + house.getCode();
		if (aspectType != null)
			res += " " + aspectType.getCode();
		if (planet2 != null)
			res += " " + planet2.getCode();
		if (house2 != null)
			res += " " + house2.getCode();
		return res;
	}
}
