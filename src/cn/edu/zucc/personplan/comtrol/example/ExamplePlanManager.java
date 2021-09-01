package cn.edu.zucc.personplan.comtrol.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cn.edu.zucc.personplan.itf.IPlanManager;
import cn.edu.zucc.personplan.model.BeanPlan;
import cn.edu.zucc.personplan.model.BeanUser;
import cn.edu.zucc.personplan.util.BaseException;
import cn.edu.zucc.personplan.util.BusinessException;
import cn.edu.zucc.personplan.util.DBUtil;
import cn.edu.zucc.personplan.util.DbException;

public class ExamplePlanManager implements IPlanManager {

	@Override
	public BeanPlan addPlan(String name) throws BaseException {
		// 添加记录
		if (name==null||"".equals(name)) throw new BusinessException("计划名称必须提供");
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String user_id=BeanUser.currentLoginUser.getUser_id();
			int plan_ord=0;
			String sql="select plan_id from tbl_plan where user_id=? and plan_name=?";
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setString(1,user_id);
			pst.setString(2,name);
			java.sql.ResultSet rs=pst.executeQuery();
			if(rs.next()){
				rs.close();
				pst.close();
				throw new BusinessException("同名计划已存在");
			}
			rs.close();
			pst.close();

			sql="select max(plan_order) from tbl_plan where user_id = ?";
			pst=conn.prepareStatement(sql);
			pst.setString(1,user_id);
			rs=pst.executeQuery();
			if(rs.next()){
				plan_ord=rs.getInt(1)+1;
			}else {
				plan_ord=1;
			}
			rs.close();
			pst.close();
			sql="insert into tbl_plan(user_id,plan_order,plan_name,create_time,step_count," +
					"start_step_count,finished_step_count)" +
					"values(?,?,?,?,0,0,0)";
			pst=conn.prepareStatement(sql);
			pst.setString(1,user_id);
			pst.setInt(2,plan_ord);
			pst.setString(3,name);
			pst.setTimestamp(4,new java.sql.Timestamp(System.currentTimeMillis()));
			pst.execute();
			pst.close();

			BeanPlan p=new BeanPlan();
			sql="select max(plan_id) from tbl_plan where user_id =?";
			pst=conn.prepareStatement(sql);
			pst.setString(1,user_id);
			rs=pst.executeQuery();
			if(rs.next()){
				int pid=rs.getInt(1);
				p.setPlan_id(pid);
			}
			rs.close();
			pst.close();
			p.setUser_id(user_id);
			p.setPlan_order(plan_ord);
			p.setPlan_name(name);
			p.setCreate_time(new java.sql.Timestamp(System.currentTimeMillis()));
			p.setStep_count(0);
			p.setStart_step_count(0);
			p.setFinished_step_count(0);
			return p;
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

	@Override
	public List<BeanPlan> loadAll() throws BaseException {
		List<BeanPlan> result=new ArrayList<BeanPlan>();
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String sql="select * from tbl_plan where user_id = ? order by plan_order";
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setString(1,BeanUser.currentLoginUser.getUser_id());
			java.sql.ResultSet rs=pst.executeQuery();
			while(rs.next()){
				BeanPlan bp=new BeanPlan();
				bp.setPlan_id(rs.getInt(1));
				bp.setUser_id(rs.getString(2));
				bp.setPlan_order(rs.getInt(3));
				bp.setPlan_name(rs.getString(4));
				bp.setCreate_time(rs.getDate(5));
				bp.setStep_count(rs.getInt(6));
				bp.setStart_step_count(rs.getInt(7));
				bp.setFinished_step_count(rs.getInt(8));
				result.add(bp);
			}
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
		return result;
	}

	@Override
	public void deletePlan(BeanPlan plan) throws BaseException {
		int plan_id=plan.getPlan_id();
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String sql="select count(*) from tbl_step where plan_id ="+plan_id;
			java.sql.Statement st=conn.createStatement();
			java.sql.ResultSet rs=st.executeQuery(sql);
			if(rs.next()){
				if(rs.getInt(1)>0){
					rs.close();
					st.close();
					throw new BusinessException("计划已存在步骤，不能删除");
				}
			}
			rs.close();

			sql="select plan_order,user_id from tbl_plan where plan_id ="+plan_id;
			rs=st.executeQuery(sql);

			int plan_ord=0;
			String plan_user_id=null;
			if(rs.next()){
				plan_ord=rs.getInt(1);
				plan_user_id=rs.getString(2);
			}else {
				rs.close();
				st.close();
				throw new BusinessException("该计划不存在");
			}
			if(!BeanUser.currentLoginUser.getUser_id().equals(plan_user_id)){
				st.close();
				throw new BusinessException("不能删除别人的计划");
			}

			sql="delete from tbl_plan where plan_id ="+plan_id;
			st.execute(sql);
			st.close();

			sql="update tbl_plan set plan_order=plan_order-1 where user_id = ? and plan_order >"+plan_ord;
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setString(1,plan_user_id);
			pst.execute();
			rs.close();
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
