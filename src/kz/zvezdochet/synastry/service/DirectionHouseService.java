package kz.zvezdochet.synastry.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.synastry.bean.DirectionHouseText;

/**
 * Сервис дирекций синастрических домов
 * @author Natalie Didenko
 */
public class DirectionHouseService extends SynastryHouseService {

	public DirectionHouseService() {
		tableName = "synastrydirections";
	}

	@Override
	public Model create() {
		return new DirectionHouseText();
	}

	/**
	 * Поиск дирекции планеты к дому
	 * @param planet планета
	 * @param house астрологический дом
	 * @param aspectType тип аспекта
	 * @return описание позиции планеты в доме
	 * @throws DataAccessException
	 */
	public Model find(Planet planet, House house, AspectType aspectType) throws DataAccessException {
        PreparedStatement ps = null;
        ResultSet rs = null;
		String sql;

		AspectTypeService service = new AspectTypeService();
		if (aspectType.getCode().equals("NEUTRAL")
				&& (planet.getCode().equals("Lilith")
					|| planet.getCode().equals("Kethu")))
				aspectType = (AspectType)service.find("NEGATIVE");
		try {
			sql = "select * from " + tableName + 
				" where typeid = " + aspectType.getId() +
				" and planetid = " + planet.getId() +
				" and houseid = " + house.getId();
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next())
				return init(rs, create());
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
		return null;
	}
}
