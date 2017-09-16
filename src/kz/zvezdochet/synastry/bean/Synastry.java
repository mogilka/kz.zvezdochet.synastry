package kz.zvezdochet.synastry.bean;

import java.util.ArrayList;
import java.util.List;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.service.AspectService;
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
	 * Описание
	 */
	private String description;

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Признак выполненного расчёта
	 */
	private boolean calculated;
	/**
	 * Признак того, что оба парнёра - знаменитости
	 */
	private boolean celebrity;

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

	private Event event;
	private Event partner;

	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public Event getPartner() {
		return partner;
	}
	public void setPartner(Event partner) {
		this.partner = partner;
	}

	/**
	 * Аспекты синастрии
	 */
	private	List<SkyPointAspect> aspectList;

	public List<SkyPointAspect> getAspects() {
		return aspectList;
	}

	/**
	 * Инициализация аспектов
	 * @throws DataAccessException 
	 */
	public void initAspects() throws DataAccessException {
		try {
			if (aspectList != null && aspectList.size() > 0)
				return;

			aspectList = new ArrayList<>();
			event.init(false);
			partner.init(false);
			List<Model> planets = event.getConfiguration().getPlanets();
			List<Model> planets2 = partner.getConfiguration().getPlanets();
			List<Model> aspects = new AspectService().getList();
			for (Model model : planets) {
				Planet planet = (Planet)model;
				for (Model model2 : planets2) {
					Planet planet2 = (Planet)model2;
					double res = CalcUtil.getDifference(planet.getCoord(), planet2.getCoord());
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(planet);
					aspect.setSkyPoint2(planet2);
					aspect.setScore(CalcUtil.roundTo(res, 2));
					for (Model realasp : aspects) {
						Aspect a = (Aspect)realasp;
						if (a.isAspect(res)) {
							aspect.setAspect(a);
							aspect.setExact(a.isExact(res));
							aspect.setApplication(a.isApplication(res));
							aspectList.add(aspect);
							continue;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
