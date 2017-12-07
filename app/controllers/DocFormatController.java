package controllers;

import actions.AuthAction;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.*;

public class DocFormatController extends Controller{
	
	@With(AuthAction.class)
	@Transactional
	public Result momChecklistFormWork() {
		return ok(checklistmomformwork.render());
	}
	
	
	@With(AuthAction.class)
	@Transactional
	public Result momChecklistGondola() {
		return ok(checklistmomgondola.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result momChecklistMCWP() {
		return ok(checklistmommcwp.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result momChecklistScaffold() {
		return ok(checklistmomscaffold.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result momCOSScaffold() {
		return ok(cosmomscaffold.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result momCOSGondola() {
		return ok(cosmomgondola.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result momCOSMCWP() {
		return ok(cosmommcwp.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result momCOSFormwork() {
		return ok(cosmomformwork.render());
	}
	
	
	
	
}












