package kz.zvezdochet.synastry.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.service.EventService;
import kz.zvezdochet.synastry.bean.Synastry;

/**
 * Сервис синастрий
 * @author Nataly Didenko
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
				sql = "insert into " + tableName + " values(0,?,?,?,?,?,?,?)";
			else
				sql = "update " + tableName + " set " +
					"eventid = ?, " +
					"partnerid = ?, " +
					"description = ?, " +
					"userid = ?, " +
					"date = ?, " +
					"calculated = ?, " +
					"celebrity = ? " +
					"where id = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, synastry.getEventid());
			ps.setLong(2, synastry.getPartnerid());
			ps.setString(3, synastry.getDescription());
			ps.setLong(4, 2);
			ps.setString(5, DateUtil.formatCustomDateTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
			ps.setInt(6, 0);
			ps.setInt(7, 0); //TODO вычислять динамически
			if (model.getId() != null)
				ps.setLong(8, model.getId());

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
	 * Поиск партнёров персоны
	 * @param eventid идентификатор персоны
	 * @return список персон
	 */
	public List<Event> findPartners(Long eventid) throws DataAccessException {
		if (null == eventid) return null;
		List<Event> list = new ArrayList<Event>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName + " where eventid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, eventid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add((Event)new EventService().find(rs.getLong("partnerid")));
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
	public Model init(ResultSet rs, Model base) throws DataAccessException,
			SQLException {
		// TODO Auto-generated method stub
		return null;
	}
}
