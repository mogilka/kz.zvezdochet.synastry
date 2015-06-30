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
	/**
	 * Описание
	 */
	private String description;

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

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

	/**
	 * Идентификатор пользователя
	 */
	private long userid;
	/**
	 * Дата создания
	 */
	private String date;
	/**
	 * Признак выполненного расчёта
	 */
	private boolean calculated;
	/**
	 * Признак того, что оба парнёра - знаменитости
	 */
	private boolean celebrity;

	public long getUserid() {
		return userid;
	}
	public void setUserid(long userid) {
		this.userid = userid;
	}

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public boolean isCalculated() {
		return calculated;
	}
	public void setCalculated(boolean calculated) {
		this.calculated = calculated;
	}
	public boolean isCelebrity() {
		return celebrity;
	}
	public void setCelebrity(boolean celebrity) {
		this.celebrity = celebrity;
	}
}
