package kz.zvezdochet.synastry.service;

import java.sql.ResultSet;
import java.sql.SQLException;

import kz.zvezdochet.analytics.service.PlanetHouseRuleService;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;
import kz.zvezdochet.synastry.bean.SynastryHouseRule;

/**
 * Сервис планет в астрологических домах
 * @author Natalie Didenko
 */
public class SynastryHouseRuleService extends PlanetHouseRuleService {

	public SynastryHouseRuleService() {
		tableName = "synastryhouserule";
	}

	@Override
	public SynastryHouseRule init(ResultSet rs, Model model) throws DataAccessException, SQLException {
		SynastryHouseRule dict = (model != null) ? (SynastryHouseRule)model : (SynastryHouseRule)create();
		dict.setId(rs.getLong("ID"));
		dict.setText(rs.getString("Text"));
		PlanetService planetService = new PlanetService();
		HouseService houseService = new HouseService();
		dict.setPlanet((Planet)planetService.find(rs.getLong("PlanetID")));
		dict.setHouse((House)houseService.find(rs.getLong("HouseID")));
		dict.setAspectType((AspectType)new AspectTypeService().find(rs.getLong("TypeID")));
		dict.setPlanet2((Planet)planetService.find(rs.getLong("Planet2ID")));
		dict.setHouse2((House)houseService.find(rs.getLong("House2ID")));
		return dict;
	}

	@Override
	public Model create() {
		return new SynastryHouseRule();
	}
}
