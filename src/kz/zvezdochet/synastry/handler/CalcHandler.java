package kz.zvezdochet.synastry.handler;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.synastry.part.SynastryPart;

/**
 * Расчёт синастрии
 * @author Natalie Didenko
 *
 */
public class CalcHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Расчёт синастрий", false);
			ICalculable synastryPart = (SynastryPart)activePart.getObject();
			synastryPart.onCalc(0);
			updateStatus("Карта синастрий сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}