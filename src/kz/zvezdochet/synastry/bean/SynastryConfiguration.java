package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.bean.EventConfiguration;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.SynastryConfigurationService;

/**
 * Конфигурация аспектов события
 * @author Natalie Didenko
 */
public class SynastryConfiguration extends EventConfiguration {
	private static final long serialVersionUID = -7760856047126133380L;

	@Override
	public ModelService getService() {
		return new SynastryConfigurationService();
	}

	/**
	 * Синастрия
	 */
	private Synastry synastry;

	public Synastry getSynastry() {
		return synastry;
	}

	public void setSynastry(Synastry event) {
		this.synastry = event;
	}

	/**
	 * Признак того, что главная планета конфигурации принадлежит второму партнёру
	 */
	private boolean reverse;

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}
}
