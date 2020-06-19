package kz.zvezdochet.synastry.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.ui.util.DialogUtil;
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
			event.initData(false);
			partner.initData(false);
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
	public void init(boolean mode) {
		List<Model> aspects = null;
		try {
			aspects = new AspectService().getList();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		List<SkyPointAspect> data = new ArrayList<SkyPointAspect>();
		makeAspects(event, partner, aspects, data);
		partner.setAspectList(data);
		makePlanets();
	}

	/**
	 * Дата создания
	 */
	private Date date;

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Расчёт аспектов
	 * @param first первый партнёр
	 * @param second второй партнёр
	 * @param aspects список аспектов
	 * @param data массив аспектов партнёров
	 */
	private void makeAspects(Event first, Event second, List<Model> aspects, List<SkyPointAspect> data) {
		Collection<Planet> trplanets = first.getPlanets().values();
		Collection<Planet> splanets = second.getPlanets().values();
		for (Planet trplanet : trplanets)
			for (Planet planet : splanets)
				calc(trplanet, planet, aspects, data);
	}

	/**
	 * Определение аспекта между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param aspects список аспектов
	 * @param data массив аспектов партнёров
	 */
	private void calc(SkyPoint point1, SkyPoint point2, List<Model> aspects, List<SkyPointAspect> data) {
		try {
			//находим угол между точками космограммы
			double one = point1.getLongitude();
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);

//			if (20 == point1.getId() && 19 == point2.getId())
//				System.out.println(point1.getCode() + " " + point2.getCode() + " = " + res);

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.getPoints() < 2)
					continue;

				//соединения Солнца не рассматриваем
				if (a.getPlanetid() > 0)
					continue;

				if (a.isAspect(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					data.add(aspect);
				}
			}
		} catch (Exception e) {
			DialogUtil.alertWarning(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}

	/**
	 * Настройки гороскопа
	 */
	private String options = "{\"zoroastr\":0}";

	public String getOptions() {
		return options;
	}
	public void setOptions(String options) {
		this.options = options;
	}

	/**
	 * Планеты первого партнёра
	 */
	private	List<Planet> planetList;
	/**
	 * Планеты второго партнёра
	 */
	private	List<Planet> planet2List;

	public List<Planet> getPlanetList() {
		return planetList;
	}
	public List<Planet> getPlanet2List() {
		return planet2List;
	}

	/**
	 * Расчёт аспектов
	 * @param first первый партнёр
	 * @param second второй партнёр
	 * @param aspects список аспектов
	 * @param data массив аспектов партнёров
	 */
	private void makePlanets() {
		planetList = new ArrayList<Planet>();
		if (partner.isHousable()) {
			Collection<Planet> planets = event.getPlanets().values();
			Map<Long, House> houses = partner.getHouses();
			for (Model hmodel : houses.values()) {
				House house = (House)hmodel;
				for (Planet planet : planets) {
					House phouse = null;
					for (House ehouse : houses.values()) {
						long h = (ehouse.getNumber() == houses.size()) ? 142 : ehouse.getId() + 1;
						House house2 = (House)houses.get(h);
						if (SkyPoint.getHouse(ehouse.getLongitude(), house2.getLongitude(), planet.getLongitude()))
							phouse = ehouse;
					}
					if (phouse != null && phouse.getId().equals(house.getId())) {
						planet.setData(house);
						planet.setDone(false);
						planetList.add(planet);
					}
				}
			}
		}
		planet2List = new ArrayList<Planet>();
		if (event.isHousable()) {
			Collection<Planet> planets = partner.getPlanets().values();
			Map<Long, House> houses = event.getHouses();
			for (Model hmodel : houses.values()) {
				House house = (House)hmodel;
				for (Planet planet : planets) {
					House phouse = null;
					for (House ehouse : houses.values()) {
						long h = (ehouse.getNumber() == houses.size()) ? 142 : ehouse.getId() + 1;
						House house2 = (House)houses.get(h);
						if (SkyPoint.getHouse(ehouse.getLongitude(), house2.getLongitude(), planet.getLongitude()))
							phouse = ehouse;
					}
					if (phouse != null && phouse.getId().equals(house.getId())) {
						planet.setData(house);
						planet.setDone(true);
						planet2List.add(planet);
					}
				}
			}
		}
	}
}
