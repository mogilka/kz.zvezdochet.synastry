package kz.zvezdochet.synastry.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.itextpdf.text.Rectangle;
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
import kz.zvezdochet.analytics.service.CategoryService;
import kz.zvezdochet.analytics.service.HouseSignService;
import kz.zvezdochet.analytics.service.NumerologyService;
import kz.zvezdochet.analytics.service.PlanetSignService;
import kz.zvezdochet.analytics.service.SynastryAspectService;
import kz.zvezdochet.analytics.service.SynastryHouseService;
import kz.zvezdochet.analytics.service.SynastrySignService;
import kz.zvezdochet.bean.Aspect;
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
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
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
	private String doctype = "love";
	/**
	 * Имена партнёров
	 */
	private String name1, name2;

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

		name1 = event.getCallname();
		name2 = partner.getCallname();

		saveCard(event, partner);
		Document doc = new Document();
		try {
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/synastry.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler(doc));
	        doc.open();

	        //metadata
	        PDFUtil.getMetaData(doc, "Парный гороскоп");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Общая информация");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Парный гороскоп", null);
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
			String text = name1 + " - ";
			text += DateUtil.fulldtf.format(event.getBirth());
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
			text = name2 + " - ";
			text += DateUtil.fulldtf.format(partner.getBirth());
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

			Font fontgray = PDFUtil.getAnnotationFont(false);
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

			chapter.add(new Paragraph("Гороскоп содержит как позитивные, так и негативные аспекты отношений. "
				+ "Негатив – признак того, что вам необходимо переосмысление отношений с партнёром и мобилизация ресурсов для решения проблемы. "
				+ "А также это возможность смягчить напряжение, ведь вы будете знать о нём заранее. "
				+ "Не зацикливайтесь на негативе, используйте свои сильные стороны для достижения конструктивного партнёрства", font));

			//космограмма
			printCard(doc, chapter);
			chapter.add(Chunk.NEXTPAGE);

			//общая диаграмма
			Synastry synastry = (Synastry)new SynastryService().find(event.getId(), partner.getId());
			printChart(writer, chapter, synastry);
			chapter.add(Chunk.NEXTPAGE);
			doc.add(chapter);

			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сравнение партнёров", null));
			chapter.setNumberDepth(0);
			printPlanetSign(doc, chapter, event, partner);
			doc.add(chapter);

			//совместимость характеров, любовная, сексуальная, коммуникативная, эмоциональная совместимость
			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Общий типаж пары", null));
			chapter.setNumberDepth(0);
			chapter.add(new Paragraph("Типаж пары – это общая тенденция развития отношений такого человека, как вы, с таким человеком, как ваш партнёр", PDFUtil.getWarningFont()));

			//совместимость по Зороастрийскому календарю
			printZoroastr(chapter, event, partner);
			chapter.add(Chunk.NEXTPAGE);

			//совместимость планет в знаках
			printSign(chapter, event, partner);
			doc.add(chapter);

			//темпераменты
			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Темпераменты", null));
			chapter.setNumberDepth(0);
			//стихии
			EventStatistics statistics = new EventStatistics(event.getConfiguration());
			EventStatistics statistics2 = new EventStatistics(partner.getConfiguration());
			statistics.getPlanetSigns(true);
			statistics2.getPlanetSigns(true);
			statistics.initPlanetDivisions();
			statistics2.initPlanetDivisions();
			printElements(writer, chapter, statistics, statistics2);
			chapter.add(Chunk.NEXTPAGE);
			//сравнение темпераментов
			printTemperament(doc, chapter, event, partner);
			doc.add(chapter);

			//аспекты
			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Совместимость", null));
			chapter.setNumberDepth(0);
			chapter.add(new Paragraph("В предыдущих разделах была дана общая характеристика каждого из вас и примерная картина отношений. "
				+ "Теперь речь пойдёт о том, как вы в реальности поведёте себя друг с другом независимо от описанных выше характеристик:", font));
			com.itextpdf.text.List ilist = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("как вы реагируете друг на друга", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("какие эмоции и чувства вызываете друг в друге", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("как влияете на изменение поведения друг друга", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("с какими конкретно ситуациями сталкиваетесь", font));
	        ilist.add(li);

			li = new ListItem();
	        li.add(new Chunk("чем ваше общение между собой отличается от общения с другими людьми", font));
	        ilist.add(li);
	        chapter.add(ilist);

			if (synastry != null)
				printAspects(doc, chapter, synastry);
			doc.add(chapter);

			//дома
			if (synastry.getEvent().isHousable() || synastry.getPartner().isHousable()) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Влияние друг на друга", null));
				chapter.setNumberDepth(0);
				chapter.add(new Paragraph("Этот раздел в меньшей степени рассказывает о том, как вы относитесь друг к другу, "
			    	+ "и в большей степени – о том, что произойдёт в реальности, как вы измените жизнь и восприятие друг друга. "
			    	+ "Для каждого описаны сферы жизни, в которых будет явно ощущаться влияние партнёра", font));
				printPlanetHouses(doc, chapter, synastry);
				doc.add(chapter);

				//рекомендации
				if (doctype.equals("love"))
					printAkins(doc, synastry);
			}

			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Диаграммы", null));
			chapter.setNumberDepth(0);

			//TODO сравнение силы планет
//			printPlanetStrength(writer, chapter, event, partner);
//			chapter.add(Chunk.NEXTPAGE);

			//инь-ян
			printYinYang(writer, chapter, statistics, statistics2);
			chapter.add(Chunk.NEXTPAGE);

			//аспекты
			printAspectTypes(writer, chapter, synastry);
			chapter.add(Chunk.NEXTPAGE);

			//координаты планет
			printCoords(chapter, event, partner, false);
			chapter.add(Chunk.NEXTPAGE);
			printCoords(chapter, event, partner, true);
			chapter.add(Chunk.NEXTPAGE);
			doc.add(chapter);

			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сокращения", null));
			chapter.setNumberDepth(0);
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
			section.add(new Paragraph("Сокращения и символы, использованные в тексте, описаны в конце документа", PDFUtil.getAnnotationFont(false)));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация планет в знаках
	 * @param doc документ
	 * @param chapter раздел
	 * @param event партнёр 1
	 * @param event партнёр 2
	 */
	private void printPlanetSign(Document doc, Chapter chapter, Event event, Event partner) {
		try {
			if (event.getConfiguration().getPlanets() != null)
				event.getConfiguration().initPlanetSigns(true);
			else
				return;

			if (partner.getConfiguration().getPlanets() != null)
				partner.getConfiguration().initPlanetSigns(true);
			else
				return;

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

			List<Model> cats = new CategoryService().getList();
			for (Model m : cats) {
    			Category category = (Category)m;
    			if (!categories.contains(category.getCode()))
    				continue;

				Section section = PDFUtil.printSection(chapter, category.getName());

   				Planet planet = event.getConfiguration().getPlanets().get(category.getObjectId());
   				Planet planet2 = partner.getConfiguration().getPlanets().get(category.getObjectId());
				Sign sign1 = planet.getSign();
				Sign sign2 = planet2.getSign();

				if (sign1.getId().equals(sign2.getId()))
   					section.add(new Paragraph("По критерию «" + category.getName() + "» вы с партнёром очень похожи", PDFUtil.getSuccessFont()));

		        PdfPTable table = new PdfPTable(2);
		        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
		        table.setLockedWidth(true);
		        table.setWidths(new float[] { 50, 50 });
		        table.setSpacingBefore(20);

				PdfPCell cell = null;
				Event[] events = new Event[] {event, partner};

    			for (Event e : events) {
    				cell = new PdfPCell(new Phrase(e.getCallname(), font));
    				PDFUtil.setCellBorderWidths(cell, 0, 0, .5F, 0);
    				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    				table.addCell(cell);
    			}
    			table.setHeaderRows(1);

				Phrase phrase = new Phrase();
				List<String> texts1 = new ArrayList<>();
				List<String> texts2 = new ArrayList<>();
				PlanetSignText text = service.find(category, sign1);
				if (text != null) {
//					if (term) {
//						section.add(new Chunk(planet.getMark("sign"), fonth5));
//						section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
//						section.add(new Chunk(" " + planet.getName() + " в созвездии " + planet.getSign().getName() + " ", fonth5));
//						section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
////			 		section.add(Chunk.NEWLINE);
//					}
		    		if (!category.getCode().equals("personality")) {
		    			String t = text.getText();
	    				if (sign1.getId().equals(sign2.getId())) {
	    					phrase = PDFUtil.removeTags(t, font);
		    				Phrase ph = new Phrase();
		    				ph.add(new Paragraph("Данная характеристика подходит вам обоим:", PDFUtil.getSuccessFont()));
		    				ph.add(Chunk.NEWLINE);
		    				ph.add(Chunk.NEWLINE);
		    				ph.add(phrase);
		       				cell = new PdfPCell(ph);
	       					cell.setBorder(Rectangle.NO_BORDER);
		       				cell.setColspan(2);
		       				table.addCell(cell);
		    			} else
		    				texts1 = PDFUtil.splitHtml(t);
		    		}
				}

				//если основные толкования планет в знаках у партнёров разные,
				//выводим их поочерёдно - порциями, которые вмещаются в ячейки
				PlanetSignText text2 = service.find(category, sign2);
				if (text2 != null) {
		    		if (!category.getCode().equals("personality")) {
	    				if (!sign1.getId().equals(sign2.getId()))
	    					texts2 = PDFUtil.splitHtml(text2.getText());
		    		}
				}
				int tcount = texts1.size() + texts2.size();
				if (tcount > 0) {
					for (int i = 0; i < tcount; i++) {
						phrase = new Phrase();
						if (texts1.size() > i)
							phrase = PDFUtil.removeTags(texts1.get(i), font);
						cell = new PdfPCell(phrase);
						PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
						table.addCell(cell);

						phrase = new Phrase();
						if (texts2.size() > i)
							phrase = PDFUtil.removeTags(texts2.get(i), font);
						cell = new PdfPCell(phrase);
						cell.setBorder(Rectangle.NO_BORDER);
						table.addCell(cell);
					}
				}

				//гендерные толкования
				if (text != null) {
	    			phrase = PDFUtil.printGenderCell(text, event.isFemale(), event.isChild(), false);
	   				cell = new PdfPCell(phrase);
					PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
					table.addCell(cell);
				}				
				if (text2 != null) {
	    			phrase = PDFUtil.printGenderCell(text2, partner.isFemale(), partner.isChild(), false);
	   				cell = new PdfPCell(phrase);
	   				cell.setBorder(Rectangle.NO_BORDER);
	   				table.addCell(cell);
				}				
				section.add(table);
				chapter.add(Chunk.NEXTPAGE);
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
				Collection<Planet> mplanets = man.getConfiguration().getPlanets().values();
				Collection<Planet> wplanets = woman.getConfiguration().getPlanets().values();
				for (String code : planets) {
					Planet planet1 = null;
					Planet planet2 = null;
					for (Planet planet : mplanets) {
		    			if (planet.getCode().equals(code)) {
		    				planet1 = planet;
		    				break;
		    			}
					}
					for (Planet planet : wplanets) {
		    			if (planet.getCode().equals(code)) {
		    				planet2 = planet;
		    				break;
		    			}
					}
					if (planet1 != null && planet2 != null) {
				    	SynastryText object = service.find(planet1, planet1.getSign(), planet2.getSign());
				    	if (object != null) {
					    	Section section = PDFUtil.printSection(chapter, planet1.getSynastry());
					    	section.add(new Chunk(man.getCallname() + "-" + planet1.getSign().getShortname() +
					    		" + " + woman.getCallname() + "-" + planet2.getSign().getShortname(), fonth5));
//			    			if (term) {
//			    				section.add(new Chunk(planet.getMark("sign"), fonth5));
//			    				section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(new Chunk(" " + planet.getName() + " в созвездии " + planet.getSign().getName() + " ", fonth5));
//			    				section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
//			    				section.add(Chunk.NEWLINE);
//			    			}
					    	if (object.getText() != null)
					    		section.add(new Paragraph(PDFUtil.removeTags(object.getText(), font)));
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
	 * @param doc документ
	 * @param chapter раздел
	 * @param synastry синастрия
	 */
	private void printAspects(Document doc, Chapter chapter, Synastry synastry) {
		try {
			List<SkyPointAspect> relative = new ArrayList<>();
			List<SkyPointAspect> positive1 = new ArrayList<>();
			List<SkyPointAspect> negative1 = new ArrayList<>();
			List<SkyPointAspect> positive2 = new ArrayList<>();
			List<SkyPointAspect> negative2 = new ArrayList<>();
			List<SkyPointAspect> aspects = synastry.getAspects();

			for (SkyPointAspect aspect : aspects) {
				Planet planet1 = (Planet)aspect.getSkyPoint1();
				if (!synastry.getEvent().isHousable() && planet1.getCode().equals("Moon"))
					continue;

				if (!planet1.isMain())
					continue;
				long asplanetid = aspect.getAspect().getPlanetid();
				if (asplanetid > 0 && asplanetid != planet1.getId())
					continue;
				Planet planet2 = (Planet)aspect.getSkyPoint2();
				if (!synastry.getPartner().isHousable() && planet2.getCode().equals("Moon"))
					continue;

				if (planet1.getNumber() >= planet2.getNumber())
					continue;

				AspectType type = aspect.checkType(true);
				if (type.getCode().equals("NEGATIVE"))
					negative1.add(aspect);
				else if (type.getCode().equals("POSITIVE"))
					positive1.add(aspect);
			}

			for (SkyPointAspect aspect : aspects) {
				Planet planet1 = (Planet)aspect.getSkyPoint2();
				if (!synastry.getEvent().isHousable() && planet1.getCode().equals("Moon"))
					continue;

				if (!planet1.isMain())
					continue;
				long asplanetid = aspect.getAspect().getPlanetid();
				if (asplanetid > 0 && asplanetid != planet1.getId())
					continue;
				Planet planet2 = (Planet)aspect.getSkyPoint1();
				if (!synastry.getPartner().isHousable() && planet2.getCode().equals("Moon"))
					continue;

				if (planet1.getNumber() >= planet2.getNumber())
					continue;

				AspectType type = aspect.checkType(true);
				if (type.getCode().equals("NEGATIVE")) {
					boolean rel = false;
					for (SkyPointAspect a : negative1) {
						if (a.getSkyPoint1().getId().equals(planet1.getId())
								&& a.getSkyPoint2().getId().equals(planet2.getId())) {
							relative.add(a);
							negative1.remove(a);
							rel = true;
							break;
						}
					}
					if (!rel)
						negative2.add(aspect);
				} else if (type.getCode().equals("POSITIVE")) {
					boolean rel = false;
					for (SkyPointAspect a : positive1) {
						if (a.getSkyPoint1().getId().equals(planet1.getId())
								&& a.getSkyPoint2().getId().equals(planet2.getId())) {
							relative.add(a);
							positive1.remove(a);
							rel = true;
							break;
						}
					}
					if (!rel)
						positive2.add(aspect);
				}
			}
			printAspectSection(doc, chapter, synastry, "POSITIVE", positive1, positive2);
			printAspectSection(doc, chapter, synastry, "NEGATIVE", negative1, negative2);
			printAspectSection(doc, chapter, synastry, "RELATIVE", relative, null);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация раздела аспектов
	 * @param doc документ
	 * @param chapter раздел
	 * @param synastry синастрия
	 * @param aspectType тип раздела
	 * @param list1 первый массив аспектов
	 * @param list2 второй массив аспектов
	 */
	private void printAspectSection(Document doc, Chapter chapter, Synastry synastry, String aspectType,
			List<SkyPointAspect> list1, List<SkyPointAspect> list2) {
		try {
			String title = "";
			if (aspectType.equals("POSITIVE"))
				title = "Позитив отношений";
			else if (aspectType.equals("NEGATIVE"))
				title = "Риски отношений";
			else if (aspectType.equals("RELATIVE")) {
				if (list1.isEmpty())
					return;
				title = "Взаимные аспекты отношений";
				//TODO объяснить, что такое общие аспекты
			}
			Section section = PDFUtil.printSection(chapter, title);
			SynastryAspectService service = new SynastryAspectService();
			Font bold = new Font(baseFont, 12, Font.BOLD);

			if (aspectType.equals("RELATIVE")) {
				section.add(new Paragraph("Здесь перечислены толкования, которые показывают, в чём вы относитесь к партнёру так же, как он к вам:", PDFUtil.getAnnotationFont(false)));
				for (SkyPointAspect aspect : list1) {
					AspectType type = aspect.checkType(true);
					Planet planet1 = (Planet)aspect.getSkyPoint1();
					Planet planet2 = (Planet)aspect.getSkyPoint2();

//    				List<Model> planets = event.getConfiguration().getPlanets();
//    				int pindex = planets.indexOf(planet1);
//    				Planet aspl1 = (Planet)planets.get(pindex);
//    				List<Model> planets2 = partner.getConfiguration().getPlanets();
//    				pindex = planets.indexOf(planet2);
//    				Planet aspl2 = (Planet)planets2.get(pindex);
//
//    				section.add(new Chunk(dict.getMark(aspl1, aspl2), fonth5));
					section.addSection(new Paragraph(planet1.getShortName() + " " + 
						type.getSymbol() + " " + 
						planet2.getShortName(), fonth5));
	
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
									section.add(new Paragraph("Роли: «" + dict.getRoles() + "»", bold));
	
								if (code.equals("OPPOSITION"))
									if (planet1.isMain() && planet2.isMain())
										section.add(new Paragraph("Уровень критичности: средний", PDFUtil.getDangerFont()));
								else if (code.equals("QUADRATURE"))
									if (planet1.isMain() && planet2.isMain())
										section.add(new Paragraph("Уровень критичности: средний", PDFUtil.getWarningFont()));

								section.add(new Paragraph(PDFUtil.removeTags(dict.getText(), font)));
								printGender(section, dict);
	
								Rule rule = EventRules.ruleSynastryAspect(aspect, synastry.getPartner());
								if (rule != null) {
				    				section.add(Chunk.NEWLINE);
									section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
								}
								section.add(Chunk.NEWLINE);
							}
						}
					}
				} else {
					if (aspectType.equals("NEGATIVE"))
						section.add(new Paragraph("Ниже приведены отрицательные факторы ваших отношений. "
							+ "Не преувеличивайте описанный негатив, он имеет место в любых парах. "
							+ "Резкие выяснения отношений возможны только в толкованиях с высоким уровнем критичности (далее по тексту это видно – выделено красным цветом). "
							+ "Если красные пометки отсутствуют, значит проблемы отношений будут связаны не с совместимостью, а с жизненными обстоятельствами (об этом расскажет долгосрочный прогноз отношений)", font));
					else
						section.add(new Paragraph("Толкования в левой колонке адресованы вам, толкования в правой колонке – вашему партнёру", PDFUtil.getAnnotationFont(false)));

			        PdfPTable table = new PdfPTable(2);
			        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
			        table.setLockedWidth(true);
			        table.setWidths(new float[] { 50, 50 });
			        table.setSpacingBefore(20);
		
					PdfPCell cell = null;
					Event[] events = new Event[] {synastry.getEvent(), synastry.getPartner()};
		
					for (Event e : events) {
						cell = new PdfPCell(new Phrase(e.getCallname(), font));
						PDFUtil.setCellBorderWidths(cell, 0, 0, .5F, 0);
						cell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(cell);
					}
					table.setHeaderRows(1);

					int tcount = list1.size() + list2.size();
					if (tcount > 0) {
						Iterator<SkyPointAspect> iter = list1.iterator();
						Iterator<SkyPointAspect> iter2 = list2.iterator();
						for (int i = 0; i < tcount; i++) {
							if (iter.hasNext()) {
								Phrase phrase = printAspect(synastry, iter.next(), service, false);
			       				cell = new PdfPCell(phrase);
								PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
			       				table.addCell(cell);
							} else if (iter2.hasNext()) {
			       				cell = new PdfPCell();
								PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
			       				table.addCell(cell);
							}

							if (iter2.hasNext()) {
								Phrase phrase = printAspect(synastry, iter2.next(), service, true);
			       				cell = new PdfPCell(phrase);
			       				cell.setBorder(Rectangle.NO_BORDER);
			       				table.addCell(cell);
							} else if (iter.hasNext()) {
			       				cell = new PdfPCell();
			       				cell.setBorder(Rectangle.NO_BORDER);
			       				table.addCell(cell);
							}
						}
					}
					section.add(table);
				}
			chapter.add(Chunk.NEXTPAGE);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация аспекта
	 * @param synastry синастрия
	 * @param aspect аспект
	 * @param service сервис толкований
	 * @param reverse true - толкование предназначено для партнёра
	 * @return толкование
	 */
	private Phrase printAspect(Synastry synastry, SkyPointAspect aspect, SynastryAspectService service, boolean reverse) {
		Phrase phrase = new Phrase();
		try {
			AspectType type = aspect.checkType(true);
			Planet planet1 = (Planet)(reverse ? aspect.getSkyPoint2() : aspect.getSkyPoint1());
			Planet planet2 = (Planet)(reverse ? aspect.getSkyPoint1() : aspect.getSkyPoint2());
			Font bold = new Font(baseFont, 12, Font.BOLD);
	
			phrase.add(new Chunk((reverse ? name2 : name1) + "-" + planet1.getShortName() + " " + 
				type.getSymbol() + " " + 
				(reverse ? name1 : name2) + "-" + planet2.getShortName(), fonth5));
			phrase.add(Chunk.NEWLINE);
			phrase.add(Chunk.NEWLINE);

			String code = aspect.getAspect().getCode();
			if (term) {
				phrase.add(new Chunk(" " + planet1.getSymbol(), PDFUtil.getHeaderAstroFont()));
	
				if (code.equals("CONJUNCTION") || code.equals("OPPOSITION"))
					phrase.add(new Chunk(aspect.getAspect().getSymbol(), PDFUtil.getHeaderAstroFont()));
				else
					phrase.add(new Chunk(type.getSymbol(), fonth5));
	
				phrase.add(new Chunk(planet2.getSymbol(), PDFUtil.getHeaderAstroFont()));
			}
	
			List<Model> dicts = service.finds(aspect);
			Event event = reverse ? synastry.getEvent() : synastry.getPartner();
			for (Model model : dicts) {
				SynastryAspectText dict = (SynastryAspectText)model;
				if (dict != null) {
					if (dict.getRoles() != null) {
						phrase.add(new Paragraph("Роли: «" + dict.getRoles() + "»", bold));
						phrase.add(Chunk.NEWLINE);
						phrase.add(Chunk.NEWLINE);
					}
					if (code.equals("OPPOSITION")) {
						if (planet1.isMain() && planet2.isMain()) {
							phrase.add(new Paragraph("Уровень критичности: высокий", PDFUtil.getDangerFont()));
							phrase.add(Chunk.NEWLINE);
							phrase.add(Chunk.NEWLINE);
						}
					} else if (code.equals("QUADRATURE")) {
						if (planet1.isMain() && planet2.isMain()) {
							phrase.add(new Paragraph("Уровень критичности: средний", PDFUtil.getWarningFont()));
							phrase.add(Chunk.NEWLINE);
							phrase.add(Chunk.NEWLINE);
						}
					}
					phrase.add(new Paragraph(PDFUtil.removeTags(dict.getText(), font)));
					Phrase ph = PDFUtil.printGenderCell(dict, event.isFemale(), event.isChild(), false);
					phrase.add(ph);
	
					Rule rule = EventRules.ruleSynastryAspect(aspect, event);
					if (rule != null) {
						phrase.add(Chunk.NEWLINE);
						phrase.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
					}
					phrase.add(Chunk.NEWLINE);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return phrase;
	}

	/**
	 * Генерация таблицы координат планет и домов
	 * @param chapter раздел
	 * @param event первый партнёр
	 * @param event второй партнёр
	 */
	private void printCoords(Chapter chapter, Event event, Event partner, boolean reverse) {
		try {
			Section section = PDFUtil.printSection(chapter, reverse ? "Координаты планет партнёра в вашем гороскопе" : "Координаты ваших планет в гороскопе партнёра");
			float fontsize = 9;
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
			Collection<Planet> planets = reverse ? partner.getConfiguration().getPlanets().values() : event.getConfiguration().getPlanets().values();
			for (Planet planet : planets) {
				BaseColor color = (++i % 2 > 0) ? new BaseColor(255, 255, 255) : new BaseColor(230, 230, 250);

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
		    	bar.setCategory(name1);
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
		    	bar.setCategory(name2);
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
		    	section.add(new Paragraph(PDFUtil.removeTags(y.getText(), font)));
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
		    	bar.setName(element.getTemperament());
		    	bar.setValue(entry.getValue() * (-1));
		    	bar.setColor(element.getColor());
		    	bar.setCategory(name1);
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
		    	bar.setName(element.getTemperament());
		    	bar.setValue(entry.getValue());
		    	bar.setColor(element.getColor());
		    	bar.setCategory(name2);
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
		    	section.add(new Paragraph(PDFUtil.html2pdf(element.getSynastry(), font)));
		    	section.add(Chunk.NEWLINE);
		    }

	        section.add(new Paragraph("Диаграмма показывает, на чём вы оба сконцентрированы, "
		        + "какие проявления для вас важны, необходимы и естественны:", font));
		    com.itextpdf.text.Image image = PDFUtil.printStackChart(writer, "Темпераменты", "Аспекты", "Баллы", bars, 500, 0, true);
			section.add(image);

			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Холерик – быстрый, порывистый, страстный, способный преодолевать значительные трудности, но неуравновешенный, склонный к бурным эмоциям и резким сменам настроения. Чувства возникают быстро и ярко отражаются в речи, жестах и мимике", PDFUtil.getDangerFont()));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Флегматик – медлительный, спокойный, с устойчивыми стремлениями и более или менее постоянным настроением (внешне слабо выражает своё душевное состояние). Тип нервной системы: сильный, уравновешенный, инертный. Хорошая память, высокий интеллект, склонность к продуманным, взвешенным решениям, без риска", PDFUtil.getSuccessFont()));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Сангвиник – живой, подвижный, сравнительно легко переживающий неудачи и неприятности. Мимика разнообразна и богата, темп речи быстрый. Эмоции преимущественно положительные, – быстро возникают и меняются", PDFUtil.getWarningFont()));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Меланхолик – легкоранимый, глубоко переживает даже незначительные неудачи, внешне вяло реагирует на происходящее. Тип нервной системы: высокочувствительный. Тонкая реакция на малейшие оттенки чувств. Переживания глубоки, эмоциональны и очень устойчивы", PDFUtil.getNeutralFont()));
	        list.add(li);
	        section.add(list);
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

			Collection<Planet> planets = event.getConfiguration().getPlanets().values();
			Collection<Planet> planets2 = partner.getConfiguration().getPlanets().values();

		    Bar[] bars = new Bar[planets.size() + planets2.size()];
		    int i = -1;
		    for (Planet planet : planets) {
		    	Bar bar = new Bar();
		    	bar.setName(term ? planet.getName() : planet.getShortName());
		    	bar.setValue(planet.getPoints());
				bar.setColor(planet.getColor());
				bar.setCategory(term ? "Ваши планеты" : "Ваши качества");
				bars[++i] = bar;
		    }

		    i = planets.size() - 1;
		    for (Planet planet : planets2) {
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

			//создаем карту статистики аспектов
			List<Model> aspectTypes = new AspectTypeService().getList();
			Map<String, Integer> aspcountmap = new HashMap<String, Integer>();
			Map<String, Integer> aspcountmap2 = new HashMap<String, Integer>();
			for (Model asptype : aspectTypes) {
				aspcountmap.put(((AspectType)asptype).getCode(), 0);
				aspcountmap2.put(((AspectType)asptype).getCode(), 0);
			}

			List<Long> pids = Arrays.asList(new Long[] {19L,20L,23L,24L,25L});
			for (SkyPointAspect spa : aspects) {
				Aspect a = spa.getAspect();
				String aspectTypeCode = a.getType().getCode();
				long pid = spa.getSkyPoint1().getId().longValue();

				if (pids.contains(pid)) {
					//суммируем аспекты каждого типа главных планет
					int score = aspcountmap.get(aspectTypeCode);
					aspcountmap.put(aspectTypeCode, ++score);
				} else {
					int score = aspcountmap2.get(aspectTypeCode);
					aspcountmap2.put(aspectTypeCode, ++score);
				}
			}

			//фильтрация списка типов аспектов
			List<Model> types = new AspectTypeService().getList();
			String[] codes = {
				"NEGATIVE", "NEGATIVE_HIDDEN", "POSITIVE", "POSITIVE_HIDDEN", "CREATIVE", "KARMIC", "SPIRITUAL", "PROGRESSIVE"
			};

			List<Bar> items = new ArrayList<Bar>();
			List<Bar> items2 = new ArrayList<Bar>();
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

		    	String name = term ? mtype.getName() : mtype.getKeyword();

		    	//первый партнёр
		    	int value = aspcountmap.get(type.getCode());
		    	if (value > 0) {
			    	boolean exists = false;
			    	for (Bar b : items) {
			    		if (b.getName().equals(name)) {
			    			exists = true;
					    	b.setValue(b.getValue() + value);
			    			break;
			    		}
			    	}
			    	if (!exists) {
				    	Bar bar = new Bar();
				    	bar.setName(mtype.getKeyword());
				    	bar.setValue(value);
						bar.setCategory(synastry.getEvent().getCallname());
						items.add(bar);
			    	}
		    	}

		    	//второй партнёр
		    	value = aspcountmap2.get(type.getCode());
		    	if (value > 0) {
			    	boolean exists = false;
			    	for (Bar b : items2) {
			    		if (b.getName().equals(name)) {
			    			exists = true;
					    	b.setValue(b.getValue() + value);
			    			break;
			    		}
			    	}
			    	if (!exists) {
				    	Bar bar = new Bar();
				    	bar.setName(mtype.getKeyword());
				    	bar.setValue(value * (-1));
						bar.setCategory(synastry.getPartner().getCallname());
						items2.add(bar);
			    	}
		    	}
		    }
		    items.addAll(items2);
		    Bar[] bars = items.toArray(new Bar[items.size()]);
		    com.itextpdf.text.Image image = PDFUtil.printStackChart(writer, "Аспекты отношений", "Аспекты", "Баллы", bars, 500, 300, true);
			Section section = PDFUtil.printSection(chapter, "Аспекты отношений");
			section.add(new Paragraph("Диаграмма сравнивает, что вы с партнёром испытываете от ваших отношений.", font));

			section.add(new Paragraph("Если у вас:", font));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("переживаний больше, чем стресса, — значит нужно искать разрядку своим негативным эмоциям, рассказывать партнёру о своих проблемах и больше доверять ему. Не держите обиды и напряжение в себе", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("скрытого позитива больше, чем лёгкости, — значит нужно выражать больше эмоций, не сдерживать радость, делиться своими успехами с партнёром", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("воздаяния за ошибки больше, чем стресса, — значит нужно обратить взгляд в прошлое и найти причины, которые привели к текущим последствиям. Скорей всего они кроются в вашем прошлом поведении и мышлении", font));
	        list.add(li);

			section.add(list);
			section.add(image);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация таблицы темпераментов
	 * @param doc документ
	 * @param chapter раздел
	 * @param event первый партнёр
	 * @param event второй партнёр
	 */
	private void printTemperament(Document doc, Chapter chapter, Event event, Event partner) {
		try {
			Section section = PDFUtil.printSection(chapter, "Сравнение темпераментов");
			section.add(new Paragraph("Если цвет ваших темпераментов совпадает, значит в указанной сфере вы совместимы от природы "
				+ "и не нужно тратить энергию на притирку характеров. Но если при этом ключевые слова тоже совпадают, "
				+ "то вы быстро наскучите друг другу (в указанной сфере)", this.font));

			float fontsize = 10;
			Font font = new Font(baseFont, fontsize, Font.NORMAL);
			Font bold = new Font(baseFont, fontsize, Font.BOLD);

			Collection<Planet> planets = event.getConfiguration().getPlanets().values();
			Collection<Planet> planets2 = partner.getConfiguration().getPlanets().values();
			int PLNUM = 5;
			Planet[] items = new Planet[PLNUM];
			Planet[] items2 = new Planet[PLNUM];

			int i = -1;
			for (Planet planet : planets) {
				if (planet.isMain())
					items[++i] = planet;
			}
			i = -1;
			for (Planet planet : planets2) {
				if (planet.isMain())
					items2[++i] = planet;
			}

	        PdfPTable table = new PdfPTable(3);
	        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
	        table.setLockedWidth(true);
	        table.setWidths(new float[] { 16, 42, 42 });
	        table.setSpacingBefore(20);

			PdfPCell cell = new PdfPCell(new Phrase(term ? "Планета" : "Сфера", font));
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(name1, font));
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(name2, font));
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
			section = PDFUtil.printSection(chapter, "Совместимость темпераментов");
			section.add(new Paragraph("Толкования в левой колонке адресованы вам, толкования в правой колонке – вашему партнёру", PDFUtil.getAnnotationFont(false)));

			table = new PdfPTable(2);
	        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
	        table.setLockedWidth(true);
	        table.setWidths(new float[] { 50, 50 });
	        table.setSpacingBefore(20);

			cell = null;
			Event[] events = new Event[] {event, partner};

			for (Event e : events) {
				cell = new PdfPCell(new Phrase(e.getCallname(), font));
				PDFUtil.setCellBorderWidths(cell, 0, 0, .5F, 0);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.addCell(cell);
			}

			ElementService service = new ElementService();
			long[] pids = new long[] {19L, 20L, 24L, 25L};
			for (long pid : pids) {
   				Planet planet = event.getConfiguration().getPlanets().get(pid);
   				Planet planet2 = partner.getConfiguration().getPlanets().get(pid);
   				kz.zvezdochet.bean.Element element1 = planet.getSign().getElement();
   				kz.zvezdochet.bean.Element element2 = planet2.getSign().getElement();

				String code = element1.getCode() + "_" + element2.getCode();
				kz.zvezdochet.bean.Element element = (kz.zvezdochet.bean.Element)service.find(code);
			    if (element != null) {
		    		if (element.getSynastry() != null) {
				    	String text = term
				    		? planet.getName() + " (" + element.getName() + ")"
				    		: planet.getSynastry();

						Phrase phrase = new Phrase();
						phrase.add(new Chunk(text, fonth5));
						phrase.add(Chunk.NEWLINE);
	    				phrase.add(Chunk.NEWLINE);
//						System.out.println("chapter depth " + chapter.getDepth());
//						System.out.println("chapter number depth " + chapter.getNumberDepth());
//						System.out.println("chapter number style " + chapter.getNumberStyle());
//						System.out.println("chapter indentation " + chapter.getIndentation());
		    			phrase.add(PDFUtil.printTextCell(element.getSynastry()));

	    				if (element1.getId().equals(element2.getId())) {
		    				cell = new PdfPCell(phrase);
	       					cell.setBorder(Rectangle.NO_BORDER);
		       				cell.setColspan(2);
		       				table.addCell(cell);
		    			} else {
		    				cell = new PdfPCell(phrase);
       						PDFUtil.setCellBorderWidths(cell, 0, 0, 0, .5F);
    	       				table.addCell(cell);
		    			}
		    		}
				}

				if (element1.getId().equals(element2.getId()))
					continue;
				else {
					code = element2.getCode() + "_" + element1.getCode();
					element = (kz.zvezdochet.bean.Element)service.find(code);
				    if (element != null) {
			    		if (element.getSynastry() != null) {
					    	String text = term
					    		? planet.getName() + " (" + element.getName() + ")"
					    		: planet.getSynastry();

							Phrase phrase = new Phrase();
							phrase.add(new Chunk(text, fonth5));
							phrase.add(Chunk.NEWLINE);
		    				phrase.add(Chunk.NEWLINE);
			    			phrase.add(PDFUtil.removeTags(element.getSynastry(), font));

		    				cell = new PdfPCell(phrase);
		    				cell.setBorder(Rectangle.NO_BORDER);
    	       				table.addCell(cell);
			    		}
					}
				}
			}
			section.add(table);
			chapter.add(Chunk.NEXTPAGE);
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
	 * Генерация домов в знаках
	 * @param event событие
	 * @param reverse true - партнёры для второго партнёра
	 * @return фраза
	 */
	private Phrase printHouseSign(Event event, boolean reverse) {
		Phrase phrase = new Phrase();
		List<Model> houses = event.getConfiguration().getHouses();
		if (null == houses)
			return phrase;
		try {
			if (reverse)
				phrase.add(new Paragraph("Типаж человека, которого ваш партнёр притягивает к себе:", PDFUtil.getAnnotationFont(false)));
			else
				phrase.add(new Paragraph("Типаж человека, которого вы притягиваете к себе:", PDFUtil.getAnnotationFont(false)));
			phrase.add(Chunk.NEWLINE);
			phrase.add(Chunk.NEWLINE);

			HouseSignService hservice = new HouseSignService();
			for (Model hmodel : houses) {
				House house = (House)hmodel;
				if (!house.getCode().equals("VII"))
					continue;

				Sign sign = SkyPoint.getSign(house.getCoord(), event.getBirthYear());
				HouseSignText dict = (HouseSignText)hservice.find(house, sign);
				if (dict != null) {
					if (term)
						phrase.add(new Paragraph(house.getDesignation() + " в созвездии " + sign.getName(), fonth5));
					else
						phrase.add(new Paragraph("Партнёр-" + sign.getShortname(), fonth5));
					phrase.add(Chunk.NEWLINE);
					phrase.add(Chunk.NEWLINE);
					phrase.add(new Paragraph(PDFUtil.removeTags(dict.getText(), font)));
				}
				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return phrase;
	}

	/**
	 * Генерация подходящих партнёров
	 * @param doc документ
	 * @param synastry синастрия
	 */
	private void printAkins(Document doc, Synastry synastry) {
		try {
			Chapter chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Рекомендуемые партнёры", null));
			chapter.setNumberDepth(0);
			chapter.add(new Paragraph("Обычно в гороскопе чётко обозначен образ человека, с которым вам суждено связать свою судьбу. "
				+ "Если вы в сомнениях, нашлась уже ваша половинка или нет, то ниже можете почитать, "
				+ "какой партнёр предназначен вам, а какой – вашему партнёру. Возможно, это поможет сделать правильный выбор", font));

			Section section = PDFUtil.printSection(chapter, "Ваша потенциальная пара");
	        PdfPTable table = new PdfPTable(2);
	        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
	        table.setLockedWidth(true);
	        table.setWidths(new float[] { 50, 50 });
//	        table.setSpacingBefore(20);

			PdfPCell cell = null;
			Event[] events = new Event[] {synastry.getEvent(), synastry.getPartner()};

			for (Event e : events) {
				cell = new PdfPCell(new Phrase(e.getCallname(), font));
				PDFUtil.setCellBorderWidths(cell, 0, 0, .5F, 0);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.addCell(cell);
			}

			Phrase phrase = printHouseSign(synastry.getEvent(), false);
			cell = new PdfPCell(phrase);
			PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
			table.addCell(cell);

			phrase = printHouseSign(synastry.getPartner(), true);
			cell = new PdfPCell(phrase);
			cell.setBorder(Rectangle.NO_BORDER);
			table.addCell(cell);
			section.add(table);
			chapter.add(Chunk.NEXTPAGE);

			//знаменитости
			EventService service = new EventService();
			List<Model> celeb = service.findAkin(synastry.getEvent(), 1);
			List<Model> celeb2 = service.findAkin(synastry.getPartner(), 1);
			int tcount = celeb.size() + celeb2.size();
			if (tcount > 0) {
				section = PDFUtil.printSection(chapter, "Подходящие знаменитости");
				section.add(new Paragraph("Известные люди, с которыми у вас хорошая совместимость:", font));

				table = new PdfPTable(2);
		        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
		        table.setLockedWidth(true);
		        table.setWidths(new float[] { 50, 50 });
		        table.setSpacingBefore(20);

				for (Event e : events) {
					cell = new PdfPCell(new Phrase(e.getCallname(), font));
					PDFUtil.setCellBorderWidths(cell, 0, 0, .5F, 0);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);
				}

				Iterator<Model> iter = celeb.iterator();
				Iterator<Model> iter2 = celeb2.iterator();
				for (int i = 0; i < tcount; i++) {
					if (iter.hasNext()) {
						phrase = printAkin((Event)iter.next());
	       				cell = new PdfPCell(phrase);
						PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
	       				table.addCell(cell);
					} else if (iter2.hasNext()) {
	       				cell = new PdfPCell();
						PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
	       				table.addCell(cell);
					}
	
					if (iter2.hasNext()) {
						phrase = printAkin((Event)iter2.next());
	       				cell = new PdfPCell(phrase);
	       				cell.setBorder(Rectangle.NO_BORDER);
	       				table.addCell(cell);
					} else if (iter.hasNext()) {
	       				cell = new PdfPCell();
	       				cell.setBorder(Rectangle.NO_BORDER);
	       				table.addCell(cell);
					}
				}
				section.add(table);
			}
			doc.add(chapter);
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
			Section section = PDFUtil.printSection(chapter, "Совместимость по Зороастрийскому календарю");
			section.add(new Paragraph("В Зороастрийском календаре началом нового года считается день весеннего равноденствия (в северном полушарии – 20 марта, в южном – 22-23 сентября)", PDFUtil.getAnnotationFont(false)));
			section.add(Chunk.NEWLINE);

			NumerologyService service = new NumerologyService();
//			int years = Math.abs(event.getBirthYear() - partner.getBirthYear()); TODO определять с учётом весеннего равноденствия
			int years = 3;
			Numerology dict = (Numerology)service.find(years);
			if (dict != null) {
				section.add(new Paragraph("Разница в годах цикла: " + CoreUtil.getAgeString(years), fonth5));
				section.add(new Paragraph(PDFUtil.removeTags(dict.getZoroastrsyn(), font)));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация планет в домах
	 * @param doc документ
	 * @param chapter раздел
	 * @param synastry синастрия
	 */
	private void printPlanetHouses(Document doc, Chapter chapter, Synastry synastry) {
		List<Model> houses = synastry.getEvent().getConfiguration().getHouses();
		List<Model> houses2 = synastry.getPartner().getConfiguration().getHouses();
		if (null == houses && null == houses2)
			return;
		Collection<Planet> planets = synastry.getEvent().getConfiguration().getPlanets().values();
		Collection<Planet> planets2 = synastry.getPartner().getConfiguration().getPlanets().values();

		List<Planet> hplanets = new ArrayList<>();
		List<Planet> hplanets2 = new ArrayList<>();
		List<Planet> relative = new ArrayList<>();
		try {
			for (Model hmodel : houses) {
				House house = (House)hmodel;
				for (Planet planet : planets2) {
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
					if (phouse != null && phouse.getId().equals(house.getId())) {
						planet.setHouse(house);
						hplanets.add(planet);
					}
				}
			}

			for (Model hmodel : houses2) {
				House house = (House)hmodel;
				for (Planet planet : planets) {
					House phouse = null;
					for (int j = 0; j < houses2.size(); j++) {
						House ehouse = (House)houses2.get(j);
						double pcoord = planet.getCoord();
						Double hmargin = (j == houses2.size() - 1) ?
							((House)houses2.get(0)).getCoord() : 
							((House)houses2.get(j + 1)).getCoord();
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
					if (phouse != null && phouse.getId().equals(house.getId())) {
						boolean rel = false;
						for (Planet p : hplanets) {
							if (planet.getId().equals(p.getId())
									&& house.getId().equals(p.getHouse().getId())) {
								boolean damaged = planet.isDamaged() || planet.isLilithed();
								boolean damaged2 = p.isDamaged() || p.isLilithed();
								if (damaged == damaged2) {
									relative.add(p);
									hplanets.remove(p);
									rel = true;
									break;
								}
							}
						}
						if (!rel) {
							planet.setHouse(house);
							hplanets2.add(planet);
						}
					}					
				}
			}

			if (!relative.isEmpty()) {
				Section section = PDFUtil.printSection(chapter, "Взаимовлияние");
				section.add(new Paragraph("Здесь перечислены толкования, которые показывают, в чём вы относитесь к партнёру так же, как он к вам:", PDFUtil.getAnnotationFont(false)));
				section.add(Chunk.NEWLINE);
				SynastryHouseService service = new SynastryHouseService();
				for (Planet planet : relative) {
					String sign = planet.isDamaged() || planet.isLilithed() ? "-" : "+";
					House house = planet.getHouse();

					String mark = planet.getMark("house");
					Paragraph p = new Paragraph("", fonth5);
					if (mark.length() > 0) {
	    				p.add(new Chunk(mark, fonth5));
	    				p.add(new Chunk(planet.getSymbol() + " ", PDFUtil.getHeaderAstroFont()));
					}
	    			if (term) {
	    				p.add(new Chunk(" " + planet.getName() + " в " + house.getDesignation() + " доме", fonth5));
	    				p.add(Chunk.NEWLINE);
	    			} else
	    				p.add(new Chunk(house.getSynastry() + " " + sign + " " + planet.getShortName(), fonth5));
	    			section.addSection(p);

					PlanetHouseText dict = (PlanetHouseText)service.find(planet, house, null);
					if (dict != null) {
						section.add(new Paragraph(PDFUtil.removeTags(dict.getText(), font)));
						printGender(section, dict);

						List<Rule> rules = EventRules.ruleSynastryPlanetHouse(planet, house, synastry.getEvent().isFemale());
						for (Rule rule : rules) {
							section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
							section.add(Chunk.NEWLINE);
						}

						rules = EventRules.ruleSynastryPlanetHouse(planet, house, synastry.getPartner().isFemale());
						for (Rule rule : rules) {
							section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
							section.add(Chunk.NEWLINE);
						}
					}
				}
				section.add(Chunk.NEXTPAGE);
			}
				
			Section section = PDFUtil.printSection(chapter, "Различия во влиянии");
			section.add(new Paragraph("Толкования в левой колонке адресованы вам, толкования в правой колонке – вашему партнёру", PDFUtil.getAnnotationFont(false)));

	        PdfPTable table = new PdfPTable(2);
	        table.setTotalWidth(doc.getPageSize().getWidth() - PDFUtil.PAGEBORDERWIDTH * 2);
	        table.setLockedWidth(true);
	        table.setWidths(new float[] { 50, 50 });
	        table.setSpacingBefore(20);

			PdfPCell cell = null;
			Event[] events = new Event[] {synastry.getEvent(), synastry.getPartner()};

			for (Event e : events) {
				cell = new PdfPCell(new Phrase(e.getCallname(), font));
				PDFUtil.setCellBorderWidths(cell, 0, 0, .5F, 0);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.addCell(cell);
			}
			table.setHeaderRows(1);

			Iterator<Planet> iter = hplanets.iterator();
			Iterator<Planet> iter2 = hplanets2.iterator();
			for (int i = 0; i < planets.size(); i++) {
				if (iter.hasNext()) {
					Phrase phrase = printPlanetHouse(synastry, iter.next(), false);
       				cell = new PdfPCell(phrase);
					PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
       				table.addCell(cell);
				} else if (iter2.hasNext()) {
       				cell = new PdfPCell();
					PDFUtil.setCellBorderWidths(cell, 0, .5F, 0, 0);
       				table.addCell(cell);
				}

				if (iter2.hasNext()) {
					Phrase phrase = printPlanetHouse(synastry, iter2.next(), true);
       				cell = new PdfPCell(phrase);
       				cell.setBorder(Rectangle.NO_BORDER);
       				table.addCell(cell);
				} else if (iter.hasNext()) {
       				cell = new PdfPCell();
       				cell.setBorder(Rectangle.NO_BORDER);
       				table.addCell(cell);
				}
			}
			section.add(table);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация планеты в доме
	 * @param synastry синастрия
	 * @param planet планета
	 * @param reverse true - толкование для партнёра
	 * @return
	 */
	private Phrase printPlanetHouse(Synastry synastry, Planet planet, boolean reverse) {
		Phrase phrase = new Phrase();
		try {
			String sign = planet.isDamaged() || planet.isLilithed() ? "-" : "+";
			House house = planet.getHouse();

			String mark = planet.getMark("house");
			if (mark.length() > 0) {
				phrase.add(new Chunk(mark, fonth5));
				phrase.add(new Chunk(planet.getSymbol() + " ", PDFUtil.getHeaderAstroFont()));
			}
			if (term) {
				phrase.add(new Chunk(" " + planet.getName() + " в " + house.getDesignation() + " доме", fonth5));
				phrase.add(Chunk.NEWLINE);
			} else {
				phrase.add(new Chunk((reverse ? name2 : name1) + "-" + house.getSynastry()
					+ " " + sign + " "
					+ (reverse ? name1 : name2) + "-" + planet.getShortName(), fonth5));
				phrase.add(Chunk.NEWLINE);
				phrase.add(Chunk.NEWLINE);
			}

			if ((planet.getCode().equals("LILITH") || planet.getCode().equals("KETHU"))
					&& (doctype.equals("love") && Arrays.asList(new String[] {"V_2", "VII"}).contains(house.getCode()))) {
				phrase.add(new Paragraph("Уровень критичности: высокий", PDFUtil.getDangerFont()));
				phrase.add(Chunk.NEWLINE);
				phrase.add(Chunk.NEWLINE);
			}

			SynastryHouseService service = new SynastryHouseService();
			PlanetHouseText dict = (PlanetHouseText)service.find(planet, house, null);
			Event event = reverse ? synastry.getPartner() : synastry.getEvent();
			if (dict != null) {
				phrase.add(new Paragraph(PDFUtil.removeTags(dict.getText(), font)));
				Phrase ph = PDFUtil.printGenderCell(dict, event.isFemale(), event.isChild(), false);
				if (ph != null) {
					phrase.add(Chunk.NEWLINE);
					phrase.add(Chunk.NEWLINE);
					phrase.add(ph);
				}
				List<Rule> rules = EventRules.ruleSynastryPlanetHouse(planet, house, event.isFemale());
				for (Rule rule : rules) {
					phrase.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
					phrase.add(Chunk.NEWLINE);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return phrase;
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
				section.add(new Paragraph(PDFUtil.removeTags(gender.getText(), font)));
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
			section.add(new Paragraph("Диаграмма показывает, какие сферы наиболее комфортны и эффективны для обоих.", font));
			section.add(Chunk.NEWLINE);
			section.add(new Paragraph("Чем выше значение, тем более вы с партнёром совместимы в указанной сфере отношений, "
				+ "тем меньше у вас претензий друг к другу, и тем выше чуткость, эмпатия, влечение и одобрение.", font));
			section.add(Chunk.NEWLINE);
			section.add(new Paragraph("Пункт «Соперничество» указывает на количество возникающих противоречий. "
				+ "Если значение сильно ниже нуля, значит, во многих вопросах вам будет трудно достичь двухстороннего понимания и согласия, "
				+ "что повлечёт за собой конфликты, напряжение, неудовлетворение (даже не явные). "
				+ "Противоречия эти чаще всего будут возникать в сферах, чьи значения ниже нуля", font));

			Map<String, Integer> map = new HashMap<String, Integer>() {
				private static final long serialVersionUID = 4739420822269120671L;
				{
			        put("Характеры", 0);
			        put("Общение", 0);
			        put("Любовь, чувства", 0);
			        put("Семья, забота, эмоции", 0);
			        put("Дружба, увлечения", 0);
			        put("Секс", 0);
			        put("Сотрудничество", 0);
			        put("Соперничество", 0);
			    }
			};
			Map<String, String[]> planets = new HashMap<String, String[]>() {
				private static final long serialVersionUID = 4739420822269120672L;
				{
			        put("Sun", new String[] {"Характеры"});
			        put("Moon", new String[] {"Семья, забота, эмоции"});
			        put("Rakhu", new String[] {"Характеры"});
			        put("Kethu", new String[] {"Характеры"});
			        put("Mercury", new String[] {"Общение"});
			        put("Venus", new String[] {"Любовь, чувства"});
			        put("Mars", new String[] {"Секс", "Соперничество"});
			        put("Selena", new String[] {"Характеры"});
			        put("Lilith", new String[] {"Характеры"});
			        put("Jupiter", new String[] {"Характеры"});
			        put("Saturn", new String[] {"Характеры"});
			        put("Chiron", new String[] {"Сотрудничество"});
			        put("Uranus", new String[] {"Дружба, увлечения"});
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
				if (!synastry.getEvent().isHousable() && planet1.getCode().equals("Moon"))
					continue;

				long asplanetid = aspect.getAspect().getPlanetid();
				if (asplanetid > 0 && asplanetid != planet1.getId())
					continue;
				Planet planet2 = (Planet)aspect.getSkyPoint2();
				if (!synastry.getPartner().isHousable() && planet2.getCode().equals("Moon"))
					continue;

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
		    com.itextpdf.text.Image image = PDFUtil.printBars(writer, "Сферы совместимости", "Сферы совместимости", "Баллы", bars, 500, 300, false, false, false);
			section.add(image);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация знаменитости
	 * @param event событие
	 * @return фраза
	 */
	private Phrase printAkin(Event event) {
		Phrase phrase = new Phrase();
		try {
			phrase.add(new Chunk(DateUtil.formatDate(event.getBirth()) + "  ", font));

			Font fonta = PDFUtil.getLinkFont();
			Chunk chunk = new Chunk(event.getName(), fonta);
	        chunk.setAnchor(event.getUrl());
	        phrase.add(chunk);
	
	        phrase.add(new Chunk("   " + event.getDescription(), font));
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return phrase;
	}
}
