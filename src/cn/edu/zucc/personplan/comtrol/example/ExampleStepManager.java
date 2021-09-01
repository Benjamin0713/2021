package cn.edu.zucc.personplan.comtrol.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.edu.zucc.personplan.itf.IStepManager;
import cn.edu.zucc.personplan.model.BeanPlan;
import cn.edu.zucc.personplan.model.BeanStep;
import cn.edu.zucc.personplan.model.BeanUser;
import cn.edu.zucc.personplan.util.BaseException;
import cn.edu.zucc.personplan.util.BusinessException;
import cn.edu.zucc.personplan.util.DBUtil;
import cn.edu.zucc.personplan.util.DbException;

public class ExampleStepManager implements IStepManager {

	@Override
	public void add(BeanPlan plan, String name, String planstartdate,
			String planfinishdate) throws BaseException {
		if (name==null||"".equals(name)) throw new BusinessException("步骤名称必须提供");
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			int plan_id=plan.getPlan_id();
			int step_ord=0;
			String sql="select step_id from tbl_step where plan_id = ? and step_name = ?";
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
			pst.setString(2,name);
			java.sql.ResultSet rs=pst.executeQuery();
			if(rs.next()){
				rs.close();
				pst.close();
				throw new BusinessException("同名步骤已存在");
			}
			rs.close();
			pst.close();

			sql="select max(step_order) from tbl_step where plan_id = ?";
			pst=conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
			rs=pst.executeQuery();
			if(rs.next()){
				step_ord=rs.getInt(1)+1;
			}else {
				step_ord=1;
			}
			rs.close();
			pst.close();

			sql="insert into tbl_step(plan_id,step_order,step_name,plan_begin_time,plan_end_time) values(?,?,?,?,?)";
			pst=conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
			pst.setInt(2,step_ord);
			pst.setString(3,name);
			pst.setString(4,planstartdate);
			pst.setString(5,planfinishdate);
			pst.execute();
			pst.close();

			BeanStep sp=new BeanStep();
			sql="select max(step_id) from tbl_step where plan_id = ?";
			pst=conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
			rs=pst.executeQuery();
			if(rs.next()){
				int pid=rs.getInt(1);
				sp.setStep_id(pid);
			}
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

	@Override
	public List<BeanStep> loadSteps(BeanPlan plan) throws BaseException {
		List<BeanStep> result=new ArrayList<BeanStep>();
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String sql="select * from tbl_step where plan_id = ? order by step_order";
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setInt(1,plan.getPlan_id());
			java.sql.ResultSet rs=pst.executeQuery();
			while(rs.next()){
				BeanStep sp=new BeanStep();
				sp.setStep_id(rs.getInt(1));
				sp.setPlan_id(rs.getInt(2));
				sp.setStep_order(rs.getInt(3));
				sp.setStep_name(rs.getString(4));
				sp.setPlan_begin_time(rs.getDate(5));
				sp.setPlan_end_time(rs.getDate(6));
				sp.setReal_begin_time(rs.getDate(7));
				sp.setReal_end_time(rs.getDate(8));
				result.add(sp);
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
	public void deleteStep(BeanStep step) throws BaseException {
		// 删除
		Connection conn=null;
		try {
			conn = DBUtil.getConnection();
			String sql="select step_order,user_id,tbl_plan.plan_id from tbl_step,tbl_plan where tbl_plan.plan_id=tbl_step.plan_id and step_id = "+step.getStep_id();
			java.sql.Statement st=conn.createStatement();
			java.sql.ResultSet rs=st.executeQuery(sql);
			int step_ord=0;
			String plan_user_id=null;
			int plan_id=0;
			if(rs.next()){
				step_ord=rs.getInt(1);
				plan_user_id=rs.getString(2);
				plan_id=rs.getInt(3);
			}else {
				rs.close();
				st.close();
				throw new BusinessException("该步骤不存在");
			}
			rs.close();
			if(!BeanUser.currentLoginUser.getUser_id().equals(plan_user_id)){
				st.close();
				throw new BusinessException("不能删除别人的步骤");
			}

			sql="delete from tbl_step where step_id = "+step.getStep_id();
			st.execute(sql);
			st.close();

			sql="update tbl_step set step_order=step_order-1 where plan_id = ? and step_order > "+step.getStep_id();
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
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

	@Override
	public void startStep(BeanStep step) throws BaseException {
		// TODO Auto-generated method stub
		Connection conn=null;
		if(step.getReal_begin_time()!=null) throw new BusinessException("步骤已经开始");
		try {
			conn = DBUtil.getConnection();
			String sql="select plan_begin_time,real_begin_time from tbl_step where step_id = ?";
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setInt(1,step.getStep_id());
			java.sql.ResultSet rs=pst.executeQuery();
			Date plan_begin_time=null;
			Date real_begin_time=null;
			if(rs.next()){
				plan_begin_time=rs.getDate(1);
				real_begin_time=rs.getDate(2);
			}else {
				rs.close();
				pst.close();
				throw new BusinessException("步骤不存在");
			}
			rs.close();

			sql="update tbl_step set real_begin_time = ? where step_id = ?";
			pst=conn.prepareStatement(sql);
			pst.setTimestamp(1,new java.sql.Timestamp(System.currentTimeMillis()));
			pst.setInt(2,step.getStep_id());
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

	@Override
	public void finishStep(BeanStep step) throws BaseException {
		// TODO Auto-generated method stub
		Connection conn=null;
		if(step.getReal_begin_time()!=null) throw new BusinessException("步骤已经结束");
		try {
			conn = DBUtil.getConnection();
			String sql="select plan_end_time,real_end_time from tbl_step where step_id = ?";
			java.sql.PreparedStatement pst=conn.prepareStatement(sql);
			pst.setInt(1,step.getStep_id());
			java.sql.ResultSet rs=pst.executeQuery();
			Date plan_end_time;
			Date real_end_time;
			if(rs.next()){
				plan_end_time=rs.getDate(1);
				real_end_time=rs.getDate(2);
			}else {
				rs.close();
				pst.close();
				throw new BusinessException("步骤不存在");
			}

			rs.close();
			sql="update tbl_step set real_end_time = ? where step_id = ?";
			pst=conn.prepareStatement(sql);
			pst.setTimestamp(1,new java.sql.Timestamp(System.currentTimeMillis()));
			pst.setInt(2,step.getStep_id());
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

	@Override
	public void moveUp(BeanStep step) throws BaseException {
		// TODO Auto-generated method stub
		int step_id = step.getStep_id();
		if(step.getReal_begin_time()!=null) throw new BusinessException("步骤已经开始，不可以上移");
		Connection conn=null;
		try {
			conn=DBUtil.getConnection();
			conn.setAutoCommit(false);
			String sql="select plan_id,step_order from tbl_step where step_id = ?";
			java.sql.PreparedStatement pst = conn.prepareStatement(sql);
			pst.setInt(1,step_id);
			java.sql.ResultSet rs = pst.executeQuery();
			int plan_id=0, step_ord=0;
			if(rs.next()){
				plan_id = rs.getInt(1);
				step_ord = rs.getInt(2);
			}else {
				rs.close();
				pst.close();
				throw new BusinessException("该步骤不存在");
			}
			rs.close();

			sql="select min(step_order) from tbl_step where real_end_time is  null" +
					" and plan_id = ?";
			pst = conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
			rs = pst.executeQuery();
			if (rs.next()){
				if(rs.getInt(1)==step_ord){
					throw new BusinessException("该步骤为第一步 无法上移");
				}
			}
			rs.close();

			sql="select step_id from tbl_step where step_order =  "+String.valueOf(step_ord-1)
					+" and plan_id = "+plan_id;
			pst = conn.prepareStatement(sql);
			rs=pst.executeQuery();
			int step_id_up=0;
			if(rs.next()){
				step_id_up = rs.getInt(1);
			}
			sql="update tbl_step set step_order="+(step_ord-1)+" where step_id = "+step_id
					+"  and plan_id = "+plan_id;
			String sql2="update tbl_step set step_order= "+step_ord+" where step_id = "+step_id_up
					+"  and plan_id = "+plan_id;
			java.sql.Statement pst1 = conn.createStatement();

			pst1.addBatch(sql);
			pst1.addBatch(sql2);
			pst1.executeBatch();
			conn.commit();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}finally {
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException throwables) {
					throwables.printStackTrace();
				}
			}
		}
	}

	@Override
	public void moveDown(BeanStep step) throws BaseException {
		// TODO Auto-generated method stub
		if(step.getReal_begin_time()!=null) throw new BusinessException("步骤已经结束，不可以下移");
		int step_id = step.getStep_id();
		Connection conn=null;
		try {
			conn=DBUtil.getConnection();
			conn.setAutoCommit(false);
			String sql="select plan_id,step_order from tbl_step where step_id = ?";
			java.sql.PreparedStatement pst = conn.prepareStatement(sql);
			pst.setInt(1,step_id);
			java.sql.ResultSet rs = pst.executeQuery();
			int plan_id=0, step_ord=0;
			if(rs.next()){
				plan_id = rs.getInt(1);
				step_ord = rs.getInt(2);
			}else {
				rs.close();
				pst.close();
				throw new BusinessException("该步骤不存在");
			}
			rs.close();

			sql="select max(step_order) from tbl_step where real_end_time is  null" +
					" and plan_id = ?";
			pst = conn.prepareStatement(sql);
			pst.setInt(1,plan_id);
			rs = pst.executeQuery();
			if (rs.next()){
				if(rs.getInt(1)==step_ord){
					throw new BusinessException("该步骤为最后一步 不可以下移");
				}
			}
			sql="select step_id from tbl_step where step_order =  "+String.valueOf(step_ord+1)
					+" and plan_id = "+plan_id ;
			pst = conn.prepareStatement(sql);
			rs=pst.executeQuery();
			int step_id_down=0;
			if(rs.next()){
				step_id_down = rs.getInt(1);
			}
			sql="update tbl_step set step_order=step_order+1 where step_id = "+step_id
					+"  and plan_id = "+plan_id;
			String sql2="update tbl_step set step_order= "+step_ord+" where step_id = "+step_id_down
					+"  and plan_id = "+plan_id;
			java.sql.Statement pst1 = conn.createStatement();

			pst1.addBatch(sql);
			pst1.addBatch(sql2);
			pst1.executeBatch();
			conn.commit();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}finally {
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException throwables) {
					throwables.printStackTrace();
				}
			}
		}
	}

}
