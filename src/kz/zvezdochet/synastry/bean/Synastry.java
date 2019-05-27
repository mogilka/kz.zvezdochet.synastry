package kz.zvezdochet.synastry.bean;

import java.util.ArrayList;
import java.util.Collection;
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
 * @author Natalie Didenko
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
			Collection<Planet> planets = event.getPlanets().values();
			Collection<Planet> planets2 = partner.getPlanets().values();
			List<Model> aspects = new AspectService().getList();
			for (Planet planet : planets) {
				for (Planet planet2 : planets2) {
					double res = CalcUtil.getDifference(planet.getLongitude(), planet2.getLongitude());
					for (Model realasp : aspects) {
						Aspect a = (Aspect)realasp;
						if (a.getPlanetid() > 0 &&
								a.getPlanetid() != planet.getId() && a.getPlanetid() != planet2.getId())
							continue;
						if (a.isAspect(res)) {
							SkyPointAspect aspect = new SkyPointAspect();
							aspect.setSkyPoint1(planet);
							aspect.setSkyPoint2(planet2);
							aspect.setScore(CalcUtil.roundTo(res, 2));
							aspect.setAspect(a);
							aspect.setExact(a.isExact(res));
							aspect.setApplication(a.isApplication(res));
							aspectList.add(aspect);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void init(boolean mode) {}
}
