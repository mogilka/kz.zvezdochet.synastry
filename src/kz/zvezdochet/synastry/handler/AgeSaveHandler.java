package kz.zvezdochet.synastry.handler;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.synastry.Activator;
import kz.zvezdochet.synastry.bean.DirectionAspectText;
import kz.zvezdochet.synastry.bean.DirectionHouseText;
import kz.zvezdochet.synastry.part.AgePart;
import kz.zvezdochet.synastry.service.DirectionAspectService;
import kz.zvezdochet.synastry.service.DirectionHouseService;

/**
 * Сохранение дирекций синастрии в файл
 * @author Natalie Didenko
 */
public class AgeSaveHandler extends Handler {
	private BaseFont baseFont;
	private boolean term = false;

	/**
	 * Используемые типы толкований
	 * love|deal|family любовный|партнёрский|семейный
	 */
	private String[] genderTypes = {};

	/**
	 * Тип гороскопа совместимости
	 * 0|1|2 любовный|партнёрский|семейный
	 */
	private int doctype = 0;

	public AgeSaveHandler() {
		super();
		try {
			baseFont = PDFUtil.getBaseFont();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Execute
	public void execute(@Active MPart activePart) {
		updateStatus("Экспорт парного прогноза", false);
		AgePart agePart = (AgePart)activePart.getObject();
		if (!agePart.check(0)) return;

		doctype = DialogUtil.alertQuestion("Вопрос", "Выберите тип гороскопа:", new String[] {"Любовный", "Партнёрский", "Семейный"});
		if (doctype > 1)
			genderTypes = new String[] {"family"};
		else if (doctype > 0)
			genderTypes = new String[] {"deal"};
		else
			genderTypes = new String[] {"love", "family"};

		int initage = agePart.getAge();
		int years = agePart.getYears();
		@SuppressWarnings("unchecked")
		List<SkyPointAspect> spas = (List<SkyPointAspect>)agePart.getData();
		term = agePart.isTerm();

		Event event = agePart.getEvent();
		Event partner = agePart.getPartner();
		String name1 = event.getCallname();
		String name2 = partner.getCallname();

		Document doc = new Document();
		try {
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/longterm.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	        //metadata
	        PDFUtil.getMetaData(doc, "Парный прогноз");

	        //раздел
			Chapter chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Парный прогноз", null));
			chapter.setNumberDepth(0);

			//шапка
	        int ages = years;
			String text = event.getCallname() + " – " + partner.getCallname();
			text += ": прогноз на " + CoreUtil.getAgeString(ages);
			Font font = PDFUtil.getRegularFont();
			Paragraph p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			text = "Тип прогноза: " + (doctype > 1 ? "семейный" : doctype > 0 ? "партнёрский" : "любовный");
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

	        chapter.add(new Paragraph("Данный прогноз описывает самые значительные тенденции ваших отношений в ближайшие " + CoreUtil.getAgeString(ages)
        		+ ". Здесь описаны события, которые не произошли бы у вас в жизни без вашего партнёра. В качестве ориентира периодов указан ваш возраст.", font));
			Font red = PDFUtil.getDangerFont();
			chapter.add(new Paragraph("Максимальная погрешность прогноза ±2 месяца.", red));

			//данные для графика
			Map<Integer,Integer> positive = new HashMap<Integer,Integer>();
			Map<Integer,Integer> negative = new HashMap<Integer,Integer>();

			for (int i = 0; i < ages; i++) {
				int nextage = initage + i;
				positive.put(nextage, 0);
				negative.put(nextage, 0);
			}
			Map<Integer, Map<Long, Double>> seriesa1 = new HashMap<Integer, Map<Long, Double>>();
			Map<Integer, Map<Long, Double>> seriesa2 = new HashMap<Integer, Map<Long, Double>>();

			//события
			Map<Integer, TreeMap<Integer, List<SkyPointAspect>>> map = new HashMap<Integer, TreeMap<Integer, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
				Planet planet = (Planet)spa.getSkyPoint1();
				String pcode = planet.getCode();
				String pcode2 = spa.getSkyPoint2().getCode();
				boolean isHouse = spa.getSkyPoint2() instanceof House;

				if (spa.getAspect().getCode().equals("OPPOSITION")) {
					if (isHouse) {
						if	(pcode.equals("Kethu") || pcode.equals("Rakhu"))
							continue;
					} else if (pcode.equals("Rakhu") || pcode2.equals("Rakhu"))
							continue;
				}
				if (!isHouse
						&& !spa.getAspect().getCode().equals("CONJUNCTION")
						&& (pcode.equals("Kethu") || pcode2.equals("Kethu")))
					continue;

				int age = (int)spa.getAge();
				TreeMap<Integer, List<SkyPointAspect>> agemap = map.get(age);
				if (null == agemap) {
					agemap = new TreeMap<Integer, List<SkyPointAspect>>();
					agemap.put(0, new ArrayList<SkyPointAspect>());
					agemap.put(1, new ArrayList<SkyPointAspect>());
				}

				String code = spa.getAspect().getType().getCode();
				if (code.equals("NEUTRAL") || code.equals("NEGATIVE") || code.equals("POSITIVE")) {
					//формируем список событий для толкования
					if (isHouse) {
						if (code.equals("NEUTRAL")) {
							List<SkyPointAspect> list = agemap.get(0);
							list.add(spa);
						}
					} else {
						List<SkyPointAspect> list = agemap.get(1);
						list.add(spa);
					}

					//формируем статистику событий по всему периоду
					double point = 0;
					if (code.equals("NEUTRAL")) {
						if (pcode.equals("Lilith") || pcode.equals("Kethu")) {
							negative.put(age, negative.get(age) + 1);
							--point;
						} else {
							positive.put(age, positive.get(age) + 1);
							++point;
						}
					} else if (code.equals("POSITIVE")) {
						positive.put(age, positive.get(age) + 1);
						point += 2;
					} else if (code.equals("NEGATIVE")) {
						negative.put(age, negative.get(age) + 1);
						--point;
					}

					if (isHouse) {
						long houseid = spa.getSkyPoint2().getId();
						//данные для диаграммы возраста
						Map<Integer, Map<Long, Double>> seriesa = spa.isRetro() ? seriesa2 : seriesa1;
						Map<Long, Double> submapa = seriesa.containsKey(age) ? seriesa.get(age) : new HashMap<Long, Double>();
						double val = submapa.containsKey(houseid) ? submapa.get(houseid) : 0;
						submapa.put(houseid, val + point);
						seriesa.put(age, submapa);
					}
				}
				map.put(age, agemap);
			}

			Bar[] bars = new Bar[ages * 2];
			for (int i = 0; i < ages; i++) {
				int nextage = initage + i;
				String strage = CoreUtil.getAgeString(nextage);
				bars[i] = new Bar(strage, positive.get(nextage), null, "Позитивные события", null);
				bars[i + ages] = new Bar(strage, negative.get(nextage) * (-1), null, "Негативные события", null);
			}
			int height = 400;
			if (ages < 2)
				height = 150;
			else if (ages < 4)
				height = 200;
			Image image = PDFUtil.printStackChart(writer, "Соотношение категорий событий", "Возраст", "Количество", bars, 500, height, true);
			chapter.add(image);
			if (ages > 2)
				chapter.add(Chunk.NEXTPAGE);
			else
				chapter.add(Chunk.NEWLINE);

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Примечание:", bold));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
	        ListItem li = new ListItem();
	        li.add(new Chunk("Чёрным цветом выделено самое важное, что с вами произойдёт. "
	        	+ "Эти тенденции могут сохраняться в течение двух лет. А всё остальное продлится не более трёх месяцев.", font));
	        list.add(li);

			Font green = PDFUtil.getSuccessFont();
			li = new ListItem();
	        li.add(new Chunk("Зелёным цветом выделены позитивные тенденции, которые нужно использовать для гармонизации отношений. "
	        	+ "К этим тенденциям относятся события, которые сами по себе удачно сложатся (без больших усилий).", green));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Красным цветом выделены негативные тенденции, которые вызовут конфликт и потребуют большого расхода энергии. "
	        	+ "От этих сфер жизни не следует ждать слишком многого. "
	        	+ "Это признак того, что вам обоим необходим отдых друг от друга, переосмысление и мобилизация ресурсов для решения проблемы.", red));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("В течение года в одной и той же сфере жизни могут происходить как напряжённые, так и приятные события.", font));
	        list.add(li);

	        if (ages > 1) {
				li = new ListItem();
		        li.add(new Chunk("Если из возраста в возраст событие повторяется, значит оно создаст большой резонанс.", font));
		        list.add(li);
	        }
	        chapter.add(list);
	        doc.add(chapter);

			HouseService serviceh = new HouseService();
			Map<Integer, TreeMap<Integer, List<SkyPointAspect>>> treeMap = new TreeMap<Integer, TreeMap<Integer, List<SkyPointAspect>>>(map);
			for (Map.Entry<Integer, TreeMap<Integer, List<SkyPointAspect>>> entry : treeMap.entrySet()) {
				TreeMap<Integer, List<SkyPointAspect>> agemap = entry.getValue();
				if (agemap.isEmpty())
					continue;

			    int age = entry.getKey();
			    String agestr = CoreUtil.getAgeString(age);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), agestr, null));
				chapter.setNumberDepth(0);

				//диаграмма возраста
				Section section = PDFUtil.printSection(chapter, "Диаграмма " + agestr, null);
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость. "
		        	+ "К ним относятся позитивные толкования, приведённые в данном разделе.", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 102))));
		        list.add(li);
		
				li = new ListItem();
		        li.add(new Chunk("Показатели на нуле указывают на нейтральность ситуации.", font));
		        list.add(li);
		
				li = new ListItem();
		        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение. "
		        	+ "К ним относятся негативные толкования, приведённые в данном разделе.", new Font(baseFont, 12, Font.NORMAL, new BaseColor(102, 0, 51))));
		        list.add(li);
		        section.add(list);
		        section.add(Chunk.NEWLINE);

				//первый партнёр
				Map<Long, Double> mapa = seriesa1.get(age);
				Bar[] items = new Bar[mapa.size()];
				int i = -1;
				for (Map.Entry<Long, Double> entry2 : mapa.entrySet()) {
					House house = (House)serviceh.find(entry2.getKey());
					Bar bar = new Bar();
			    	bar.setName(house.getSynastry());
				    bar.setValue(entry2.getValue());
					bar.setColor(house.getColor());
					bar.setCategory(age + "");
					items[++i] = bar;
				}
				section.add(PDFUtil.printBars(writer, name2, "Сферы жизни партнёра, на которые вы повлияете в течение года", "Сферы жизни", "Баллы", items, 500, 280, false, false, false));

				//второй партнёр
				mapa = seriesa2.get(age);
				items = new Bar[mapa.size()];
				i = -1;
				for (Map.Entry<Long, Double> entry2 : mapa.entrySet()) {
					House house = (House)serviceh.find(entry2.getKey());
					Bar bar = new Bar();
			    	bar.setName(house.getSynastry());
				    bar.setValue(entry2.getValue());
					bar.setColor(house.getColor());
					bar.setCategory(age + "");
					items[++i] = bar;
				}
				section.add(PDFUtil.printBars(writer, name1, "Ваши сферы жизни, на которые повлияет партнёр", "Сферы жизни", "Баллы", items, 500, 280, false, false, false));
				chapter.add(Chunk.NEXTPAGE);

				for (Map.Entry<Integer, List<SkyPointAspect>> subentry : agemap.entrySet())
					printEvents(event, partner, chapter, age, subentry.getKey(), subentry.getValue());
				doc.add(chapter);
				doc.add(Chunk.NEXTPAGE);
			}
			
			if (term) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сокращения", null));
				chapter.setNumberDepth(0);

				chapter.add(new Paragraph("Раздел событий:", font));
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("\u2191 — сильная планета, адекватно проявляющая себя в астрологическом доме", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("\u2193 — ослабленная планета, источник неуверенности, стресса и препятствий", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("обт — указанный астрологический дом является обителью планеты и облегчает ей естественное и свободное проявление", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("экз — указанный астрологический дом является местом экзальтации планеты, усиливая её проявления и уравновешивая слабые качества", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("пдн — указанный астрологический дом является местом падения планеты, где она чувствует себя «не в своей тарелке»", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("изг — указанный астрологический дом является местом изгнания планеты, ослабляя её проявления и усиливает негатив", font));
		        list.add(li);
		        chapter.add(list);

		        chapter.add(Chunk.NEWLINE);
				chapter.add(new Paragraph("Раздел личности:", font));
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("\u2191 — усиленный аспект, проявляющийся ярче других аспектов указанных планет (хорошо для позитивных сочетаний, плохо для негативных)", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("\u2193 — ослабленный аспект, проявляющийся менее ярко по сравнению с другими аспектами указанных планет (плохо для позитивных сочетаний, хорошо для негативных)", font));
		        list.add(li);
		        chapter.add(list);
				doc.add(chapter);
			}
	        doc.add(PDFUtil.printCopyright());
	        updateStatus("Файл дирекций сформирован", false);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
	        doc.close();
		}
	}

	/**
	 * Генерация событий по категориям
	 * @param event первый партнёр
	 * @param partner второй партнёр
	 * @param chapter раздел документа
	 * @param age возраст
	 * @param code код подраздела
	 * @param spas список событий
	 */
	private Section printEvents(Event event, Event partner, Chapter chapter, int age, int code, List<SkyPointAspect> spas) {
		try {
			if (spas.isEmpty())
				return null;

			Font grayfont = PDFUtil.getAnnotationFont(false);
			String header = "";

			String name1 = event.getCallname();
			String name2 = partner.getCallname();

			Paragraph p = null;
			String agestr = CoreUtil.getAgeString(age);
			if (0 == code) {
				header += "Значимые события " + agestr;
				p = new Paragraph("В данном разделе описаны яркие события вашей пары, "
					+ "которые произойдут в возрасте " + agestr + ", надолго запомнятся и повлекут за собой перемены", grayfont);
			} else if (1 == code) {
				header += "Проявления пары в " + agestr;
				p = new Paragraph("В данном разделе описаны ваши взаимные проявления, которые станут особенно яркими в возрасте " + agestr, grayfont);
			}
			Section section = PDFUtil.printSection(chapter, header, null);
			if (p != null) {
				p.setSpacingAfter(10);
				section.add(p);
			}
			boolean female = event.isFemale();
			Font fonth5 = PDFUtil.getHeaderFont();

			DirectionHouseService service = new DirectionHouseService();
			DirectionAspectService servicea = new DirectionAspectService();

			for (SkyPointAspect spa : spas) {
				AspectType type = spa.getAspect().getType();
				Planet planet = (Planet)spa.getSkyPoint1();
				SkyPoint skyPoint = spa.getSkyPoint2();
				boolean reverse = spa.isRetro();

				if (skyPoint instanceof House) {
					House house = (House)skyPoint;
					DirectionHouseText dirText = (DirectionHouseText)service.find(planet, house, type);

					String text = "";
					if (term)
						text = planet.getName() + " " + type.getSymbol() + " " + house.getDesignation() + " дом";
					else {
						String hname = reverse ? name1 : name2;
						String pname = reverse ? name2 : name1;
	    				text = hname + "-" + house.getSynastry() + " " + type.getSymbol() + " " + pname + "-" + planet.getShortName();
					}
					section.addSection(new Paragraph(text, fonth5));
					if (term) {
						String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
							? "с куспидом"
							: "к куспиду";

						p = new Paragraph();
	    				p.add(new Chunk(planet.getMark("house"), grayfont));
			    		p.add(new Chunk(spa.getAspect().getName() + " ", grayfont));
	    				p.add(new Chunk(planet.getSymbol() + "d", PDFUtil.getHeaderAstroFont()));
			    		p.add(new Chunk(" " + pretext + " " + house.getDesignation(), grayfont));
	    				section.add(p);
					}
					if (dirText != null) {
						text = dirText.getText();
						if (text != null) {
					    	if (!reverse)
						    	section.add(new Paragraph("Толкование следует воспринимать так, как будто оно адресовано к персоне «" + name2 + "»", PDFUtil.getWarningFont()));
							String typeColor = type.getFontColor();
							BaseColor color = PDFUtil.htmlColor2Base(typeColor);
							section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
							section.add(Chunk.NEWLINE);
							PDFUtil.printGender(section, dirText, female, false, true);
							for (String gtype : genderTypes)
								PDFUtil.printGender(section, dirText, gtype);
							if (doctype < 1) {
								PDFUtil.printGender(section, dirText, event.isFemale() ? "man" : "woman");
							}
						}
					}

				} else if (skyPoint instanceof Planet) {
					Planet planet2 = (Planet)skyPoint;
    				boolean preverse = planet.getNumber() > planet2.getNumber();
    				String text = "";
    				if (term)
						text = planet.getName() + " " + type.getSymbol() + " " + planet2.getName();
    				else {
    					String pname = "", pname2 = "";
	    				if (reverse) {
							pname = preverse ? name1 + "-" + planet2.getShortName() : name2 + "-" + planet.getShortName();
		    				pname2 = preverse ? name2 + "-" + planet.getShortName() : name1 + "-" + planet2.getShortName();
	    				} else {
							pname = preverse ? name2 + "-" + planet2.getShortName() : name1 + "-" + planet.getShortName();
		    				pname2 = preverse ? name1 + "-" + planet.getShortName() : name2 + "-" + planet2.getShortName();
	    				}
						text = pname + " " + type.getSymbol() + " " + pname2;
    				}
    				section.addSection(new Paragraph(text, fonth5));

//    				if (25 == planet.getId() && 30 == planet2.getId())
//    					System.out.println(age + ": " + spa);
//    				if (30 == planet.getId() && 25 == planet2.getId())
//    					System.out.println(age + ": " + spa);
					List<Model> texts = servicea.finds(spa, preverse);
					if (!texts.isEmpty())
						for (Model model : texts) {
							DirectionAspectText dirText = (DirectionAspectText)model;
							if (dirText != null) {
								text = dirText.getText();
								if (null == text || text.isEmpty())
									continue;
							}

			    			if (term) {
								String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
									? "с "
									: "к ";

			    				p = new Paragraph();
			    				if (dirText != null)
			    					p.add(new Chunk(dirText.getMark(), grayfont));
					    		p.add(new Chunk(spa.getAspect().getName() + " ", grayfont));
			    				p.add(new Chunk(planet.getSymbol() + "d", PDFUtil.getHeaderAstroFont()));
					    		p.add(new Chunk(" " + pretext + " ", grayfont));
			    				p.add(new Chunk(planet2.getSymbol() + "r", PDFUtil.getHeaderAstroFont()));
			    				section.add(p);
			    			}
							if (dirText != null) {
								text = dirText.getText();
								if (text != null) {
							    	if (null == dirText.getAspect().getId()
							    			&& ((preverse && !reverse)
							    				|| (reverse && planet.getNumber() < planet2.getNumber())))
								    	section.add(new Paragraph("Толкование следует воспринимать так, как будто оно адресовано к персоне «" + name2 + "»", PDFUtil.getWarningFont()));
					    			String typeColor = type.getFontColor();
									BaseColor color = PDFUtil.htmlColor2Base(typeColor);
									section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
									section.add(Chunk.NEWLINE);
									PDFUtil.printGender(section, dirText, female, false, true);
									for (String gtype : genderTypes)
										PDFUtil.printGender(section, dirText, gtype);
								}
							}
						}
				}
			}
			return section;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
