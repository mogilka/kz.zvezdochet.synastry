package kz.zvezdochet.synastry.ui;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.widgets.Label;

/**
 * Композит синастрии планет в знаках партнеров
 * @author Natalie Didenko 
 */
public class SynastrySignComposite /*extends EditorComposite*/ {
	protected ComboViewer cvPlanet;
	protected ComboViewer cvSign1;
	protected ComboViewer cvSign2;
	protected Label lbPlanet;
	protected Label lbSign1;
	protected Label lbSign2;
/*
	@Override
	public View create(Composite parent) {
		group = new Group(parent, SWT.NONE);
		group.setText("");
		
		lbPlanet = new Label(group, SWT.NONE);
		lbPlanet.setText("Планета");
		cvPlanet = new ComboViewer(group, SWT.BORDER | SWT.READ_ONLY);
		new RequiredDecoration(lbPlanet, SWT.TOP | SWT.RIGHT);

		lbSign1 = new Label(group, SWT.NONE);
		lbSign1.setText("Знак планеты первого партнера");
		cvSign1 = new ComboViewer(group, SWT.BORDER | SWT.READ_ONLY);
		new RequiredDecoration(lbSign1, SWT.TOP | SWT.RIGHT);
		
		lbSign2 = new Label(group, SWT.NONE);
		lbSign2.setText("Знак планеты второго партнера");
		cvSign2 = new ComboViewer(group, SWT.BORDER | SWT.READ_ONLY);
		new RequiredDecoration(lbSign2, SWT.TOP | SWT.RIGHT);

		decorate();
		init(group);
		try {
			initControls();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		syncView();
		return this;
	}
	
	@Override
	protected void initControls() throws DataAccessException {
		cvPlanet.setContentProvider(new ArrayContentProvider());
		cvPlanet.setLabelProvider(new DictionaryLabelProvider());
		cvPlanet.setInput(new PlanetService().getList());

		SignService service = new SignService();
		cvSign1.setContentProvider(new ArrayContentProvider());
		cvSign1.setLabelProvider(new DictionaryLabelProvider());
		cvSign1.setInput(service.getList());

		cvSign2.setContentProvider(new ArrayContentProvider());
		cvSign2.setLabelProvider(new DictionaryLabelProvider());
		cvSign2.setInput(service.getList());
	}
	
	@Override
	protected void init(Composite composite) {
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(composite);
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvPlanet.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvSign1.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvSign2.getCombo());

		StateChangedListener listener = new StateChangedListener();
		cvPlanet.addSelectionChangedListener(listener);
		cvSign1.addSelectionChangedListener(listener);
		cvSign2.addSelectionChangedListener(listener);
	}
	
	@Override
	protected void syncView() {
		reset();
		if (model != null) {
			SynastryText dict = (SynastryText)model;
			if (dict.getPlanet() != null)
				cvPlanet.getCombo().setText(dict.getPlanet().getName());
			if (dict.getSign1() != null)
				cvSign1.getCombo().setText(dict.getSign1().getName());
			if (dict.getSign2() != null)
				cvSign2.getCombo().setText(dict.getSign2().getName());
		} 
	}
	
	@Override
	public void reset() {
		cvPlanet.setSelection(null);
		cvSign1.setSelection(null);
		cvSign2.setSelection(null);
	}
	
	@Override
	public void syncModel(int mode) {
		if (null == model) return;
		SynastryText dict = (SynastryText)model;
		IStructuredSelection selection = (IStructuredSelection)cvPlanet.getSelection();
		if (selection.getFirstElement() != null) 
			dict.setPlanet((Planet)selection.getFirstElement());
		selection = (IStructuredSelection)cvSign1.getSelection();
		if (selection.getFirstElement() != null) 
			dict.setSign1((Sign)selection.getFirstElement());
		selection = (IStructuredSelection)cvSign2.getSelection();
		if (selection.getFirstElement() != null) 
			dict.setSign2((Sign)selection.getFirstElement());
	}

	@Override
	public boolean check(int mode) {
		String msgBody = "";  //$NON-NLS-1$
		if (cvPlanet.getSelection().isEmpty())
			msgBody += lbPlanet.getText() + '\n';
		if (cvSign1.getSelection().isEmpty())
			msgBody += lbSign1.getText() + '\n';
		if (cvSign2.getSelection().isEmpty())
			msgBody += lbSign2.getText() + '\n';
		if (msgBody.length() > 0) {
			DialogUtil.alertWarning(GUIutil.SOME_FIELDS_NOT_FILLED + msgBody);
			return false;
		} else 
			return true;
	}*/
}
