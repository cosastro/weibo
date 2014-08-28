package org.robby;

import java.util.Map;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class baseAction extends ActionSupport{
	String errmsg = "";
	String login_user = "";
	long login_id;

	public baseAction(){
		try {
			errmsg = new String();
			ActionContext actionContext = ActionContext.getContext();
			Map session = actionContext.getSession();
			login_user = (String) session.get("login_user");
			login_id = (Long) session.get("login_id");
		} catch (Exception e) {
			login_user = "";
			login_id = 0;
		}
	}
	
	public String getErrmsg() {
		return errmsg;
	}
	public void setErrmsg(String errmsg) {
		this.errmsg = errmsg;
	}
	public long getLogin_id() {
		return login_id;
	}
	public void setLogin_id(long login_id) {
		this.login_id = login_id;
	}
	public String getLogin_user() {
		return login_user;
	}

	public void setLogin_user(String login_user) {
		this.login_user = login_user;
	}

	public String execute() throws Exception{
		
		return errmsg;
	}
}
