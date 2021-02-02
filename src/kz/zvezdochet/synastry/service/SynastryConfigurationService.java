package kz.zvezdochet.synastry.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kz.zvezdochet.bean.AspectConfiguration;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.service.AspectConfigurationService;
import kz.zvezdochet.synastry.bean.Synastry;
import kz.zvezdochet.synastry.bean.SynastryConfiguration;

/**
 * Сервис конфигурации аспектов синастрии
 * @author Natalie Didenko
 */
public class SynastryConfigurationService extends ModelService {

	public SynastryConfigurationService() {
		tableName = "synastryconfs";
	}

	@Override
	public Model create() {
		return new SynastryConfiguration();
	}

	@Override
	public SynastryConfiguration init(ResultSet rs, Model model) throws DataAccessException, SQLException {
		SynastryConfiguration type = (model != null) ? (SynastryConfiguration)model : (SynastryConfiguration)create();
		SynastryService eventService = new SynastryService();
		Synastry synastry = (Synastry)eventService.find(rs.getLong("synastryid"));
		synastry.initPlanets();
		type.setSynastry(synastry);

		type.setVertex(rs.getString("vertex"));
		type.setLeftFoot(rs.getString("leftfoot"));
		type.setRightFoot(rs.getString("rightfoot"));
		type.setBase(rs.getString("base"));
		type.setLeftHand(rs.getString("lefthand"));
		type.setRightHand(rs.getString("righthand"));
		type.setLeftHorn(rs.getString("lefthorn"));
		type.setRightHorn(rs.getString("righthorn"));
		type.setHouseid(rs.getLong("houseid"));
		type.setText(rs.getString("text"));
		String s = rs.getString("reverse");
		boolean reverse = s.equals("1") ? true : false;
		type.setReverse(reverse);

		AspectConfiguration conf = (AspectConfiguration)new AspectConfigurationService().find(rs.getLong("confid"));
		conf.setVertex(getPlanets(type.getVertex(), synastry, reverse));
		conf.setBase(getPlanets(type.getBase(), synastry, reverse));
		conf.setLeftFoot(getPlanets(type.getLeftFoot(), synastry, reverse));
		conf.setRightFoot(getPlanets(type.getRightFoot(), synastry, reverse));
		conf.setLeftHand(getPlanets(type.getLeftHand(), synastry, reverse));
		conf.setRightHand(getPlanets(type.getRightHand(), synastry, reverse));
		conf.setLeftHorn(getPlanets(type.getLeftHorn(), synastry, reverse));
		conf.setRightHorn(getPlanets(type.getRightHorn(), synastry, reverse));
		conf.setData(type.getHouseid());
		conf.setImportable(reverse);
		conf.setDescription(type.getText());
		type.setConf(conf);
		return type;
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Преобразование идентификаторов планет в массив планет
	 * @param strarr строка идентификаторов планет
	 * @param synastry синастрия
	 * @param reverse true - доминирует второй партнёр
	 * @return массив планет
	 */
	private Planet[] getPlanets(String strarr, Synastry synastry, boolean reverse) {
		Planet[] arr = null;
		if (strarr != null) {
			Event event = reverse ? synastry.getPartner() : synastry.getEvent();
			Map<Long, Planet> planets = event.getPlanets();
			String[] list = strarr.split(",");
			arr = new Planet[list.length];
			int i = -1;
			for (String strid : list) {
				long id = Long.parseLong(strid);
				Planet planet = planets.get(id);
				arr[++i] = planet;
			}
		}
		return arr;
	}

	/**
	 * Поиск конфигураций аспектов синастрии
	 * @param synastryid идентификатор синастрии
	 * @return список конфигураций
	 */
	public List<SynastryConfiguration> findBySynastry(Long synastryid) throws DataAccessException {
		if (null == synastryid) return null;
		List<SynastryConfiguration> list = new ArrayList<SynastryConfiguration>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName + " where synastryid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, synastryid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add(init(rs, null));
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
}
