package kz.zvezdochet.synastry.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.Tab;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.part.CosmogramComposite;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.provider.EventProposalProvider;
import kz.zvezdochet.provider.EventProposalProvider.EventContentProposal;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.synastry.bean.Synastry;
import kz.zvezdochet.synastry.service.SynastryService;

/**
 * Представление синастрии
 * @author Natalie Didenko
 *
 */
public class SynastryPart extends ModelListView implements ICalculable {
	@Inject
	public SynastryPart() {}

	/**
	 * Первый партнёр
	 */
	private Event synpartner;

	private CosmogramComposite cmpCosmogram;
	private CTabFolder folder;
	private Group grPlanets;
	private Group grHouses;
	private Group grAspectType;
	private Button btTerm;

	/**
	 * Режим расчёта синастрии.
	 * По умолчанию отображаются планеты партнёра в карте человека.
	 * 1 - режим планет человека в карте партнёра
	 */
	private int MODE_CALC = 0;

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	@Override
	protected void init(Composite parent) {
		super.init(parent);
		Group grCosmogram = new Group(sashForm, SWT.NONE);
		grCosmogram.setText("Космограмма");
		cmpCosmogram = new CosmogramComposite(grCosmogram, SWT.NONE);

		folder = new CTabFolder(grCosmogram, SWT.BORDER);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setSimple(false);
		folder.setUnselectedCloseVisible(false);
		Tab[] tabs = initTabs();
		for (Tab tab : tabs) {
			CTabItem item = new CTabItem(folder, SWT.CLOSE);
			item.setText(tab.name);
//			item.setImage(tab.image);
			item.setControl(tab.control);
		}
		folder.pack();
		GridLayoutFactory.swtDefaults().applyTo(grCosmogram);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grCosmogram);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
	}

	@Override
	protected String[] initTableColumns() {
		return new String[] {
			"ID",
			"Персона",
			"Дата",
			"Партнёр",
			"Дата" };
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Synastry model = (Synastry)element;
				switch (columnIndex) {
					case 0: return model.getId() != null ? model.getId().toString() : "";
					case 1: return model.getEvent().getName("ru");
					case 2: return DateUtil.formatDateTime(model.getEvent().getBirth());
					case 3: return model.getPartner().getName("ru");
					case 4: return DateUtil.formatDateTime(model.getPartner().getBirth());
				}
				return null;
			}
		};
	}

	/**
	 * Перерисовка космограммы
	 * @param partner первый партнёр
	 * @param partner2 второй партнёр
	 */
	private void refreshCard(Event partner, Event partner2) {
		Map<String, Object> params = new HashMap<>();
		List<String> aparams = new ArrayList<String>();
		Map<String, String[]> types = AspectType.getHierarchy(true);
		for (Control control : grAspectType.getChildren()) {
			Button button = (Button)control;
			if (button.getSelection())
				aparams.addAll(Arrays.asList(types.get(button.getData("type"))));
		}
		params.put("aspects", aparams);
		params.put("type", "synastry");
		Event event = (null == partner2) ? null : partner2;
		cmpCosmogram.paint(partner, event, params);
	}

	/**
	 * Инициализация первого партнёра
	 * @param event персона
	 */
	public void setPartner(Event event) {
//			if (event.getConfiguration() != null)
//				event.getConfiguration().initPlanetStatistics();
		try {
			synpartner = event;
			setData(new SynastryService().findPartners(event.getId()));
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Инициализация вкладок космограммы
	 * @return массив вкладок
	 */
	private Tab[] initTabs() {
		Tab[] tabs = new Tab[4];
		//настройки расчёта
		Tab tab = new Tab();
		tab.name = "Настройки";
		Image image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.runner", "icons/configure.gif").createImage();
		tab.image = image;
		image.dispose();
		Group group = new Group(folder, SWT.NONE);
		group.setText("Общие");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tab.control = group;
		tabs[0] = tab;

		//планеты
		tab = new Tab();
		tab.name = "Планеты";
		image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/planet.gif").createImage();
		tab.image = image;
		image.dispose();
		grPlanets = new Group(folder, SWT.NONE);
		Object[] titles = { "Планета", "Координата #1", "Координата #2"	};
		Table table = new Table(grPlanets, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grPlanets.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (Object title : titles) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grPlanets;
		GridLayoutFactory.swtDefaults().applyTo(grPlanets);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grPlanets);
		tabs[1] = tab;
		
		//дома
		tab = new Tab();
		tab.name = "Дома";
		image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/home.gif").createImage();
		tab.image = image;
		image.dispose();
		grHouses = new Group(folder, SWT.NONE);
		String[] titles2 = {"Дом", "Координата #1", "Координата #2"};
		table = new Table(grHouses, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grHouses.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (int i = 0; i < titles2.length; i++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText(titles2[i]);
		}
		tab.control = grHouses;
		GridLayoutFactory.swtDefaults().applyTo(grHouses);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grHouses);
		tabs[2] = tab;
		
		//аспекты
		tab = new Tab();
		tab.name = "Аспекты";
		image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect.gif").createImage();
		tab.image = image;
		image.dispose();
		grAspectType = new Group(folder, SWT.NONE);
		grAspectType.setLayout(new GridLayout());
		List<Model> types = new ArrayList<Model>();
		try {
			types = new AspectTypeService().getList();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		for (Model model : types) {
			AspectType type = (AspectType)model;
			if (type.getImage() != null) {
				final Button bt = new Button(grAspectType, SWT.BORDER | SWT.CHECK);
				bt.setText(type.getName());
				image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect/" + type.getImage()).createImage();
				bt.setImage(image);
				image.dispose();
				bt.setSelection(true);
				bt.setData("type", type.getCode());
				bt.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (bt.getSelection())
							onCalc(MODE_CALC);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {}
				});
			}
		}
		tab.control = grAspectType;
		tabs[3] = tab;
		return tabs;
	}

	/**
	 * Обновление вкладок
	 * @param partner первый партнёр
	 * @param partner2 второй партнёр
	 */
	private void refreshTabs(Event partner, Event partner2) {
		//планеты
		Control[] controls = grPlanets.getChildren();
		Table table = (Table)controls[0];
		table.removeAll();
		if (partner != null) {
			Collection<Planet> planets = partner.getPlanets().values();
			for (Planet planet : planets) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, planet.getName());
				item.setText(1, String.valueOf(planet.getLongitude()));
				//планеты партнёра
				if (partner2 != null) {
					planet = (Planet)partner2.getPlanets().get(planet.getId());
					item.setText(2, String.valueOf(planet.getLongitude()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		}
			
		//дома
		controls = grHouses.getChildren();
		table = (Table)controls[0];
		table.removeAll();
		if (partner != null) {
			for (House house : partner.getHouses().values()) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, house.getName());		
				item.setText(1, String.valueOf(house.getLongitude()));
				//дома партнёра
				if (partner2 != null) {
					House house2 = (House)partner2.getHouses().get(house.getId());
					item.setText(2, String.valueOf(house2.getLongitude()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		}
	}
	
	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(parent, SWT.NONE);
		grFilter.setText("Поиск");
		grFilter.setLayout(new GridLayout());
		grFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Text txSearch = new Text(grFilter, SWT.BORDER);
		new InfoDecoration(txSearch, SWT.TOP | SWT.LEFT);
		txSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txSearch.setFocus();

		EventProposalProvider proposalProvider = new EventProposalProvider(new Object[] {1}, new Object[] {0});
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txSearch, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null) {
					Synastry synastry = new Synastry();
					synastry.setEvent(synpartner);
					synastry.setPartner(event);
					addModel(synastry);
				}
			}
		});

		btTerm = new Button(grFilter, SWT.BORDER | SWT.CHECK);
		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Термины");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).
		grab(false, false).applyTo(btTerm);
	}

	@Override
	public void onCalc(Object mode) {
		MODE_CALC = (int)mode;
//		System.out.println("onCalc" + MODE_CALC);
		Synastry synastry = (Synastry)getModel();
		synastry.init(true);
		Event event = synastry.getEvent();
		Event event2 = synastry.getPartner();
		event2.setAspectList(synastry.getPartner().getAspectList()); //TODO устранить костыль, использовать одного партнёра а не двух разрозненных

		if (mode.equals(0)) {
			refreshCard(event, event2);
			refreshTabs(event, event2);
		} else {
			refreshCard(event2, event);
			refreshTabs(event2, event);
		}
	}

	@Override
	public void addModel(Model model) {
		super.addModel(model);
		try {
			//сразу сохраняем партнёра в базу
			Synastry synastry = (Synastry)model;
			synastry.save();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Возвращает режим представления для расчёта
	 * @return 0 - планеты человека в домах события,
	 * 1 - планеты события в домах человека
	 */
	public int getModeCalc() {
		return MODE_CALC;
	}

	@Override
	public Model createModel() {
		return null;
	}

	@Override
	public Model getModel(int mode, boolean sync) throws Exception {
		Synastry synastry = (Synastry)super.getModel();
		synastry.init(true);
		return synastry;
	}

	/**
	 * Определяем тип отчёта по выделению пункта "Астрологические термины":
	 * 	true - делаем отчёт гороскопа с указанием названий планет, аспектов и других терминов
	 * 	false - пишем человекопонятные заменители вместо терминов
	 * @return
	 */
	public boolean isTerm() {
		return btTerm.getSelection();
	}
}
