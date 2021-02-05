package kz.zvezdochet.synastry.part;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.provider.DictionaryLabelProvider;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.direction.provider.TransitLabelProvider;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;

/**
 * Поиск дирекций на указанный возраст
 * @author Natalie Didenko
 */
public class AgePart extends ModelListView {
	private Spinner spAge;
	private Spinner spAge2;
	private Spinner spYears;
	private ComboViewer cvPlanet;
	private ComboViewer cvHouse;
	private ComboViewer cvAspect;
	private Button btTerm;

	@Inject
	public AgePart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}
	
	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(parent, SWT.NONE);
		grFilter.setText("Поиск");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Возраст 1");
		spAge = new Spinner(grFilter, SWT.BORDER);
		spAge.setMinimum(0);
		spAge.setMaximum(150);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Возраст 2");
		spAge2 = new Spinner(grFilter, SWT.BORDER);
		spAge2.setMinimum(0);
		spAge2.setMaximum(150);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Период");
		spYears = new Spinner(grFilter, SWT.BORDER);
		spYears.setMinimum(0);
		spYears.setMaximum(10);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Сфера жизни");
		cvPlanet = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);
		cvHouse = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Аспекты");
		cvAspect = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		btTerm = new Button(grFilter, SWT.BORDER | SWT.CHECK);
		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Термины");

		GridLayoutFactory.swtDefaults().numColumns(13).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spAge);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spYears);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvPlanet.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvHouse.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvAspect.getCombo());
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).
			grab(false, false).applyTo(btTerm);
	}

	@Override
	protected String[] initTableColumns() {
		return new String[] {
			"",
			"Точка 1",
			"Аспект",
			"Точка 2",
			"Направление",
			"Величина аспекта",
			"Знак Зодиака",
			"Дом",
			"Описание"
		};
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
	}

	private Event event;
	private Event partner;

	/**
	 * Поиск персоны, для которой делается прогноз
	 * @return персона
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * Инициализация персоны, для которой делается прогноз
	 * @param event персона
	 */
	public void setEvent(Event event) {
		this.event = event;
		int age = spAge.getSelection();
		if (age < 1)
			spAge.setSelection(event.getAge());
	}

	@Override
	protected void initControls() {
		try {
			super.initControls();
			cvPlanet.setContentProvider(new ArrayContentProvider());
			cvPlanet.setLabelProvider(new DictionaryLabelProvider());
			List<Model> list = new PlanetService().getList();
			Planet planet = new Planet();
			planet.setId(0L);
			list.add(0, planet);
			cvPlanet.setInput(list);

			cvHouse.setContentProvider(new ArrayContentProvider());
			cvHouse.setLabelProvider(new DictionaryLabelProvider());
			list = new HouseService().getList();
			House house = new House();
			house.setId(0L);
			list.add(0, house);
			cvHouse.setInput(list);

			cvAspect.setContentProvider(new ArrayContentProvider());
			cvAspect.setLabelProvider(new DictionaryLabelProvider());
			list = new AspectTypeService().getList();
			AspectType aspect = new AspectType();
			aspect.setId(0L);
			list.add(0, aspect);
			cvAspect.setInput(list);
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean check(int mode) {
		if (null == event) {
			DialogUtil.alertWarning("Персона не задана");
			return false;
		} else if (null == partner) {
			DialogUtil.alertWarning("Партнёр не задан");
			return false;
		}
		return true;
	}

	/**
	 * Возвращает выбранный возраст первого партнёра
	 * @return начальный возраст
	 */
	public int getAge() {
		return spAge.getSelection();
	}

	/**
	 * Возвращает выбранный возраст второго партнёра
	 * @return начальный возраст
	 */
	public int getAge2() {
		return spAge2.getSelection();
	}

	/**
	 * Возвращает выбранный период лет
	 * @return число лет прогноза
	 */
	public int getYears() {
		return spYears.getSelection();
	}

	/**
	 * Возвращает выбранную планету как сферу жизни
	 * @return планета
	 */
	public Planet getPlanet() {
		IStructuredSelection selection = (IStructuredSelection)cvPlanet.getSelection();
		if (selection.getFirstElement() != null) {
			Planet planet = (Planet)selection.getFirstElement();
			if (planet.getId() > 0)
				return planet;
		}
		return null;
	}

	/**
	 * Возвращает выбранный дом как сферу жизни
	 * @return астрологический дом
	 */
	public House getHouse() {
		IStructuredSelection selection = (IStructuredSelection)cvHouse.getSelection();
		if (selection.getFirstElement() != null) {
			House house = (House)selection.getFirstElement();
			if (house.getId() > 0)
				return house;
		}
		return null;
	}

	/**
	 * Возвращает выбранный тип аспекта
	 * @return тип аспекта
	 */
	public AspectType getAspect() {
		IStructuredSelection selection = (IStructuredSelection)cvAspect.getSelection();
		if (selection.getFirstElement() != null) {
			AspectType type = (AspectType)selection.getFirstElement();
			if (type.getId() > 0)
				return type;
		}
		return null;
	}

	@Override
	public Model createModel() {
		return null;
	}

	/**
	 * Инициализация партнёра
	 * @param event персона
	 */
	public void setPartner(Event event) {
		event.initData(false);
		this.partner = event;
	}

	/**
	 * Поиск партнёра
	 * @return персона
	 */
	public Event getPartner() {
		return partner;
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
