package kz.zvezdochet.synastry.handler;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.part.AspectPart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.synastry.part.SynastryPart;
import kz.zvezdochet.util.Configuration;

/**
 * Расчёт синастрических аспектов планет
 * @author Nataly Didenko
 *
 */
public class AspectHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			SynastryPart synPart = (SynastryPart)activePart.getObject();
			Event event = synPart.getPartner();
			if (null == event) return;
			Configuration conf = event.getConfiguration();
			if (null == conf) return; //TODO выдавать сообщение
			if (null == conf.getPlanets()) return; //TODO выдавать сообщение
			updateStatus("Инициализация планет первого партнёра", false);

			Event partner = (Event)synPart.getModel();
			if (null == partner) return;
			Configuration conf2 = partner.getConfiguration();
			if (null == conf2) return; //TODO выдавать сообщение
			if (null == conf2.getPlanets()) return; //TODO выдавать сообщение
			updateStatus("Инициализация планет второго партнёра", false);

			Collection<Planet> planets = conf.getPlanets().values();
			Collection<Planet> planets2 = conf2.getPlanets().values();
			int pcount = planets.size();
			Object[][] data = new Object[pcount][pcount + 1];
			//заполняем заголовки строк названиями планет и их координатами
			for (Planet planet : planets2)
				data[planet.getId().intValue()][0] = planet.getName() + " (" + CalcUtil.roundTo(planet.getCoord(), 1) + ")";

			//формируем массив аспектов планет
			List<Model> aspects = new AspectService().getList();
			for (Planet planet : planets) {
				for (Planet planet2 : planets) {
					double res = CalcUtil.getDifference(planet.getCoord(), planet2.getCoord());
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(planet);
					aspect.setSkyPoint2(planet2);
					aspect.setScore(CalcUtil.roundTo(res, 2));
					for (Model realasp : aspects) {
						Aspect a = (Aspect)realasp;
						if (a.isAspect(res)) {
							aspect.setAspect(a);
							continue;
						}
					}
					data[planet2.getId().intValue()][planet.getId().intValue()] = aspect;
				}
			}
			updateStatus("Расчёт аспектов завершён", false);
			MPart part = partService.findPart("kz.zvezdochet.part.aspect");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    AspectPart aspectPart = (AspectPart)part.getObject();
		    aspectPart.setConfiguration(conf);
		    aspectPart.setData(data);
			updateStatus("Таблица аспектов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
