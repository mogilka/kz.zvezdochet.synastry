package kz.zvezdochet.synastry.service;

import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.synastry.bean.DirectionAspectText;

/**
 * Сервис синастрических дирекций аспектов планет
 * @author Natalie Didenko
 */
public class DirectionAspectService extends SynastryAspectService {

	public DirectionAspectService() {
		tableName = "synastrydirectionaspects";
	}

	@Override
	public Model create() {
		return new DirectionAspectText();
	}
}
