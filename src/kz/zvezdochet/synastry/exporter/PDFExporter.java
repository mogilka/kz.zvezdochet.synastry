package kz.zvezdochet.synastry.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.analytics.bean.Category;
import kz.zvezdochet.analytics.bean.PlanetSignText;
import kz.zvezdochet.analytics.bean.SynastryText;
import kz.zvezdochet.analytics.service.PlanetSignService;
import kz.zvezdochet.analytics.service.SynastrySignService;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.synastry.Activator;
import kz.zvezdochet.util.Cosmogram;

/**
 * Генератор PDF-файла для экспорта событий
 * @author Nataly Didenko
 *
 */
public class PDFExporter {
	/**
	 * Компонент рисования
	 */
	private Display display;
	/**
	 * Базовый шрифт
	 */
	private BaseFont baseFont;
	/**
	 * Вариации шрифтов
	 */
	private Font font, fonth5;
	/**
	 * Признак использования астрологических терминов
	 */
	private boolean term = false;

	public PDFExporter(Display display) {
		this.display = display;
		try {
			baseFont = PDFUtil.getBaseFont();
			font = PDFUtil.getRegularFont();
			fonth5 = PDFUtil.getHeaderFont();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация синастрии
	 * @param event первый партнёр
	 * @param partner второй партнёр
	 */
	public void generate(Event event, Event partner) {
		event.init();
		partner.init();
		saveCard(event, partner);
		Document doc = new Document();
		try {
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/synastry.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler(doc));
	        doc.open();

	        //metadata
	        PDFUtil.getMetaData(doc, "Гороскоп совместимости");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Общая информация");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Гороскоп совместимости");
			chapter.add(p);

			//первый партнёр
			String text = DateUtil.fulldtf.format(event.getBirth());
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Place place = event.getPlace();
			if (null == place)
				place = new Place().getDefault();
			text = (event.getZone() >= 0 ? "UTC+" : "") + event.getZone() +
				" " + (event.getDst() >= 0 ? "DST+" : "") + event.getDst() + 
				" " + place.getName() +
				" " + place.getLatitude() + "°" +
				", " + place.getLongitude() + "°";
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			//второй партнёр
			text = DateUtil.fulldtf.format(partner.getBirth());
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			place = partner.getPlace();
			if (null == place)
				place = new Place().getDefault();
			text = (partner.getZone() >= 0 ? "UTC+" : "") + partner.getZone() +
				" " + (partner.getDst() >= 0 ? "DST+" : "") + partner.getDst() + 
				" " + place.getName() +
				" " + place.getLatitude() + "°" +
				", " + place.getLongitude() + "°";
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Font fontgray = new Font(baseFont, 10, Font.NORMAL, PDFUtil.FONTCOLORGRAY);
			text = "Дата составления: " + DateUtil.fulldtf.format(new Date());
			p = new Paragraph(text, fontgray);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			p = new Paragraph();
	        p.setAlignment(Element.ALIGN_CENTER);
			p.setSpacingAfter(20);
	        p.add(new Chunk("Автор: ", fontgray));
	        Chunk chunk = new Chunk(PDFUtil.AUTHOR, new Font(baseFont, 10, Font.UNDERLINE, PDFUtil.FONTCOLOR));
	        chunk.setAnchor(PDFUtil.WEBSITE);
	        p.add(chunk);
	        chapter.add(p);

			chapter.add(new Paragraph("Прогноз содержит как позитивные, так и негативные аспекты отношений. "
				+ "Негатив - признак того, что вам необходимо переосмысление отношений с партнёром и мобилизация ресурсов для решения проблемы. "
				+ "А также это возможность смягчить напряжение, ведь вы будете знать о нём заранее. "
				+ "Не зацикливайтесь на негативе, используйте свои сильные стороны для достижения конструктивного партнёрства.", font));

			//космограмма
			printCard(doc, chapter);
			chapter.add(Chunk.NEXTPAGE);

			//экспресс-тест
//			printChart(writer, chapter, event, null);
			doc.add(chapter);

			chapter = new ChapterAutoNumber("Ваша характеристика");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Ваша характеристика");
			chapter.add(p);
			printPlanetSign(chapter, event);
			doc.add(chapter);

			chapter = new ChapterAutoNumber("Характеристика партнёра");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Характеристика партнёра");
			chapter.add(p);
			printPlanetSign(chapter, partner);
			doc.add(chapter);


			//совместимость характеров
			//любовная совместимость
			//сексуальная совместимость
			chapter = new ChapterAutoNumber("Общий типаж пары");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Общий типаж пары");
			chapter.add(p);
			printSign(chapter, event, partner);

			//позитивные аспекты для вас
			//для партнёра
			//негативные для вас
			//для партнёра
			chapter = new ChapterAutoNumber("Совместимость");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Совместимость");
			chapter.add(p);
			printAspect(chapter, event, partner);

			//влияние партнёра на вашу жизнь
			//ваше влияние на жизнь партнёра
			
			//мужское и женское начало
			//темпераменты
			


			doc.add(chapter);

			
			if (term) {
//				chapter = new ChapterAutoNumber("Сокращения");
//				chapter.setNumberDepth(0);
//				p = new Paragraph();
//				PDFUtil.printHeader(p, "Сокращения");
//				chapter.add(p);
//
//				chapter.add(new Paragraph("Раздел событий:", font));
//				list = new com.itextpdf.text.List(false, false, 10);
//				li = new ListItem();
//		        li.add(new Chunk("\u2191 — сильная планета, адекватно проявляющая себя в астрологическом доме", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("\u2193 — ослабленная планета, источник неуверенности, стресса и препятствий", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("обт — указанный астрологический дом является обителью планеты и облегчает ей естественное и свободное проявление", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("экз — указанный астрологический дом является местом экзальтации планеты, усиливая её проявления и уравновешивая слабые качества", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("пдн — указанный астрологический дом является местом падения планеты, где она чувствует себя «не в своей тарелке»", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("изг — указанный астрологический дом является местом изгнания планеты, ослабляя её проявления и усиливает негатив", font));
//		        list.add(li);
//		        chapter.add(list);
//
//		        chapter.add(Chunk.NEWLINE);
//				chapter.add(new Paragraph("Раздел личности:", font));
//				list = new com.itextpdf.text.List(false, false, 10);
//				li = new ListItem();
//		        li.add(new Chunk("\u2191 — усиленный аспект, проявляющийся ярче других аспектов указанных планет (хорошо для позитивных сочетаний, плохо для негативных)", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("\u2193 — ослабленный аспект, проявляющийся менее ярко по сравнению с другими аспектами указанных планет (плохо для позитивных сочетаний, хорошо для негативных)", font));
//		        list.add(li);
//		        chapter.add(list);
//				doc.add(chapter);
			}

			chapter = new ChapterAutoNumber("Диаграммы");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Диаграммы");
			chapter.add(p);

			text = "Диаграммы обобщают приведённую выше информацию и наглядно отображают сферы жизни, которые будут занимать вас в каждом конкретном возрасте.";
			chapter.add(new Paragraph(text, font));
	        chapter.add(Chunk.NEWLINE);

			doc.add(chapter);
	        doc.add(PDFUtil.printCopyright());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
	        doc.close();
		}
	}

	/**
	 * Сохранение космограммы в PNG-файл
	 * @param event первый партнёр
	 * @param partner второй партнёр
	 */
	private void saveCard(Event event, Event partner) {
		try {
		    Image image = new Image(display, Cosmogram.HEIGHT, Cosmogram.HEIGHT);
		    GC gc = new GC(image);
		    gc.setBackground(new Color(display, 254, 250, 248));
		    gc.fillRectangle(image.getBounds());
			new Cosmogram(event.getConfiguration(), partner.getConfiguration(), null, gc);
			ImageLoader loader = new ImageLoader();
		    loader.data = new ImageData[] {image.getImageData()};
		    try {
				String card = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/card.png").getPath();
			    loader.save(card, SWT.IMAGE_PNG);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		    image.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация космограммы
	 * @param doc документ
	 * @param chapter глава
	 */
	private void printCard(Document doc, Chapter chapter) {
		try {
			Section section = PDFUtil.printSection(chapter, "Карта отношений");

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/card.png").getPath();
			com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(filename);
			float side = 300f;
			image.scaleAbsolute(side, side);
			float x = (doc.right() - doc.left()) / 2 - (side / 2);
			image.setIndentationLeft(x);
			section.add(image);

			String text = "Карта отношений - это совмещение положения планет на небе в момент вашего рождения с планетами партнёра. Подробности в разделе «Координаты планет»";
			section.add(new Paragraph(text, font));

			Font fontgray = new Font(baseFont, 12, Font.NORMAL, PDFUtil.FONTCOLORGRAY);
			section.add(new Paragraph("Сокращения и символы, использованные в тексте, описаны в конце документа", fontgray));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация экспресс-теста
	 * @param writer обработчик генерации документа
	 * @param chapter раздел
	 * @param event событие
	 * @param statistics объект статистики
	 */
	private void printChart(PdfWriter writer, Chapter chapter, Event event, Event partner) {
		try {
//			Section section = PDFUtil.printSection(chapter, "Диаграмма отношений");
//			
//			Диаграмма показывает взаимную совместимость партнёров, не зависящую от социальных факторов
//			Чем больше значение, тем меньше напряжения партнёр создаёт в указанной сфере отношений
//			Если пара чисел в сумме даёт ноль или положительное число, то напряжение в указанной сфере можно уравновесить
//			Нулевое значение является скорей нейтральным и нормальным, чем негативным
//			
//			Map<String, Double> planetMap = statistics.getPlanetElements();
//			Map<String, Double> houseMap = statistics.getHouseElements();
//
//			String[] elements = new String[planetMap.size()];
//			Bar[] bars = new Bar[planetMap.size() + houseMap.size()];
//			Iterator<Map.Entry<String, Double>> iterator = planetMap.entrySet().iterator();
//			int i = -1;
//			ElementService service = new ElementService();
//		    while (iterator.hasNext()) {
//		    	i++;
//		    	Entry<String, Double> entry = iterator.next();
//		    	elements[i] = entry.getKey();
//		    	Bar bar = new Bar();
//		    	kz.zvezdochet.bean.Element element = (kz.zvezdochet.bean.Element)service.find(entry.getKey());
//		    	bar.setName(element.getDiaName());
//		    	bar.setValue(entry.getValue() * (-1));
//		    	bar.setColor(element.getColor());
//		    	bar.setCategory("Темперамент в сознании");
//		    	bars[i] = bar;
//		    }
//		    
//			//определение выраженной стихии
//		    kz.zvezdochet.bean.Element element = null;
//		    for (Model model : service.getList()) {
//		    	element = (kz.zvezdochet.bean.Element)model;
//		    	String[] codes = element.getCode().split("_");
//		    	if (codes.length == elements.length) {
//		    		boolean match = true;
//		    		for (String code : codes)
//		    			if (!Arrays.asList(elements).contains(code)) {
//		    				match = false;
//		    				break;
//		    			}
//		    		if (match)
//		    			break;
//		    		else
//		    			continue;
//		    	}
//		    }
//		    if (element != null) {
//		    	String text = element.getTemperament();
//		    	if (term)
//		    		text += " (" + element.getName() + ")";
//		    	section.add(new Paragraph(text, fonth5));
//		    	section.add(new Paragraph(StringUtil.removeTags(element.getText()), font));
//		    	PDFUtil.printGender(section, element, female, child);
//		    }
//
//			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
//			ListItem li = new ListItem();
//	        li.add(new Chunk("Категория \"Темперамент в сознании\" показывает вашу идеальную модель: "
//					+ "на чём мысленно вы сконцентрированы, какие проявления для вас важны, необходимы и естественны.", font));
//	        list.add(li);
//
//			li = new ListItem();
//	        li.add(new Chunk("Категория \"Темперамент в поступках\" показывает, "
//					+ "как меняются ваши приоритеты на событийном уровне, в социуме по сравнению с предыдущей моделью.", font));
//	        list.add(li);
//	        section.add(list);
//
//			iterator = houseMap.entrySet().iterator();
//			i = planetMap.size() - 1;
//		    while (iterator.hasNext()) {
//		    	i++;
//		    	Entry<String, Double> entry = iterator.next();
//		    	Bar bar = new Bar();
//		    	element = (kz.zvezdochet.bean.Element)service.find(entry.getKey());
//		    	bar.setName(element.getDiaName());
//		    	bar.setValue(entry.getValue());
//		    	bar.setColor(element.getColor());
//		    	bar.setCategory("Темперамент в поступках");
//		    	bars[i] = bar;
//		    }
//		    com.itextpdf.text.Image image = PDFUtil.printStackChart(writer, "Сравнение темпераментов", "Аспекты", "Баллы", bars, 500, 0, true);
//			section.add(image);
//			
//			Характеры, вкусы, мировоззрение, авторитет, духовная близость, возможность самовыражения
//			Эмоции, поддержка, забота, душевное и интуитивное понимание
//			Взаимопонимание, сотрудничество, умение договориться и найти общий язык, сходство мышления
//			Любовь, чувства, вдохновение, эстетические вкусы, оценка внешней привлекательности партнёра
//			Секс, физическое притяжение, мотивация, инициатива, здоровое соперничество
			
//			text = "Диаграммы в тексте обобщают информацию по каждому возрасту:";
//			chapter.add(new Paragraph(text, font));
//
//			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
//			ListItem li = new ListItem();
//	        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость.", font));
//	        list.add(li);
//
//			li = new ListItem();
//	        li.add(new Chunk("Показатели на нуле указывают на сбалансированность ситуации.", font));
//	        list.add(li);
//
//			li = new ListItem();
//	        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение.", font));
//	        list.add(li);
//	        chapter.add(list);

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация планет в знаках
	 * @param chapter раздел
	 * @param event партнёр
	 */
	private void printPlanetSign(Chapter chapter, Event event) {
		try {
			if (event.getConfiguration().getPlanets() != null) {
				event.getConfiguration().initPlanetSigns(true);
				PlanetSignService service = new PlanetSignService();
				String[] categories = {"personality", "emotions", "contact", "feelings", "love", "family", "faithfulness", "sex"};
				for (Model model : event.getConfiguration().getPlanets()) {
					Planet planet = (Planet)model;
				    if (planet.isMain()) {
				    	List<PlanetSignText> list = service.find(planet, planet.getSign());
				    	if (list != null && list.size() > 0)
				    		for (PlanetSignText object : list) {
				    			Category category = object.getCategory();
				    			if (!Arrays.asList(categories).contains(category.getCode()))
				    				continue;
				    			Section section = PDFUtil.printSection(chapter, category.getName());
				    			if (term) {
				    				section.add(new Chunk(planet.getMark("sign"), fonth5));
				    				section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
				    				section.add(new Chunk(" " + planet.getName() + " в созвездии " + planet.getSign().getName() + " ", fonth5));
				    				section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
				    				section.add(Chunk.NEWLINE);
				    			}
				    			if (!category.getCode().equals("personality"))
				    				section.add(PDFUtil.html2pdf(object.getText()));
				    			PDFUtil.printGender(section, object, event.isFemale(), event.isChild(), false);
				    		}
				    }
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация сравнения планет в знаках
	 * @param chapter раздел
	 * @param event партнёр
	 */
	private void printSign(Chapter chapter, Event event, Event partner) {
		try {
			//только для мужчины и женщины
			boolean female = event.isFemale();
			boolean female2 = partner.isFemale();
			if (female == female2)
				return;

			Event man = female ? partner : event;
			Event woman = female ? event : partner;

			if (woman.getConfiguration().getPlanets() != null && man.getConfiguration().getPlanets() != null) {
				SynastrySignService service = new SynastrySignService();
				String[] planets = {"Venus", "Mars"};
				for (String code : planets) {
					Planet planet1 = null;
					Planet planet2 = null;
					for (Model model : man.getConfiguration().getPlanets()) {
						Planet planet = (Planet)model;
		    			if (planet.getCode().equals(code)) {
		    				planet1 = planet;
		    				break;
		    			}
					}
					for (Model model : woman.getConfiguration().getPlanets()) {
						Planet planet = (Planet)model;
		    			if (planet.getCode().equals(code)) {
		    				planet2 = planet;
		    				break;
		    			}
					}
					if (planet1 != null && planet2 != null) {
				    	SynastryText object = service.find(planet1, planet1.getSign(), planet2.getSign());
				    	if (object != null) {
					    	Section section = PDFUtil.printSection(chapter, planet1.getShortName());
					    	section.add(new Chunk("Мужчина-" + planet1.getSign().getShortname() +
					    		" + Женщина-" + planet2.getSign().getShortname(), fonth5));
//			    			if (term) {
//			    				section.add(new Chunk(planet.getMark("sign"), fonth5));
//			    				section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(new Chunk(" " + planet.getName() + " в созвездии " + planet.getSign().getName() + " ", fonth5));
//			    				section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(Chunk.NEWLINE);
//			    			}
		    				section.add(PDFUtil.html2pdf(object.getText()));
					    }
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация аспектов
	 * @param chapter раздел
	 * @param event партнёр
	 */
	private void printAspect(Chapter chapter, Event event, Event partner) {
		try {
			//только для мужчины и женщины
			boolean female = event.isFemale();
			boolean female2 = partner.isFemale();
			if (female == female2)
				return;

			Event man = female ? partner : event;
			Event woman = female ? event : partner;

			if (woman.getConfiguration().getPlanets() != null && man.getConfiguration().getPlanets() != null) {
				SynastrySignService service = new SynastrySignService();
				String[] planets = {"Venus", "Mars"};
				for (String code : planets) {
					Planet planet1 = null;
					Planet planet2 = null;
					for (Model model : man.getConfiguration().getPlanets()) {
						Planet planet = (Planet)model;
		    			if (planet.getCode().equals(code)) {
		    				planet1 = planet;
		    				break;
		    			}
					}
					for (Model model : woman.getConfiguration().getPlanets()) {
						Planet planet = (Planet)model;
		    			if (planet.getCode().equals(code)) {
		    				planet2 = planet;
		    				break;
		    			}
					}
					if (planet1 != null && planet2 != null) {
				    	SynastryText object = service.find(planet1, planet1.getSign(), planet2.getSign());
				    	if (object != null) {
					    	Section section = PDFUtil.printSection(chapter, planet1.getShortName());
					    	section.add(new Chunk("Мужчина-" + planet1.getSign().getShortname() +
					    		" + Женщина-" + planet2.getSign().getShortname(), fonth5));
//			    			if (term) {
//			    				section.add(new Chunk(planet.getMark("sign"), fonth5));
//			    				section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(new Chunk(" " + planet.getName() + " в созвездии " + planet.getSign().getName() + " ", fonth5));
//			    				section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(Chunk.NEWLINE);
//			    			}
		    				section.add(PDFUtil.html2pdf(object.getText()));
					    }
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
