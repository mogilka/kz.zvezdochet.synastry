package kz.zvezdochet.synastry.handler;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.synastry.part.SynastryPart;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик переключения карты синастрий.
 * По умолчанию отображаются планеты партнёра в карте человека.
 * Можно переключиться в режим планет человека в карте партнёра
 * @author Nataly Didenko
 */
public class SwitchHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Переключение карты синастрий", false);
			SynastryPart synastryPart = (SynastryPart)activePart.getObject();
			int mode = synastryPart.getModeCalc();
			mode = (0 == mode) ? 1 : 0;
			System.out.println("mode" + mode);
			synastryPart.onCalc(mode);
			updateStatus("Карта синастрий переключена", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
