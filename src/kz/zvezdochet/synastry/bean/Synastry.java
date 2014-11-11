package kz.zvezdochet.synastry.bean;

import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.synastry.service.SynastryService;

/**
 * Синастрия
 * @author Nataly Didenko
 *
 */
public class Synastry extends Model {
	private static final long serialVersionUID = 1549847723120810835L;

	@Override
	public ModelService getService() {
		return new SynastryService();
	}
	
	/**
	 * Идентификатор персоны
	 */
	private long eventid;
	/**
	 * Идентификатор партнёра
	 */
	private long partnerid;

	public long getEventid() {
		return eventid;
	}
	public void setEventid(long eventid) {
		this.eventid = eventid;
	}
	public long getPartnerid() {
		return partnerid;
	}
	public void setPartnerid(long partnerid) {
		this.partnerid = partnerid;
	}
}
