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
import kz.zvezdochet.synastry.part.AgePart;
import kz.zvezdochet.synastry.part.SynastryPart;

/**
 * Обработчик отображения представления дирекций синастрии
 * @author Natalie Didenko
 *
 */
public class AgeHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			SynastryPart synastryPart = (SynastryPart)activePart.getObject();
			final Event event = synastryPart.getPartner();
			if (null == event) return;
			event.initData(false);
			final Event partner = (Event)synastryPart.getModel();
			partner.initData(false);
		
			MPart part = partService.findPart("kz.zvezdochet.synastry.part.age");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    AgePart agePart = (AgePart)part.getObject();
		    agePart.setEvent(event);
		    agePart.setPartner(partner);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			e.printStackTrace();
		}
	}
		
}