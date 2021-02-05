package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.core.bean.TextGenderModel;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.SynastrySignService;

/**
 * Прототип справочника синастрии по выбранной планете
 * @author Natalie Didenko
 *
 */
public class SynastryText extends TextGenderModel {
	private static final long serialVersionUID = -7027009322004688751L;

	/**
	 * Синастрическая планета
	 */
    private Planet planet;
        
	/**
	 * Знак первого партнера, в котором находится планета
	 */
    private Sign sign1;
        
	/**
	 * Знак второго партнера, в котором находится планета
	 */
    private Sign sign2;

	public Sign getSign1() {
		return sign1;
	}

	public void setSign1(Sign sign1) {
		this.sign1 = sign1;
	}

	public Planet getPlanet() {
		return planet;
	}

	public void setPlanet(Planet planet) {
		this.planet = planet;
	}

	public Sign getSign2() {
		return sign2;
	}

	public void setSign2(Sign sign2) {
		this.sign2 = sign2;
	}

	@Override
	public ModelService getService() {
		return new SynastrySignService();
	}
}
