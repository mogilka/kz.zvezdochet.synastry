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
			Planet selplanet = agePart.getPlanet();
			House selhouse = agePart.getHouse();
			AspectType selaspect = agePart.getAspect();
			if (null == selaspect)
				aspectype = null;
			else
				aspectype = selaspect.getCode();
			
			int initage = agePart.getAge();
			int initage2 = agePart.getAge2();
			int years = agePart.getYears() + 1;

			//инициализируем аспекты
			try {
				aspects = new AspectService().getMajorList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			for (int age = initage; age < initage + years; age++) {
				for (Planet selp : planets1) {
					//дирекции планет первого партнёра к планетам второго
					if (null == selhouse) {
						if (selplanet != null && !selplanet.getId().equals(selp.getId()))
							continue;
						for (Planet selp2 : planets2)
							calc(selp, selp2, age, age, false);
					}
					//дирекции планет первого партнёра к куспидам домов второго
					boolean housable = partner.isHousable();
					if (housable) {
						for (Model model2 : houses2) {
							if (selhouse != null && !selhouse.getId().equals(model2.getId()))
								continue;
							House selp2 = (House)model2;
							calc(selp, selp2, age, age, false);
						}
					}
				}

				int age2 = initage2 + years;
				for (Planet selp : planets2) {
					//дирекции планет второго партнёра к планетам первого
					if (null == selhouse) {
						if (selplanet != null && !selplanet.getId().equals(selp.getId()))
							continue;
						for (Planet selp2 : planets1)
							calc(selp, selp2, age, age2, true);
					}
					//дирекции планет второго партнёра к куспидам домов первого
					boolean housable = event.isHousable();
					if (housable) {
						for (Model model2 : houses1) {
							if (selhouse != null && !selhouse.getId().equals(model2.getId()))
								continue;
							House selp2 = (House)model2;
							calc(selp, selp2, age, age2, true);
						}
					}
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

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (aspectype != null && !aspectype.equals(a.getType().getCode()))
					continue;

				if (a.getPlanetid() > 0 && a.getPlanetid() != point1.getId())
					continue;

				if (a.isExact(res)) {
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
