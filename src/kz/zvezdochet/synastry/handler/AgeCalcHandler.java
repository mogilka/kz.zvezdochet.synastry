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
import kz.zvezdochet.util.Configuration;

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

			Configuration conf = event.getConfiguration();
			Collection<Planet> planets1 = conf.getPlanets().values();
			List<Model> houses1 = conf.getHouses();

			Configuration conf2 = partner.getConfiguration();
			Collection<Planet> planets2 = conf2.getPlanets().values();
			List<Model> houses2 = conf2.getHouses();
			
			updateStatus("Расчёт дирекций на возраст", false);
			Planet selplanet = agePart.getPlanet();
			House selhouse = agePart.getHouse();
			AspectType selaspect = agePart.getAspect();
			if (null == selaspect)
				aspectype = null;
			else
				aspectype = selaspect.getCode();
			
			boolean reverse = agePart.getReverse();
			int initage = agePart.getInitialAge();
			int finage = agePart.getFinalAge() + 1;

			//инициализируем аспекты
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			for (int age = initage; age < finage; age++) {
				for (Planet selp : reverse ? planets2 : planets1) {
					//дирекции планеты к другим планетам
					if (null == selhouse) {
						if (selplanet != null && !selplanet.getId().equals(selp.getId()))
							continue;
						for (Planet selp2 : reverse ? planets1 : planets2)
							calc(selp, selp2, age, reverse);
					}
					//дирекции планеты к куспидам домов
					boolean housable = reverse ? partner.isHousable() : event.isHousable();
					if (housable) {
						for (Model model2 : reverse ? houses1 : houses2) {
							if (selhouse != null && !selhouse.getId().equals(model2.getId()))
								continue;
							House selp2 = (House)model2;
							calc(selp, selp2, age, reverse);
						}
					}
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
		    agePart.setData(aged);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param age возраст
	 * @param reverse признак расчёта планет партнёра
	 */
	private void calc(SkyPoint point1, SkyPoint point2, int age, boolean reverse) {
		try {
			//находим угол между точками космограммы с учетом возраста
			double one = makeAge(point1.getCoord(), age);
			double two = point2.getCoord();
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
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber() + ", " + age);
			e.printStackTrace();
		}
	}

	/**
	 * Преобразуем координату с учётом возраста
	 * @param k координата
	 * @param age возраст
	 * @return модифицированное значение координаты
	 */
	private double makeAge(double k, int age) {
		double res;
		double val = k + age;
		res = (val > 360) ? val - 360 : val;
		return res;
	}
}
