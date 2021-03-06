package kz.zvezdochet.synastry.handler;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.part.EventPart;
import kz.zvezdochet.synastry.part.SynastryPart;

/**
 * Обработчик открытия синастрий персоны
 * @author Natalie Didenko
 *
 */
public class SynastryHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_ASPECT_PLANET_PLANET, true);
			if (null == event) return;

			updateStatus("Открытие списка синастрий", false);
			MPart part = partService.findPart("kz.zvezdochet.synastry.part.list");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    SynastryPart synastryPart = (SynastryPart)part.getObject();
		    synastryPart.setPartner(event);
			updateStatus("Таблица синастрий сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertWarning(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
