package kz.zvezdochet.synastry.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.analytics.bean.Category;
import kz.zvezdochet.analytics.bean.HouseSignText;
import kz.zvezdochet.analytics.bean.Numerology;
import kz.zvezdochet.analytics.bean.PlanetHouseText;
import kz.zvezdochet.analytics.bean.PlanetSignText;
import kz.zvezdochet.analytics.bean.Rule;
import kz.zvezdochet.analytics.bean.SynastryAspectText;
import kz.zvezdochet.analytics.bean.SynastryText;
import kz.zvezdochet.analytics.exporter.EventRules;
import kz.zvezdochet.analytics.exporter.EventStatistics;
import kz.zvezdochet.analytics.service.HouseSignService;
import kz.zvezdochet.analytics.service.NumerologyService;
import kz.zvezdochet.analytics.service.PlanetSignService;
import kz.zvezdochet.analytics.service.SynastryAspectService;
import kz.zvezdochet.analytics.service.SynastryHouseService;
import kz.zvezdochet.analytics.service.SynastrySignService;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.bean.YinYang;
import kz.zvezdochet.core.bean.ITextGender;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.core.util.StringUtil;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.service.ElementService;
import kz.zvezdochet.service.EventService;
import kz.zvezdochet.service.YinYangService;
import kz.zvezdochet.synastry.Activator;
import kz.zvezdochet.synastry.bean.Synastry;
import kz.zvezdochet.synastry.service.SynastryService;
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
	/**
	 * Тип гороскопа совместимости
	 * love|family|deal любовный|семейный|партнёрский
	 * TODO задавать кнопками выбора в интерфейсе
	 */
	private String doctype = "family";

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
		event.init(true);
		partner.init(true);

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

			//тип
			Map<String, String> map = new HashMap<String, String>() {
				private static final long serialVersionUID = 4739421822269120671L;
				{
			        put("love", "любовный");
			        put("family", "семейный");
			        put("deal", "деловой");
			    }
			};
			p = new Paragraph("Тип гороскопа: " + map.get(doctype), font);
	        p.setAlignment(Element.ALIGN_CENTER);
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
			doc.add(chapter);

			chapter = new ChapterAutoNumber("Ваш типаж");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Ваш типаж");
			chapter.add(p);
			chapter.add(new Paragraph("Типаж – это общая характеристика поколения людей, рождённых вблизи " + DateUtil.sdf.format(event.getBirth()), font));
			printPlanetSign(chapter, event);
			doc.add(chapter);

			chapter = new ChapterAutoNumber("Типаж партнёра");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Типаж партнёра");
			chapter.add(p);
			chapter.add(new Paragraph("Типаж – это общая характеристика поколения людей, рождённых вблизи " + DateUtil.sdf.format(partner.getBirth()), font));
			p = new Paragraph("Толкования данного раздела следует воспринимать так, как будто они адресованы не вам, а партнёру:", PDFUtil.getWarningFont());
			p.setSpacingBefore(20);
			p.setSpacingAfter(20);
			chapter.add(p);
			printPlanetSign(chapter, partner);
			doc.add(chapter);

			//совместимость характеров, любовная, сексуальная, коммуникативная, эмоциональная совместимость
			chapter = new ChapterAutoNumber("Общий типаж пары");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Общий типаж пары");
			chapter.add(p);
			chapter.add(new Paragraph("Типаж пары – это общая характеристика совместимости двух людей вашего типа: "
				+ "общая тенденция развития отношений такого человека, как вы, с таким человеком, как ваш партнёр", font));

			//совместимость по Зороастрийскому календарю
			printZoroastr(chapter, event, partner);

			//совместимость планет в знаках
			printSign(chapter, event, partner);

			//совместимость темпераментов
			printTemperament(chapter, event, partner);
			doc.add(chapter);

			//аспекты
			chapter = new ChapterAutoNumber("Совместимость");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Совместимость");
			chapter.add(p);
			chapter.add(new Paragraph("В предыдущих разделах была дана общая характеристика партнёров и примерная картина отношений между вами. "
				+ "Теперь речь пойдёт о том, как вы в реальности ведёте себя друг с другом независимо от описанных выше характеристик:", font));
			com.itextpdf.text.List ilist = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("как вы оба реагируете друг на друга", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("какие эмоции и чувства вы вызываете друг в друге своим поведением", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("как вы влияете на изменение поведения друг друга", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("с какими конкретно ситуациями вы сталкиваетесь", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("чем общение партнёра с вами отличается от его общения с другими людьми", font));
	        ilist.add(li);
	        chapter.add(ilist);

			chapter.add(new Paragraph("Ниже приведены положительные и отрицательные факторы ваших отношений. "
				+ "Не преувеличивайте описанный негатив, он имеет место в любых парах. "
				+ "Резкие выяснения отношений возможны только если негативные толкования указывают на высокий уровень критичности (далее по тексту это видно)", font));
			Synastry synastry = (Synastry)new SynastryService().find(event.getId(), partner.getId());
			if (synastry != null) {
				printAspect(chapter, synastry, "Позитив для вас", "POSITIVE", false);
				printAspect(chapter, synastry, "Негатив для вас", "NEGATIVE", false);
				chapter.add(Chunk.NEXTPAGE);
				printAspect(chapter, synastry, "Позитив для партнёра", "POSITIVE", true);
				printAspect(chapter, synastry, "Негатив для партнёра", "NEGATIVE", true);
			}
			doc.add(chapter);

			//дома
			chapter = new ChapterAutoNumber("Влияние партнёра на вас");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Влияние партнёра на вас");
			chapter.add(p);
			printPlanetHouses(chapter, event, partner);
			doc.add(chapter);

			chapter = new ChapterAutoNumber("Ваше влияние на партнёра");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Ваше влияние на партнёра");
			chapter.add(p);
			p = new Paragraph("Толкования данного раздела следует воспринимать так, как будто они адресованы не вам, а партнёру:", PDFUtil.getWarningFont());
			chapter.add(p);
			printPlanetHouses(chapter, partner, event);
			doc.add(chapter);

			//рекомендации
			if (doctype.equals("love")) {
				chapter = new ChapterAutoNumber("Рекомендуемые партнёры");
				chapter.setNumberDepth(0);
				p = new Paragraph();
				PDFUtil.printHeader(p, "Рекомендуемые партнёры");
				chapter.add(p);
				printHouseSign(chapter, event, false);
				printHouseSign(chapter, partner, true);
				//знаменитости
				printAkin(chapter, event, false);
				printAkin(chapter, partner, true);
				doc.add(chapter);
			}
			
			chapter = new ChapterAutoNumber("Диаграммы");
			chapter.setNumberDepth(0);

			p = new Paragraph();
			PDFUtil.printHeader(p, "Диаграммы");
			chapter.add(p);

			//координаты планет
			printCoords(chapter, event, partner, false);
			chapter.add(Chunk.NEXTPAGE);
			printCoords(chapter, event, partner, true);
			chapter.add(Chunk.NEXTPAGE);

			//TODO сравнение силы планет
//			printPlanetStrength(writer, chapter, event, partner);
//			chapter.add(Chunk.NEXTPAGE);

			//аспекты
			printChart(writer, chapter, synastry);
			chapter.add(Chunk.NEXTPAGE);
			printAspectTypes(writer, chapter, synastry);
			chapter.add(Chunk.NEXTPAGE);

			//стихии
			EventStatistics statistics = new EventStatistics(event.getConfiguration());
			EventStatistics statistics2 = new EventStatistics(partner.getConfiguration());
			statistics.getPlanetSigns(true);
			statistics2.getPlanetSigns(true);
			statistics.initPlanetDivisions();
			statistics2.initPlanetDivisions();
			printElements(writer, chapter, statistics, statistics2);
			chapter.add(Chunk.NEXTPAGE);

			//инь-ян
			printYinYang(writer, chapter, statistics, statistics2);
			doc.add(chapter);

			chapter = new ChapterAutoNumber("Сокращения");
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Сокращения");
			chapter.add(p);
			printAbbreviation(chapter);
			doc.add(chapter);
			doc.add(Chunk.NEWLINE);
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

			section.add(new Paragraph("Карта отношений — это совмещённый рисунок двух событий:", font));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("положение планет в момент вашего рождения", new Font(baseFont, 12, Font.NORMAL)));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("положение планет в момент рождения партнёра", new Font(baseFont, 12, Font.NORMAL)));
	        list.add(li);
			section.add(list);
			section.add(new Paragraph("Подробности в разделе «Координаты планет»", font));

			Font fontgray = new Font(baseFont, 12, Font.NORMAL, PDFUtil.FONTCOLORGRAY);
			section.add(new Paragraph("Сокращения и символы, использованные в тексте, описаны в конце документа", fontgray));
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
				String general[] = {"personality", "emotions", "contact", "feelings"};
				List<String> categories = new ArrayList<>(Arrays.asList(general));
				if (doctype.equals("love")) {
					String love[] = {"love", "family", "faithfulness", "sex"};
					categories.addAll(Arrays.asList(love));
				} else if (doctype.equals("deal")) {
					String deal[] = {"thinking", "work", "profession", "activity"};
					categories.addAll(Arrays.asList(deal));
				}
				for (Model model : event.getConfiguration().getPlanets()) {
					Planet planet = (Planet)model;
				    if (planet.isMain()) {
				    	List<PlanetSignText> list = service.find(planet, planet.getSign());
				    	if (list != null && list.size() > 0)
				    		for (PlanetSignText object : list) {
				    			Category category = object.getCategory();
				    			if (!categories.contains(category.getCode()))
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
				String[] general = {"Sun", "Mercury"};
				List<String> planets = new ArrayList<>(Arrays.asList(general));
				if (doctype.equals("love")) {
					String love[] = {"Venus", "Mars"};
					planets.addAll(Arrays.asList(love));
				}
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
					    	Section section = PDFUtil.printSection(chapter, planet1.getSynastry());
					    	section.add(new Chunk("Мужчина-" + planet1.getSign().getShortname() +
					    		" + Женщина-" + planet2.getSign().getShortname(), fonth5));
//			    			if (term) {
//			    				section.add(new Chunk(planet.getMark("sign"), fonth5));
//			    				section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(new Chunk(" " + planet.getName() + " в созвездии " + planet.getSign().getName() + " ", fonth5));
//			    				section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(Chunk.NEWLINE);
//			    			}
		    				section.add(new Paragraph(PDFUtil.html2pdf(object.getText())));
					    }
					}
				}
			}
			chapter.add(Chunk.NEXTPAGE);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация аспектов
	 * @param chapter раздел
	 * @param event партнёр
	 */
	private void printAspect(Chapter chapter, Synastry synastry, String title, String aspectType, boolean reverse) {
		try {
			Section section = PDFUtil.printSection(chapter, title);
			if (aspectType.equals("NEGATIVE")) {
				Paragraph p = new Paragraph("В данном разделе описаны ваши с партнёром качества, которые проявляются в конфликтных ситуациях.", font);
				p.setSpacingAfter(10);
				section.add(p);
			}
			if (reverse) {
				Paragraph p = new Paragraph("Толкования данного раздела следует воспринимать так, как будто они адресованы не вам, а партнёру. "
					+ "Если в тексте упомянута ролевая пара типа «учитель — ученик», то первая роль обозначает партнёра, а вторая – вас (не наоборот)", PDFUtil.getWarningFont());
				p.setSpacingAfter(20);
				section.add(p);
			}
			Font bold = new Font(baseFont, 12, Font.BOLD);

			SynastryAspectService service = new SynastryAspectService();
			List<SkyPointAspect> aspects = synastry.getAspects();
//			Event event = reverse ? synastry.getPartner() : synastry.getEvent();

			for (SkyPointAspect aspect : aspects) {
				Planet planet1 = reverse ? (Planet)aspect.getSkyPoint2() : (Planet)aspect.getSkyPoint1();
				if (!planet1.isMain())
					continue;
				long asplanetid = aspect.getAspect().getPlanetid();
				if (asplanetid > 0 && asplanetid != planet1.getId())
					continue;
				Planet planet2 = reverse ? (Planet)aspect.getSkyPoint1() : (Planet)aspect.getSkyPoint2();
				if (planet1.getNumber() >= planet2.getNumber())
					continue;

				AspectType type = aspect.checkType(true);
				boolean match = false;
				//аспект соответствует заявленному (негативному или позитивному)
				if (type.getCode().equals(aspectType))
					match = true;

				if (match) {
//    				List<Model> planets = event.getConfiguration().getPlanets();
//    				int pindex = planets.indexOf(planet1);
//    				Planet aspl1 = (Planet)planets.get(pindex);
//    				List<Model> planets2 = partner.getConfiguration().getPlanets();
//    				pindex = planets.indexOf(planet2);
//    				Planet aspl2 = (Planet)planets2.get(pindex);
//
//    				section.add(new Chunk(dict.getMark(aspl1, aspl2), fonth5));
					section.add(new Chunk(planet1.getShortName() + "-" + (reverse ? "Партнёр" : "Вы") + " " + 
						type.getSymbol() + " " + 
						planet2.getShortName() + "-" + (reverse ? "Вы" : "Партнёр"), fonth5));

					String code = aspect.getAspect().getCode();
					if (term) {
						section.add(new Chunk(" " + planet1.getSymbol(), PDFUtil.getHeaderAstroFont()));

	    				if (code.equals("CONJUNCTION") || code.equals("OPPOSITION"))
	    					section.add(new Chunk(aspect.getAspect().getSymbol(), PDFUtil.getHeaderAstroFont()));
	    				else
	    					section.add(new Chunk(type.getSymbol(), fonth5));

	    				section.add(new Chunk(planet2.getSymbol(), PDFUtil.getHeaderAstroFont()));
					}

					List<Model> dicts = service.finds(aspect);
					for (Model model : dicts) {
						SynastryAspectText dict = (SynastryAspectText)model;
						if (dict != null) {
							if (dict.getRoles() != null)
								section.add(new Paragraph("Роли: «" + StringUtil.removeTags(dict.getRoles()) + "»", bold));

							if (code.equals("QUADRATURE"))
								section.add(new Paragraph("Уровень критичности: высокий", PDFUtil.getWarningFont()));
							else if (code.equals("OPPOSITION"))
								section.add(new Paragraph("Уровень критичности: средний", PDFUtil.getWarningFont()));

							section.add(new Paragraph(StringUtil.removeTags(dict.getText()), font));
							printGender(section, dict);

							Rule rule = EventRules.ruleSynastryAspect(aspect, synastry.getPartner());
							if (rule != null) {
			    				section.add(Chunk.NEWLINE);
								section.add(PDFUtil.html2pdf(rule.getText()));
							}
							section.add(Chunk.NEWLINE);
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация таблицы координат планет и домов
	 * @param chapter раздел
	 * @param event первый партнёр
	 * @param event второй партнёр
	 */
	private void printCoords(Chapter chapter, Event event, Event partner, boolean reverse) {
		try {
			Section section = PDFUtil.printSection(chapter, reverse ? "Координаты планет партнёра" : "Координаты ваших планет");
			float fontsize = 10;
			Font font = new Font(baseFont, fontsize, Font.NORMAL, BaseColor.BLACK);
			section.add(new Paragraph("Планеты в знаках Зодиака и астрологических домах:", this.font));

	        PdfPTable table = new PdfPTable(5);
	        table.setSpacingBefore(10);

			PdfPCell cell = new PdfPCell(new Phrase("Градус планеты", font));
	        cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Планета", font));
	        cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Созвездие", font));
	        cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Градус дома", font));
	        cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Дом партнёра", font));
	        cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			int i = -1;
			List<Model> planets = reverse ? partner.getConfiguration().getPlanets() : event.getConfiguration().getPlanets();
			for (Model model : planets) {
				BaseColor color = (++i % 2 > 0) ? new BaseColor(255, 255, 255) : new BaseColor(230, 230, 250);
				Planet planet = (Planet)model;

				cell = new PdfPCell(new Phrase(CalcUtil.roundTo(planet.getCoord(), 2) + "°", font));
		        cell.setBorder(PdfPCell.NO_BORDER);
		        cell.setBackgroundColor(color);
				table.addCell(cell);

				Color scolor = planet.getColor();
				cell = new PdfPCell();
		        String descr = "";
				if (planet.isLord())
					descr += "влд ";

				if (planet.isKing())
					descr += "крл ";

				if (planet.isBelt())
					descr += "пояс ";
				else if (planet.isKernel())
					descr += "ядро ";

				if (planet.isPerfect())
					descr += "грм ";
				else if (planet.isDamaged())
					descr += "прж ";

				if (planet.isLilithed())
					descr += "сбз ";

				if (planet.isBroken() || planet.inMine())
					descr += "слб ";

				if (planet.isRetrograde())
					descr += "R";

				cell.addElement(new Phrase(planet.getName() + (descr.length() > 0 ? " (" + descr + ")" : ""), new Font(baseFont, fontsize, Font.NORMAL, new BaseColor(scolor.getRed(), scolor.getGreen(), scolor.getBlue()))));
		        cell.setBorder(PdfPCell.NO_BORDER);
		        cell.setBackgroundColor(color);
		        table.addCell(cell);

				Sign sign = planet.getSign();
				scolor = sign.getElement().getDimColor();
		        cell = new PdfPCell();
		        descr = "";
				if (planet.isSignHome())
					descr = "(обт)";
				else if (planet.isSignExaltated())
					descr = "(экз)";
				else if (planet.isSignDeclined())
					descr = "(пдн)";
				else if (planet.isSignExile())
					descr = "(изг)";

				cell.addElement(new Phrase(sign.getName() + " " + descr, new Font(baseFont, fontsize, Font.NORMAL, new BaseColor(scolor.getRed(), scolor.getGreen(), scolor.getBlue()))));
		        cell.setBorder(PdfPCell.NO_BORDER);
		        cell.setBackgroundColor(color);
				table.addCell(cell);

				//определяем, в каком доме партнёра находится планета
				List<Model> houses = reverse ? event.getConfiguration().getHouses() : partner.getConfiguration().getHouses();
				if (houses != null && houses.size() > 0) {
					for (int j = 0; j < houses.size(); j++) {
						House house = (House)houses.get(j);
						double pcoord = planet.getCoord();
						Double hmargin = (j == houses.size() - 1) ?
							((House)houses.get(0)).getCoord() : 
							((House)houses.get(j + 1)).getCoord();
						double[] res = CalcUtil.checkMarginalValues(house.getCoord(), hmargin, pcoord);
						hmargin = res[0];
						pcoord = res[1];
						//если градус планеты находится в пределах куспидов
						//текущей и предыдущей трети домов,
						//запоминаем, в каком доме находится планета
						if (Math.abs(pcoord) < hmargin & 
								Math.abs(pcoord) >= house.getCoord())
							planet.setHouse(house);
					}
				}

				House house = planet.getHouse();
				if (null == house) {
					cell = new PdfPCell();
			        cell.setBorder(PdfPCell.NO_BORDER);
			        cell.setBackgroundColor(color);
					table.addCell(cell);
	
					cell = new PdfPCell();
			        cell.setBorder(PdfPCell.NO_BORDER);
			        cell.setBackgroundColor(color);
					table.addCell(cell);
				} else {					
					cell = new PdfPCell(new Phrase(CalcUtil.roundTo(house.getCoord(), 2) + "°", font));
			        cell.setBorder(PdfPCell.NO_BORDER);
			        cell.setBackgroundColor(color);
					table.addCell(cell);
	
					scolor = house.getElement().getDimColor();
					cell = new PdfPCell();
			        descr = "";
					if (planet.isHouseHome())
						descr = "(обт)";
					else if (planet.isHouseExaltated())
						descr = "(экз)";
					else if (planet.isHouseDeclined())
						descr = "(пдн)";
					else if (planet.isHouseExile())
						descr = "(изг)";
	
					cell.addElement(new Phrase(house.getName() + " " + descr, new Font(baseFont, fontsize, Font.NORMAL, new BaseColor(scolor.getRed(), scolor.getGreen(), scolor.getBlue()))));
			        cell.setBorder(PdfPCell.NO_BORDER);
			        cell.setBackgroundColor(color);
					table.addCell(cell);
				}
			}
			section.add(table);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация инь-ян
	 * @param writer обработчик генерации документа
	 * @param chapter раздел
	 * @param statistics объект статистики первого партнёра
	 * @param statistics объект статистики второго партнёра
	 */
	private void printYinYang(PdfWriter writer, Chapter chapter, EventStatistics statistics, EventStatistics statistics2) {
		try {
			Section section = PDFUtil.printSection(chapter, "Мужское и женское начало");
			
			Map<String, Double> planetMap = statistics.getPlanetYinYangs();
			Map<String, Double> planetMap2 = statistics2.getPlanetYinYangs();

			Bar[] bars = new Bar[planetMap.size() + planetMap2.size()];
			Iterator<Map.Entry<String, Double>> iterator = planetMap.entrySet().iterator();
			int i = -1;
			YinYang yinyang = null;
			double score = 0.0;
			YinYangService service = new YinYangService();
		    while (iterator.hasNext()) {
		    	Entry<String, Double> entry = iterator.next();
		    	Bar bar = new Bar();
		    	YinYang element = (YinYang)service.find(entry.getKey());
		    	bar.setName(element.getDiaName());
		    	bar.setValue(entry.getValue() * (-1));
		    	bar.setColor(element.getColor());
		    	bar.setCategory("Вы");
		    	bars[++i] = bar;
		    	//определяем наиболее выраженный элемент
		    	if (entry.getValue() > score) {
		    		score = entry.getValue();
		    		yinyang = element;
		    	}
		    }

			iterator = planetMap2.entrySet().iterator();
			i = planetMap.size() - 1;
			YinYang yinyang2 = null;
			score = 0.0;
		    while (iterator.hasNext()) {
		    	Entry<String, Double> entry = iterator.next();
		    	Bar bar = new Bar();
		    	YinYang element = (YinYang)service.find(entry.getKey());
		    	bar.setName(element.getDiaName());
		    	bar.setValue(entry.getValue());
		    	bar.setColor(element.getColor());
		    	bar.setCategory("Партнёр");
		    	bars[++i] = bar;
		    	//определяем наиболее выраженный элемент
		    	if (entry.getValue() > score) {
		    		score = entry.getValue();
		    		yinyang2 = element;
		    	}
		    }

		    if (yinyang != null && yinyang2 != null) {
		    	YinYang y = (YinYang)service.find(yinyang.getCode() + "-" + yinyang2.getCode());
		    	if (term)
		    		section.add(new Paragraph(y.getDescription(), fonth5));
		    	section.add(new Paragraph(StringUtil.removeTags(y.getText()), font));
		    }
		    section.add(Chunk.NEWLINE);
	        section.add(new Paragraph("Диаграмма показывает, насколько вы оба активны", font));
		    com.itextpdf.text.Image image = PDFUtil.printStackChart(writer, "Мужское и женское начало", "Аспекты", "Баллы", bars, 500, 150, true);
			section.add(image);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация стихий
	 * @param writer обработчик генерации документа
	 * @param chapter раздел
	 * @param statistics объект статистики первого партнёра
	 * @param statistics объект статистики второго партнёра
	 */
	private void printElements(PdfWriter writer, Chapter chapter, EventStatistics statistics, EventStatistics statistics2) {
		try {
			Section section = PDFUtil.printSection(chapter, "Темпераменты");

			Map<String, Double> planetMap = statistics.getPlanetElements();
			Map<String, Double> planetMap2 = statistics2.getPlanetElements();

			List<String> elements = new ArrayList<>();
			Bar[] bars = new Bar[planetMap.size() + planetMap2.size()];
			Iterator<Map.Entry<String, Double>> iterator = planetMap.entrySet().iterator();
			int i = -1;
			ElementService service = new ElementService();
		    while (iterator.hasNext()) {
		    	i++;
		    	Entry<String, Double> entry = iterator.next();
		    	if (!elements.contains(entry.getKey()))
		    		elements.add(entry.getKey());
		    	Bar bar = new Bar();
		    	kz.zvezdochet.bean.Element element = (kz.zvezdochet.bean.Element)service.find(entry.getKey());
		    	bar.setName(element.getDiaName());
		    	bar.setValue(entry.getValue() * (-1));
		    	bar.setColor(element.getColor());
		    	bar.setCategory("Ваш темперамент");
		    	bars[i] = bar;
		    }
		    
			iterator = planetMap2.entrySet().iterator();
			i = planetMap.size() - 1;
		    while (iterator.hasNext()) {
		    	i++;
		    	Entry<String, Double> entry = iterator.next();
		    	if (!elements.contains(entry.getKey()))
		    		elements.add(entry.getKey());
		    	Bar bar = new Bar();
		    	kz.zvezdochet.bean.Element element = (kz.zvezdochet.bean.Element)service.find(entry.getKey());
		    	bar.setName(element.getDiaName());
		    	bar.setValue(entry.getValue());
		    	bar.setColor(element.getColor());
		    	bar.setCategory("Темперамент партнёра");
		    	bars[i] = bar;
		    }

			//определение выраженной стихии
		    Object els[] = elements.toArray();
		    Arrays.sort(els);
		    kz.zvezdochet.bean.Element element = null;
		    for (Model model : service.getList()) {
		    	kz.zvezdochet.bean.Element e = (kz.zvezdochet.bean.Element)model;
		    	String[] codes = e.getCode().split("_");
		    	if (codes.length == elements.size()) {
			    	Arrays.sort(codes);
		    		boolean match = Arrays.equals(codes, els);
		    		if (match) {
		    			element = e;
		    			break;
		    		} else
		    			continue;
		    	}
		    }
		    if (element != null) {
		    	if (term)
		    		section.add(new Paragraph(element.getName(), fonth5));
		    	section.add(new Paragraph(StringUtil.removeTags(element.getSynastry()), font));
		    	section.add(Chunk.NEWLINE);
		    }

	        section.add(new Paragraph("Диаграмма показывает, на чём вы оба сконцентрированы, "
		        + "какие проявления для вас важны, необходимы и естественны", font));
		    com.itextpdf.text.Image image = PDFUtil.printStackChart(writer, "Темпераменты", "Аспекты", "Баллы", bars, 500, 0, true);
			section.add(image);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация силы планет
	 * @param chapter раздел
	 * @param event событие
	 */
	private void printPlanetStrength(PdfWriter writer, Chapter chapter, Event event, Event partner) {
		try {
		    String text = term ? "Соотношение силы планет" : "Соотношение силы качеств";
			Section section = PDFUtil.printSection(chapter, text);
		    text = term ? "Чем выше значение, тем легче и активнее планета выражает свои качества" : "Чем выше значение, тем легче и активнее проявляются качества";
	    	section.add(new Paragraph(text, font));

			List<Model> planets = event.getConfiguration().getPlanets();
			List<Model> planets2 = partner.getConfiguration().getPlanets();

		    Bar[] bars = new Bar[planets.size() + planets2.size()];
		    int i = -1;
		    for (Model model : planets) {
	    		Planet planet = (Planet)model;
		    	Bar bar = new Bar();
		    	bar.setName(term ? planet.getName() : planet.getShortName());
		    	bar.setValue(planet.getPoints());
				bar.setColor(planet.getColor());
				bar.setCategory(term ? "Ваши планеты" : "Ваши качества");
				bars[++i] = bar;
		    }

		    i = planets.size() - 1;
		    for (Model model : planets2) {
	    		Planet planet = (Planet)model;
		    	Bar bar = new Bar();
		    	bar.setName(term ? planet.getName() : planet.getShortName());
		    	bar.setValue(planet.getPoints() * (-1));
				bar.setColor(planet.getColor());
				bar.setCategory(term ? "Планеты партнёра" : "Качества партнёра");
				bars[++i] = bar;
		    }
		    com.itextpdf.text.Image image = PDFUtil.printStackChart(writer, "Соотношение силы качеств", "Планеты", "Баллы", bars, 500, 500, true);
			section.add(image);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация типов аспектов
	 * @param chapter раздел
	 * @param event событие
	 */
	private void printAspectTypes(PdfWriter writer, Chapter chapter, Synastry synastry) {
		try {
			synastry.getEvent().getConfiguration().initPlanetAspects();
			synastry.getPartner().getConfiguration().initPlanetAspects();
			List<SkyPointAspect> aspects = synastry.getAspects();

			//фильтрация списка типов аспектов
			List<Model> types = new AspectTypeService().getList();
			String[] codes = {
					"NEUTRAL", "NEGATIVE", "NEGATIVE_HIDDEN", "POSITIVE", "POSITIVE_HIDDEN", "CREATIVE", "KARMIC", "SPIRITUAL", "PROGRESSIVE"
			};

			List<Bar> items = new ArrayList<Bar>();
		    for (Model tmodel : types) {
		    	AspectType mtype = null; 
		    	AspectType type = (AspectType)tmodel;
		    	if (Arrays.asList(codes).contains(type.getCode())) {
		    		mtype = type;
		    	} else {
		    		AspectType ptype = type.getParentType();
		    		if (ptype != null && Arrays.asList(codes).contains(ptype.getCode()))
		    			mtype = type.getParentType();
		    	}
		    	if (null == mtype)
		    		continue;

		    	int value = 0;
		    	for (SkyPointAspect aspect : aspects) {
		    		if (aspect.getAspect().getType().getCode().equals(type.getCode()))
		    			value++;
		    	}
		    	if (0 == value)
		    		continue;
		    	Bar bar = new Bar();
		    	bar.setName(mtype.getName()/*.substring(0, 4)*/);
		    	bar.setValue(value);
				bar.setColor(mtype.getColor());
				bar.setCategory("Аспекты");
				items.add(bar);
		    }
		    Bar[] bars = items.toArray(new Bar[items.size()]);
		    com.itextpdf.text.Image image = PDFUtil.printBars(writer, "Аспекты отношений", "Аспекты", "Баллы", bars, 500, 300, false, false);
			Section section = PDFUtil.printSection(chapter, "Аспекты отношений");
			section.add(image);

			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Больше гармоничных аспектов — больше лёгкости", new Font(baseFont, 12, Font.NORMAL, BaseColor.RED)));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Больше творческих — больше свободы", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 51))));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Больше нейтральных — больше сфер жизни, где вы действуете неразделимо", new Font(baseFont, 12, Font.NORMAL, new BaseColor(255, 153, 51))));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Больше негармоничных — больше стрессовых ситуаций", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Больше скрытых — больше переживаний происходит за кулисами общения", new Font(baseFont, 12, Font.NORMAL, BaseColor.GRAY)));
	        list.add(li);
			
			li = new ListItem();
	        li.add(new Chunk("Больше кармических — больше тупиковых ситуаций, которые нужно преодолеть", new Font(baseFont, 12, Font.NORMAL, BaseColor.BLUE)));
	        list.add(li);
			
			li = new ListItem();
	        li.add(new Chunk("Больше прогрессивных — больше испытаний", new Font(baseFont, 12, Font.NORMAL, new BaseColor(51, 153, 153))));
	        list.add(li);
			
			li = new ListItem();
	        li.add(new Chunk("Чем больше духовных, тем более высокого уровня развития вы можете достичь в данном союзе", new Font(baseFont, 12, Font.NORMAL, BaseColor.MAGENTA)));
	        list.add(li);
			section.add(list);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация таблицы темпераментов
	 * @param chapter раздел
	 * @param event первый партнёр
	 * @param event второй партнёр
	 */
	private void printTemperament(Chapter chapter, Event event, Event partner) {
		try {
			Section section = PDFUtil.printSection(chapter, "Сравнение темпераментов");
			section.add(new Paragraph("Если цвет ваших темпераментов совпадает, значит в указанной сфере вы совместимы от природы "
					+ "и не нужно тратить энергию на притирку характеров. Но если при этом ключевые слова тоже совпадают, "
					+ "то вы быстро наскучите друг другу (в указанной сфере)", this.font));

			float fontsize = 10;
			Font font = new Font(baseFont, fontsize, Font.NORMAL);
			Font bold = new Font(baseFont, fontsize, Font.BOLD);

			List<Model> planets = event.getConfiguration().getPlanets();
			List<Model> planets2 = partner.getConfiguration().getPlanets();
			int PLNUM = 5;
			Planet[] items = new Planet[PLNUM];
			Planet[] items2 = new Planet[PLNUM];

			int i = -1;
			for (Model model : planets) {
				Planet planet = (Planet)model;
				if (planet.isMain())
					items[++i] = planet;
			}
			i = -1;
			for (Model model : planets2) {
				Planet planet = (Planet)model;
				if (planet.isMain())
					items2[++i] = planet;
			}

	        PdfPTable table = new PdfPTable(3);
	        table.setWidths(new float[] { 16, 42, 42 });
	        table.setSpacingBefore(20);

			PdfPCell cell = new PdfPCell(new Phrase(term ? "Планета" : "Сфера", font));
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Вы", font));
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Партнёр", font));
			table.addCell(cell);

			for (int j = 0; j < PLNUM; j++) {
				Planet planet = items[j];
				Planet planet2 = items2[j];

				cell = new PdfPCell(new Phrase(term ? planet.getName() : planet.getSynastry(), font));
				table.addCell(cell);

				table.addCell(printTemperamentCell(planet, font, bold));
				table.addCell(printTemperamentCell(planet2, font, bold));
			}
			section.add(table);
			section.add(Chunk.NEXTPAGE);

			//совместимость стихий
			section = PDFUtil.printSection(chapter, "Совместимость темпераментов для вас");
			for (int j = 0; j < PLNUM; j++) {
				Planet planet = items[j];
				Planet planet2 = items2[j];
				printTemperamentDescr(planet, planet2, font, section, false);
			}

			section = PDFUtil.printSection(chapter, "Совместимость темпераментов для партнёра");
			Paragraph p = new Paragraph("Толкования данного раздела следует воспринимать так, как будто они адресованы не вам, а партнёру", PDFUtil.getWarningFont());
			p.setSpacingAfter(20);
			section.add(p);
			for (int j = 0; j < PLNUM; j++) {
				Planet planet = items[j];
				Planet planet2 = items2[j];
				printTemperamentDescr(planet2, planet, font, section, true);
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация ячейки темперамента
	 * @param planet планета
	 * @param font шрифт
	 * @param bold жирный шрифт
	 * @return ячейка таблицы
	 */
	private PdfPCell printTemperamentCell(Planet planet, Font font, Font bold) {
		PdfPCell cell = new PdfPCell();
		Color color = planet.getSign().getElement().getLightColor();
		cell.setBackgroundColor(new BaseColor(color.getRed(), color.getGreen(), color.getBlue()));

        String descr = term
        	? planet.getSign().getName() + " (" + planet.getSign().getShortname() + ")"
        	: "Ключевое слово: " + planet.getSign().getShortname();
		cell.addElement(new Phrase(descr, bold));

        descr = "Модель поведения: " + planet.getSign().getElement().getYinYang().getName();
    	cell.addElement(new Phrase(descr, font));

        descr = term
	       	? "Стихия: " + planet.getSign().getElement().getName()
	       	: "Темперамент: " + planet.getSign().getElement().getTemperament();
	    descr += " (" + planet.getSign().getElement().getShortName() + ")";
		cell.addElement(new Phrase(descr, font));
		return cell;
	}

	/**
	 * Генерация совместимости темпераментов
	 * @param planet планета первого партнёра
	 * @param planet2 планета второго партнёра
	 * @param font шрифт
	 * @param section раздел
	 */
	private void printTemperamentDescr(Planet planet, Planet planet2, Font font, Section section, boolean reverse) {
		try {
			if (reverse && planet.getSign().getElement().getCode().equals(planet2.getSign().getElement().getCode()))
				return;

			String code = planet.getSign().getElement().getCode() + "_" + planet2.getSign().getElement().getCode();
			ElementService service = new ElementService();
			kz.zvezdochet.bean.Element element = (kz.zvezdochet.bean.Element)service.find(code);
		    if (element != null) {
	    		if (element.getSynastry() != null) {
			    	String text = term
			    		? planet.getName() + " (" + element.getName() + ")"
			    		: planet.getSynastry();
		    		section.add(new Paragraph(text, fonth5));
	    			section.add(new Paragraph(StringUtil.removeTags(element.getSynastry()), font));
	    		}
		    	section.add(Chunk.NEWLINE);
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация домов в знаках
	 * @param chapter раздел
	 * @param event событие
	 * @param houseMap карта домов
	 */
	private void printHouseSign(Chapter chapter, Event event, boolean reverse) {
		List<Model> houses = event.getConfiguration().getHouses();
		if (null == houses)
			return;
		try {
			Section section = PDFUtil.printSection(chapter, reverse ? "Потенциальный партнёр для вашего партнёра" : "Потенциальный партнёр для вас");
			if (reverse) {
				section.add(new Paragraph("Ниже приведён типаж человека, которого ваш партнёр притягивает к себе.", font));
				section.add(new Paragraph("Толкования данного раздела следует воспринимать так, как будто они адресованы не вам, а партнёру", PDFUtil.getWarningFont()));
			} else
				section.add(new Paragraph("Ниже приведён типаж человека, которого вы притягиваете к себе:", font));

			HouseSignService hservice = new HouseSignService();
			for (Model hmodel : houses) {
				House house = (House)hmodel;
				if (!house.getCode().equals("VII"))
					continue;
				if (!house.isExportOnSign())
					continue;

				Sign sign = SkyPoint.getSign(house.getCoord(), event.getBirthYear());
				HouseSignText dict = (HouseSignText)hservice.find(house, sign);
				if (dict != null) {
					if (null == section)
						section = PDFUtil.printSection(chapter, house.getName());
					if (term)
						section.add(new Paragraph(house.getDesignation() + " в созвездии " + sign.getName(), fonth5));
					else
						section.add(new Paragraph(house.getName() + " + " + sign.getShortname(), fonth5));
					section.add(new Paragraph(StringUtil.removeTags(dict.getText()), font));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация похожих по характеру знаменитостей
	 * @param date дата события
	 * @param cell тег-контейнер для вложенных тегов
	 */
	private void printAkin(Chapter chapter, Event event, boolean reverse) {
		try {
			List<Model> events = new EventService().findAkin(event, 1);
			if (events != null && events.size() > 0) {
				Section section = PDFUtil.printSection(chapter, reverse ? "Хорошая пара для партнёра" : "Хорошая пара для вас");
				section.add(new Paragraph(reverse ? "Известные люди, с которыми у вашего партнёра хорошая совместимость:" : "Известные люди, с которыми у вас хорошая совместимость:", font));
				Font fonta = PDFUtil.getLinkFont();

				com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
				for (Model model : events) {
					Event man = (Event)model;
					ListItem li = new ListItem();
			        Chunk chunk = new Chunk(DateUtil.formatDate(man.getBirth()), font);
			        li.add(chunk);

			        chunk = new Chunk("  ");
			        li.add(chunk);
			        chunk = new Chunk(man.getName(), fonta);
			        chunk.setAnchor(man.getUrl());
			        li.add(chunk);

			        chunk = new Chunk("   " + man.getDescription(), font);
			        li.add(chunk);
			        list.add(li);
				}
				section.add(list);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация совместимости по Зороастрийскому календарю
	 * @param date дата события
	 * @param cell тег-контейнер для вложенных тегов
	 */
	private void printZoroastr(Chapter chapter, Event event, Event partner) {
		try {
			NumerologyService service = new NumerologyService();
			int years = Math.abs(event.getBirthYear() - partner.getBirthYear());
			Numerology dict = (Numerology)service.find(years);
			if (dict != null) {
				Section section = PDFUtil.printSection(chapter, "Зороастрийский календарь");
				section.add(new Paragraph("Разница в годах цикла: " + CoreUtil.getAgeString(years), fonth5));
				section.add(new Paragraph(StringUtil.removeTags(dict.getZoroastrsyn()), font));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация планет в домах
	 * @param chapter раздел
	 * @param event событие
	 */
	private void printPlanetHouses(Chapter chapter, Event event, Event partner) {
		List<Model> houses = event.getConfiguration().getHouses();
		if (null == houses)
			return;
		List<Model> cplanets = partner.getConfiguration().getPlanets();
		try {
			SynastryHouseService service = new SynastryHouseService();
			boolean female = partner.isFemale();

			for (Model hmodel : houses) {
				House house = (House)hmodel;

				//Определяем количество планет в доме
				List<Planet> planets = new ArrayList<Planet>();
				for (Model pmodel : cplanets) {
					Planet planet = (Planet)pmodel;
					House phouse = null;
					for (int j = 0; j < houses.size(); j++) {
						House ehouse = (House)houses.get(j);
						double pcoord = planet.getCoord();
						Double hmargin = (j == houses.size() - 1) ?
							((House)houses.get(0)).getCoord() : 
							((House)houses.get(j + 1)).getCoord();
						double[] res = CalcUtil.checkMarginalValues(ehouse.getCoord(), hmargin, pcoord);
						hmargin = res[0];
						pcoord = res[1];
						//если градус планеты находится в пределах куспидов
						//текущей и предыдущей трети домов,
						//запоминаем, в каком доме находится планета
						if (Math.abs(pcoord) < hmargin & 
								Math.abs(pcoord) >= ehouse.getCoord())
							phouse = ehouse;
					}
					if (phouse != null && phouse.getId().equals(house.getId()))
						planets.add(planet);
				}
				//Создаем информационный блок, только если дом не пуст
				Section section = null;
				if (planets.size() > 0) {
					section = PDFUtil.printSection(chapter, house.getName());
			
					for (Planet planet : planets) {
						String sign = planet.isDamaged() || planet.isLilithed() ? "-" : "+";

						String mark = planet.getMark("house");
						if (mark.length() > 0) {
		    				section.add(new Chunk(mark, fonth5));
		    				section.add(new Chunk(planet.getSymbol() + " ", PDFUtil.getHeaderAstroFont()));
						}
		    			if (term) {
		    				section.add(new Chunk(" " + planet.getName() + " в " + house.getDesignation() + " доме", fonth5));
		    				section.add(Chunk.NEWLINE);
		    			} else
		    				section.add(new Chunk(planet.getShortName() + " " + sign + " " + house.getName(), fonth5));

						PlanetHouseText dict = (PlanetHouseText)service.find(planet, house, null);
						if (dict != null) {
							section.add(new Paragraph(StringUtil.removeTags(dict.getText()), font));
							printGender(section, dict);

							Rule rule = EventRules.rulePlanetHouse(planet, house, female);
							if (rule != null) {
								section.add(PDFUtil.html2pdf(rule.getText()));
								section.add(Chunk.NEWLINE);
							}
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Сокращения, использованные в документе
	 * @param chapter раздел
	 */
	private void printAbbreviation(Chapter chapter) {
		try {
			chapter.add(new Paragraph("Сокращения:", font));
			com.itextpdf.text.List ilist = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("\u2191 — сильная планета, адекватно проявляющая себя в гороскопе", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("\u2193 — ослабленная планета, чьё проявление связано с неуверенностью, стрессом и препятствиями", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("R — ретроградная планета, проявление качеств которой неочевидно и неуверенно", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("влд — владыка гороскопа, самая сильная планета", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("грм — гармоничная планета, способная преодолеть негатив", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("изг — планета в изгнании, что-то мешает проявлению её качеств", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("крл — король аспектов, самая позитивная планета", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("обт — планета в обители, проявляющая себя естественно и свободно", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("пдн — планета в падении (чувствует себя «не в своей тарелке»)", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("пояс — ущербная планета, чьи качества подавлены", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("прж — поражённая планета, несущая стресс и препятствия", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("сбз — планета-источник порока и соблазна", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("слб — слабо развитая планета", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("экз — планета в экзальтации, способная максимально проявить себя", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("ядро — планета-источник потенциала", font));
	        ilist.add(li);
	        chapter.add(ilist);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация гендерного толкования для данного типа гороскопа
	 * @param section подраздел
	 * @param dict справочник
	 * @throws IOException 
	 * @throws DocumentException 
	 */
	private void printGender(Section section, ITextGender dict) throws DocumentException, IOException {
		if (dict != null) {
			TextGender gender = dict.getGenderText(doctype);
			if (gender != null) {
				Paragraph p = new Paragraph(PDFUtil.getGenderHeader(gender.getType()), PDFUtil.getSubheaderFont());
				p.setSpacingBefore(10);
				section.add(p);
				section.add(new Paragraph(StringUtil.removeTags(gender.getText()), font));
			};
			section.add(Chunk.NEWLINE);
		}
	}

	/**
	 * Генерация аспектов
	 * @param writer обработчик генерации документа
	 * @param chapter раздел
	 * @param synastry синастрия
	 */
	private void printChart(PdfWriter writer, Chapter chapter, Synastry synastry) {
		try {
			Section section = PDFUtil.printSection(chapter, "Сферы совместимости");
			section.add(new Paragraph("Чем выше значение, тем более вы с партнёром совместимы в указанной сфере жизни", font));

			Map<String, Integer> map = new HashMap<String, Integer>() {
				private static final long serialVersionUID = 4739420822269120671L;
				{
			        put("Характеры", 0);
			        put("Общение", 0);
			        put("Любовь", 0);
			        put("Семья", 0);
			        put("Дружба", 0);
			        put("Секс", 0);
			        put("Сотрудничество", 0);
			        put("Соперничество", 0);
			    }
			};
			Map<String, String[]> planets = new HashMap<String, String[]>() {
				private static final long serialVersionUID = 4739420822269120672L;
				{
			        put("Sun", new String[] {"Характеры"});
			        put("Moon", new String[] {"Семья"});
			        put("Rakhu", new String[] {"Характеры"});
			        put("Kethu", new String[] {"Характеры"});
			        put("Mercury", new String[] {"Общение"});
			        put("Venus", new String[] {"Любовь"});
			        put("Mars", new String[] {"Секс", "Соперничество"});
			        put("Selena", new String[] {"Характеры"});
			        put("Lilith", new String[] {"Характеры"});
			        put("Jupiter", new String[] {"Характеры"});
			        put("Saturn", new String[] {"Характеры"});
			        put("Chiron", new String[] {"Сотрудничество"});
			        put("Uranus", new String[] {"Дружба"});
			        put("Neptune", new String[] {"Характеры"});
			        put("Pluto", new String[] {"Характеры"});
			        put("Proserpina", new String[] {"Характеры"});
			    }
			};
			List<SkyPointAspect> aspects = synastry.getAspects();
			for (SkyPointAspect aspect : aspects) {
				if (aspect.getAspect().getPoints() < 2)
					continue;
				Planet planet1 = (Planet)aspect.getSkyPoint1();
				long asplanetid = aspect.getAspect().getPlanetid();
				if (asplanetid > 0 && asplanetid != planet1.getId())
					continue;
				Planet planet2 = (Planet)aspect.getSkyPoint2();
				String pcode = planet1.getCode();
				String pcode2 = planet2.getCode();

				//оппозиция усиливает соперничество
				if (aspect.getAspect().getCode().equals("OPPOSITION")) {
					String cat = "Соперничество";
					int value = map.get(cat);
					map.put(cat, value - 1);
				}

				AspectType type = aspect.checkType(true);
				int points = type.getPoints();
				Iterator<Map.Entry<String, String[]>> iterator = planets.entrySet().iterator();
			    while (iterator.hasNext()) {
			    	Entry<String, String[]> entry = iterator.next();
			    	String key = entry.getKey();
			    	if (pcode.equals(key) || pcode2.equals(key)) {
			    		String categories[] = entry.getValue();
						for (String cat : categories) {
							int value = map.get(cat);
							value += points;
							map.put(cat, value);
						}
			    	}
		    	}
			}
		    Bar[] bars = new Bar[map.size()];
			Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
			int i = -1;
		    while (iterator.hasNext()) {
		    	Entry<String, Integer> entry = iterator.next();
		    	Bar bar = new Bar();
		    	bar.setName(entry.getKey());
		    	bar.setValue(entry.getValue());
//				bar.setColor(mtype.getColor());
				bar.setCategory("Сферы совместимости");
				bars[++i] = bar;
		    }
		    com.itextpdf.text.Image image = PDFUtil.printBars(writer, "Сферы совместимости", "Сферы совместимости", "Баллы", bars, 500, 300, false, false);
			section.add(image);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
