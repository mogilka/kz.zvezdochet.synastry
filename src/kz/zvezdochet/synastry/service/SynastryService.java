package kz.zvezdochet.synastry.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.EventService;
import kz.zvezdochet.service.PlanetService;
import kz.zvezdochet.synastry.bean.Synastry;

/**
 * Сервис синастрий
 * @author Natalie Didenko
 */
public class SynastryService extends ModelService {

	public SynastryService() {
		tableName = "synastry";
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		Synastry synastry = (Synastry)model;
		int result = -1;
        PreparedStatement ps = null;
		try {
			String sql;
			if (null == model.getId()) 
				sql = "insert into " + tableName + " values(0,?,?,?,?,?,?,?,?,?)";
			else
				sql = "update " + tableName + " set " +
					"eventid = ?, " +
					"partnerid = ?, " +
					"description = ?, " +
					"userid = ?, " +
					"date = ?, " +
					"calculated = ?, " +
					"celebrity = ?, " +
					"updated_at = ?, " +
					"options = ? " +
					"where id = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, synastry.getEvent().getId());
			ps.setLong(2, synastry.getPartner().getId());
			ps.setString(3, synastry.getDescription());
			ps.setLong(4, 3);
			String now = DateUtil.formatCustomDateTime(new Date(), "yyyy-MM-dd HH:mm:ss");
			ps.setString(5, now);
			ps.setInt(6, 0);
			ps.setInt(7, 0); //TODO вычислять динамически
			ps.setString(8, now);
			ps.setString(9, synastry.getOptions());
			if (model.getId() != null)
				ps.setLong(10, model.getId());

			result = ps.executeUpdate();
			if (1 == result) {
				if (null == model.getId()) { 
					Long autoIncKeyFromApi = -1L;
					ResultSet rsid = ps.getGeneratedKeys();
					if (rsid.next()) {
				        autoIncKeyFromApi = rsid.getLong(1);
				        model.setId(autoIncKeyFromApi);
					    System.out.println("inserted " + tableName + "\t" + autoIncKeyFromApi);
					}
					if (rsid != null)
						rsid.close();
				}
			}
			saveAspects(synastry);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)	ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return model;
	}

	/**
	 * Поиск синастрий персоны
	 * @param eventid идентификатор персоны
	 * @return список персон
	 */
	public List<Synastry> findPartners(Long eventid) throws DataAccessException {
		if (null == eventid) return null;
		List<Synastry> list = new ArrayList<Synastry>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName + " where eventid = ? or partnerid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, eventid);
			ps.setLong(2, eventid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add((Synastry)init(rs, new Synastry()));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) { 
				e.printStackTrace(); 
			}
		}
		return list;
	}

	@Override
	public Model create() {
		return new Synastry();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException, SQLException {
		Synastry dict = (base != null) ? (Synastry)base : (Synastry)create();
		EventService service = new EventService();
		dict.setId(rs.getLong("id"));
		dict.setEvent((Event)service.find(rs.getLong("eventid")));
		dict.setPartner((Event)service.find(rs.getLong("partnerid")));
		dict.setDescription(rs.getString("description"));
		dict.setDate(DateUtil.getDatabaseDateTime(rs.getString("date")));
		String s = rs.getString("calculated");
		dict.setCalculated(s.equals("1") ? true : false);
		s = rs.getString("celebrity");
		dict.setCelebrity(s.equals("1") ? true : false);
		dict.setOptions(rs.getString("options"));
		return dict;
	}

	/**
	 * Поиск синастрии партнёров
	 * @param eventid идентификатор первого партнёра
	 * @param partnerid идентификатор второго партнёра
	 * @return синастрия
	 */
	public Synastry find(Long eventid, Long partnerid) throws DataAccessException {
		if (null == eventid || null == partnerid)
			return null;
		Synastry model = new Synastry();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName +
				" where (eventid = ? and partnerid = ?) " +
					"or (eventid = ? and partnerid = ?)";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, eventid);
			ps.setLong(2, partnerid);
			ps.setLong(3, partnerid);
			ps.setLong(4, eventid);
			rs = ps.executeQuery();
			if (rs.next())
				init(rs, model);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) { 
				e.printStackTrace(); 
			}
		}
		return model;
	}

	/**
	 * Возвращает имя таблицы, хранящей аспекты планет синастрии
	 * @return имя ТБД
	 */
	public String getAspectTable() {
		return "synastry_aspects";
	}

	/**
	 * Поиск аспектов синастрии
	 * @param id идентификатор синастрии
	 * @return список персон
	 */
	public List<SkyPointAspect> findAspects(Long id) throws DataAccessException {
		if (null == id) return null;
		List<SkyPointAspect> list = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + getAspectTable() + " where synastryid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			PlanetService service = new PlanetService();
			AspectService aservice = new AspectService();
			while (rs.next()) {
				SkyPointAspect aspect = new SkyPointAspect();
				Planet p = (Planet)service.find(rs.getLong("planetid"));
				Planet p2 = (Planet)service.find(rs.getLong("planet2id"));
				Aspect a = (Aspect)aservice.find(rs.getLong("aspectid"));
				aspect.setSkyPoint1(p);
				aspect.setSkyPoint2(p2);
				aspect.setAspect(a);
				list.add(aspect);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) { 
				e.printStackTrace(); 
			}
		}
		return list;
	}

	/**
	 * Сохранение аспектов планет синастрии
	 * @param synastry синастрия
	 * @throws DataAccessException
	 */
	public void saveAspects(Synastry synastry) throws DataAccessException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String table = getAspectTable();
		try {
			String sql = "update " + table + " set aspectid = null where synastryid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, synastry.getId());
			ps.executeUpdate();
			ps.close();

			List<SkyPointAspect> aspects = synastry.getAspects();
			if (aspects != null) {
				for (SkyPointAspect aspect : aspects) {
					SkyPoint point = aspect.getSkyPoint1();
					SkyPoint point2 = aspect.getSkyPoint2();
					if (point.getNumber() > point2.getNumber()) continue;
					sql = "select id from " + table + 
						" where synastryid = ?" +
						" and planetid = ?" +
						" and planet2id = ?";
					ps = Connector.getInstance().getConnection().prepareStatement(sql);
					ps.setLong(1, synastry.getId());
					ps.setLong(2, point.getId());
					ps.setLong(3, point2.getId());
					rs = ps.executeQuery();
					long id = (rs.next()) ? rs.getLong("id") : 0;
					ps.close();
					
					if (0 == id)
						sql = "insert into " + table + " values(0,?,?,?,?,?,?)";
					else
						sql = "update " + table + 
							" set synastryid = ?,"
							+ " planetid = ?,"
							+ " aspectid = ?,"
							+ " planet2id = ?,"
							+ " exact = ?,"
							+ " application = ?" +
							" where id = ?";
					ps = Connector.getInstance().getConnection().prepareStatement(sql);
					ps.setLong(1, synastry.getId());
					ps.setLong(2, point.getId());
					ps.setLong(3, aspect.getAspect().getId());
					ps.setLong(4, point2.getId());
					ps.setInt(5, aspect.isExact() ? 1 : 0);
					ps.setInt(6, aspect.isApplication() ? 1 : 0);
					if (id != 0)
						ps.setLong(7, id);
					ps.executeUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null)	ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
