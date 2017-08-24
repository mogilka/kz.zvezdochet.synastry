package kz.zvezdochet.synastry.handler;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.synastry.exporter.PDFExporter;
import kz.zvezdochet.synastry.part.SynastryPart;

/**
 * Экспорт синастрии
 * @author Nataly Didenko
 *
 */
public class ExportHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Экспорт синастрии", false);
			SynastryPart synastryPart = (SynastryPart)activePart.getObject();
			final Event partner = synastryPart.getPartner();
			final Event partner2 = (Event)synastryPart.getModel();
			updateStatus("Сохранение синастрии в файл", false);

			final Display display = Display.getDefault();
    		BusyIndicator.showWhile(display, new Runnable() {
    			@Override
    			public void run() {
    				new PDFExporter(display).generate(partner, partner2);
    			}
    		});
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
			updateStatus("Файл событий сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}		
}
