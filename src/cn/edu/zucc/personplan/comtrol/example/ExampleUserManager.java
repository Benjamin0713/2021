package cn.edu.zucc.personplan.comtrol.example;

import cn.edu.zucc.personplan.itf.IUserManager;
import cn.edu.zucc.personplan.model.BeanUser;
import cn.edu.zucc.personplan.util.BaseException;
import cn.edu.zucc.personplan.util.BusinessException;
import cn.edu.zucc.personplan.util.DBUtil;
import cn.edu.zucc.personplan.util.DbException;

import java.sql.*;
import java.util.Date;

public class ExampleUserManager implements IUserManager {

	@Override
	public BeanUser reg(String userid, String pwd,String pwd2) throws BaseException {
		//用户注册
		if(userid == null || "".equals(userid)) throw new BusinessException("账号不能为空");
		if(pwd == null || "".equals(pwd)) throw new BusinessException("密码不能为空");
		if(!pwd.equals(pwd2)) throw new BusinessException("两次密码不一致");
		Connection conn = null;
		try {
			conn = DBUtil.getConnection();
			//检查用户是否已存在
			String sql = "select user_id from tbl_user where user_id = ?";
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1,userid);
			ResultSet rs = pst.executeQuery();
			if(rs.next()){
				rs.close();
				pst.close();
				throw new BusinessException("用户已经存在");
			}
			rs.close();
			pst.close();
			sql = "insert into tbl_user(user_id,user_pwd,register_time) values(?,?,?)";
			pst=conn.prepareStatement(sql);
			pst.setString(1,userid);
			pst.setString(2,pwd);
			pst.setTimestamp(3,new Timestamp(System.currentTimeMillis()));
			pst.execute();
			BeanUser user = new BeanUser();
			user.setRegister_time(new Date());
			user.setUser_id(userid);
			return user;
		}catch (SQLException e){
			throw new DbException(e);
		}finally {
			if(conn!=null){
				try{
					conn.close();
				}catch (SQLException ex){
					ex.printStackTrace();
				}
			}
		}
// 		return null;
	}

	
	@Override
	public BeanUser login(String userid, String pwd) throws BaseException {
		// 用户登录检验
		if(userid == null || "".equals(userid)) throw new BusinessException("账号不能为空");
		if(pwd == null || "".equals(pwd)) throw new BusinessException("密码不能为空");
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String sql = "select user_id from tbl_user where user_id = ?";
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1,userid);
			ResultSet rs = pst.executeQuery();
			if(!rs.next()){
				rs.close();
				pst.close();
				throw new BusinessException("用户不存在");
			}
			rs.close();
			pst.close();
			sql = "select user_pwd from tbl_user where user_pwd = ?";
			pst=conn.prepareStatement(sql);
			pst.setString(1,pwd);
			rs = pst.executeQuery();
			if(!rs.next()){
				rs.close();
				pst.close();
				throw new BusinessException("密码错误");
			}
			BeanUser user = new BeanUser();
			user.setUser_pwd(pwd);
			user.setUser_id(userid);
			return user;
		}catch (SQLException e){
			throw new DbException(e);
		}finally {
			if(conn!=null){
				try{
					conn.close();
				}catch (SQLException ex){
					ex.printStackTrace();
				}
			}
		}
// 		return null;
	}


	@Override
	public void changePwd(BeanUser user, String oldPwd, String newPwd,
			String newPwd2) throws BaseException {
		// 修改密码
		if(!oldPwd.equals(user.getUser_pwd())) throw new BusinessException("原密码错误");
		if(oldPwd.equals(newPwd)) throw new BusinessException("新密码不能与原密码一致");
		if(!newPwd.equals(newPwd2)) throw new BusinessException("与第一次输入新密码不一致");
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String sql = "select * from tbl_user where user_id=?";
			PreparedStatement pst = conn.prepareStatement(sql);
			String userid=user.getUser_id();
			pst.setString(1,userid);
			ResultSet rs = pst.executeQuery();
			if(!rs.next()){
				rs.close();
				pst.close();
				throw new BusinessException("登录用户不存在");
			}
			rs.close();
			pst.close();
			sql = "update tbl_user set user_pwd=? where user_id=?";
			pst = conn.prepareStatement(sql);
			pst.setString(1,newPwd);
			pst.setString(2,userid);
			pst.execute();
			pst.close();
		}catch (SQLException e){
			throw new DbException(e);
		}finally {
			if(conn!=null){
				try{
					conn.close();
				}catch (SQLException ex){
					ex.printStackTrace();
				}
			}
		}
	}

}
