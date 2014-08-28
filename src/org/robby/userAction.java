package org.robby;

import java.util.Map;

import com.opensymphony.xwork2.ActionContext;

public class userAction extends baseAction{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String username;
	String password;
	String password2;
	String content;
	Hbaself hbase;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword2() {
		return password2;
	}
	public void setPassword2(String password2) {
		this.password2 = password2;
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public userAction(){
		username = new String();
		password = new String();
		hbase = Hbaself.getInstance();
	}
	
	public String post() throws Exception{
		if(hbase.post(login_user, content)){
			errmsg = "发布成功";
		}else
			errmsg = "发布失败";
		
		return SUCCESS;
	}
	
	@SuppressWarnings("unchecked")
	public String login() throws Exception{
		long id = hbase.checkPassword(username, password);
		if( id > 0){
			login_user = username;
			login_id = id;
			ActionContext actionContext = ActionContext.getContext();
			Map session = actionContext.getSession();
			session.put("login_user", login_user);
			session.put("login_id", login_id);
		}else{
			errmsg = "登录失败！";
			return ERROR;
		}
		return SUCCESS;
	}
	
	public String unfollow() throws Exception {
		if(hbase.unfollow(login_user, username))
			errmsg = "取消关注成功";
		else
			errmsg = "取消关注失败";

		return SUCCESS;
	}
	
	public String follow() throws Exception {
		if(hbase.follow(login_user, username))
			errmsg = "关注" + username + "成功";
		else
			errmsg = "关注失败";

		return SUCCESS;
	}
	
	public String logout() throws Exception{
		login_user = "";
		login_id = 0;
		ActionContext ac = ActionContext.getContext();
		Map<String, Object> session = ac.getSession();
		session.put("login_user", username);
		session.put("login_id", login_id);
		
		return SUCCESS;
	}
	
	public String register() throws Exception{
		if(username.equals(""))
			return ERROR;
		if(username.length() <= 3 || password.length() <= 3){
			errmsg = "注册失败";
			return ERROR;
		}
		if(!password.equals(password2)){
			errmsg = "两次输入的密码不一致";
			return ERROR;
		}
		
		boolean result = hbase.createNewUser(username, password);
		if(result == false)
		{
			errmsg = "创建用户失败";
			return ERROR;
		}
		login_user = username;
		errmsg = "注册成功";
		return SUCCESS;
	}
	
	public String execute() throws Exception{
		return SUCCESS;
	}
}
