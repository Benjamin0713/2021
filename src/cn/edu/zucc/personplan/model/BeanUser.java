package cn.edu.zucc.personplan.model;

import java.util.Date;

public class BeanUser {
	public static BeanUser currentLoginUser=null;
	private String user_id;
	private Date Register_time;

	public String getUser_pwd() {
		return user_pwd;
	}

	public void setUser_pwd(String user_pwd) {
		this.user_pwd = user_pwd;
	}

	private String user_pwd;

	public BeanUser() {
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public String getUser_id() {
		return user_id;
	}


	public Date getRegister_time() {
		return Register_time;
	}

	public void setRegister_time(Date register_time) {
		Register_time = register_time;
	}
}
