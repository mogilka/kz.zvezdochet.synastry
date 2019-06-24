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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
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

	/**
	 * Поиск первого партнёра
	 * @return человек
	 */
	public Event getPartner() {
		return synpartner;
	}

	private CosmogramComposite cmpCosmogram;
	private CTabFolder folder;
	private Group grPlanets;
	private Group grHouses;
	private Group grAspectType;

	/**
	 * Режим расчёта синастрии.
	 * По умолчанию отображаются планеты партнёра в карте человека.
	 * 1 - режим планет человека в карте партнёра
	 */
	private int MODE_CALC = 0;

	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		Group grCosmogram = new Group(parent, SWT.NONE);
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
			item.setImage(tab.image);
			item.setControl(tab.control);
		}
		folder.pack();
		GridLayoutFactory.swtDefaults().applyTo(grCosmogram);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grCosmogram);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
		return null;
	}

	@Override
	protected String[] initTableColumns() {
		return new String[] {
			"Имя",
			"Дата" };
	}

	@Override
	public boolean check(int mode) throws Exception {
		return false;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Event event = (Event)element;
				switch (columnIndex) {
					case 0: return event.getName();
					case 1: return DateUtil.formatDateTime(event.getBirth());
				}
				return null;
			}
		};
	}

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer.getTable());
	}

	/**
	 * Перерисовка космограммы
	 * @param partner первый партнёр
	 * @param partner2 второй партнёр
	 */
	private void refreshCard(Event partner, Event partner2) {
		Map<String, Object> params = new HashMap<>();
		List<String> aparams = new ArrayList<String>();
		Map<String, String[]> types = AspectType.getHierarchy();
		for (Control control : grAspectType.getChildren()) {
			Button button = (Button)control;
			if (button.getSelection())
				aparams.addAll(Arrays.asList(types.get(button.getData("type"))));
		}
		params.put("aspects", aparams);
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
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.runner", "icons/configure.gif").createImage();
		Group group = new Group(folder, SWT.NONE);
		group.setText("Общие");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tab.control = group;
		tabs[0] = tab;

		//планеты
		tab = new Tab();
		tab.name = "Планеты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/planet.gif").createImage();
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
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/home.gif").createImage();
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
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect.gif").createImage();
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
				bt.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect/" + type.getImage()).createImage());
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
			folder.setSelection(1);
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
		} else
			folder.setSelection(0);
			
		//дома
		controls = grHouses.getChildren();
		table = (Table)controls[0];
		table.removeAll();
		if (partner != null) {
			int j = -1;
			for (Model base : partner.getHouses()) {
				++j;
				House house = (House)base;
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, house.getName());		
				item.setText(1, String.valueOf(house.getLongitude()));
				//дома партнёра
				if (partner2 != null) {
					house = (House)partner2.getHouses().get(j);
					item.setText(2, String.valueOf(house.getLongitude()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		}
	}
	
	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(container, SWT.NONE);
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
				if (event != null)
					addModel(event);
			}
		});
	}

	@Override
	public void onCalc(Object mode) {
		MODE_CALC = (int)mode;
		System.out.println("onCalc" + MODE_CALC);
		Event event = synpartner;
		Event event2 = (Event)getModel();
		event2.initData(false);
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
		//сразу сохраняем партнёра в базу
		Synastry synastry = new Synastry();
		synastry.setEvent(synpartner);
		synastry.setPartner((Event)model);
		try {
			new SynastryService().save(synastry);
		} catch (DataAccessException e) {
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
}
