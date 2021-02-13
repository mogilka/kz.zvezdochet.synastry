package kz.zvezdochet.synastry.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.synastry.part.AgePart;

/**
 * Обработчик расчёта дирекций синастрии
 * @author Natalie Didenko
 */
public class AgeCalcHandler extends Handler {
	private List<SkyPointAspect> aged = null;
	private String aspectype;
	List<Model> aspects = null;
	Event event;
	Event partner;
	int initage, finage;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			aged = new ArrayList<SkyPointAspect>();
			AgePart agePart = (AgePart)activePart.getObject();
			if (!agePart.check(0)) return;
			event = agePart.getEvent();
			partner = agePart.getPartner();

			Collection<Planet> planets1 = event.getPlanets().values();
			Collection<House> houses1 = event.getHouses().values();

			Collection<Planet> planets2 = partner.getPlanets().values();
			Collection<House> houses2 = partner.getHouses().values();
			
			updateStatus("Расчёт дирекций на возраст", false);
			List<Planet> selplanets1 = new ArrayList<Planet>();
			List<Planet> selplanets2 = new ArrayList<Planet>();
			Planet selplanet = agePart.getPlanet();
			if (selplanet != null) {
				for (Planet planet : planets1) {
					if (selplanet.getId().equals(planet.getId())) {
						selplanets1.add(planet);
						break;
					}
				}
				for (Planet planet : planets2) {
					if (selplanet.getId().equals(planet.getId())) {
						selplanets2.add(planet);
						break;
					}
				}
			} else {
				selplanets1.addAll(planets1);
				selplanets2.addAll(planets2);
			}
			List<House> selhouses1 = new ArrayList<House>();
			House selhouse = agePart.getHouse();
			if (event.isHousable()) {
				if (selhouse != null)
					for (House house : houses1) {
						if (selhouse.getId().equals(house.getId()))
							selhouses1.add(house);
					}
				else
					selhouses1.addAll(houses1);
			}
			List<House> selhouses2 = new ArrayList<House>();
			if (partner.isHousable()) {
				if (selhouse != null)
					for (House house : houses2) {
						if (selhouse.getId().equals(house.getId()))
							selhouses2.add(house);
					}
				else
					selhouses2.addAll(houses2);
			}

			AspectType selaspect = agePart.getAspect();
			if (null == selaspect)
				aspectype = null;
			else
				aspectype = selaspect.getCode();
			
			initage = agePart.getAge();
			finage = initage + agePart.getYears() - 1;
			int initage2 = agePart.getAge2();

			//инициализируем аспекты
			try {
				aspects = new AspectService().getMajorList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			int i = -1;
			boolean housable2 = partner.isHousable(), housable1 = event.isHousable();
			for (int age = initage; age <= finage; age++) {
				for (Planet selp : planets1) {
					//дирекции планет первого партнёра к планетам второго
					if (null == selhouse) {
						boolean match1 = (null == selplanet);
						if (selplanet != null && selplanet.getId().equals(selp.getId()))
							match1 = true;

						for (Planet selp2 : planets2) {
							boolean match2 = (null == selplanet);
							if (selplanet != null && selp2.getId().equals(selplanet.getId()))
								match2 = true;
							if (match1 || match2)
								calc(selp, selp2, age, age, false);
						}
					}
				}
				//дирекции планет первого партнёра к куспидам домов второго
				if (housable2) {
					for (Planet selp : selplanets1)
						for (House selp2 : selhouses2)
							calc(selp, selp2, age, age, false);
				}

				int age2 = initage2 + (++i);
//				System.out.println(age + "-" + age2);
				for (Planet selp : planets2) {
					//дирекции планет второго партнёра к планетам первого
					if (null == selhouse) {
						boolean match1 = (null == selplanet);
						if (selplanet != null && selplanet.getId().equals(selp.getId()))
							match1 = true;

						for (Planet selp2 : planets1) {
							boolean match2 = (null == selplanet);
							if (selplanet != null && selp2.getId().equals(selplanet.getId()))
								match2 = true;
							if (match1 || match2)
								calc(selp, selp2, age, age2, true);
						}
					}
				}
				//дирекции планет второго партнёра к куспидам домов первого
				if (housable1) {
					for (Planet selp : selplanets2)
						for (House selp2 : selhouses1)
							calc(selp, selp2, age, age2, true);
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
		    agePart.setData(aged);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertWarning(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param age возраст первого партнёра
	 * @param age2 возраст текущего партнёра
	 * @param reverse признак расчёта планет партнёра
	 */
	private void calc(SkyPoint point1, SkyPoint point2, int age, int age2, boolean reverse) {
		try {
			//находим угол между точками космограммы с учетом возраста
			double one = CalcUtil.incrementCoord(point1.getLongitude(), age2, true);
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);

			//искусственно устанавливаем нарастающую оппозицию,
			//чтобы она синхронизировалась с соответствующим ей соединением в этом возрасте
			if (point2 instanceof House) {
				if (res >= 179 && res < 180)
					++res;
			} else if (point1.getCode().equals("Kethu") || point2.getCode().equals("Kethu")) {
				++res;
			}

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (aspectype != null && !aspectype.equals(a.getType().getCode()))
					continue;

				if (a.getPlanetid() > 0)
					continue;

				if (a.isExact(res)) {
					String acode = a.getCode();
                    if (acode.equals("OPPOSITION")) {
    	                if (point1.getCode().equals("Rakhu") || point2.getCode().equals("Rakhu"))
	                        continue;
    	                if (point1.getCode().equals("Kethu") || point2.getCode().equals("Kethu"))
	                        continue;
                    }

					if (point2 instanceof House && CalcUtil.compareAngles(one, two)) {
						++res;
						--age;
					}
					if (age < 0)
						continue;
					if (age < initage || age > finage)
						continue;

					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAge(age);
					aspect.setAspect(a);
					aspect.setRetro(reverse);
					aspect.setExact(true);
					aged.add(aspect);
				}
			}
		} catch (Exception e) {
			DialogUtil.alertWarning(point1.getNumber() + ", " + point2.getNumber() + ", " + age2);
			e.printStackTrace();
		}
	}
}
