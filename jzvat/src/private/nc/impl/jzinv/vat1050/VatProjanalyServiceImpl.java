package nc.impl.jzinv.vat1050;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.itf.jzinv.vat1050.IVatProjanalyService;
import nc.jdbc.framework.processor.BeanListProcessor;
import nc.jdbc.framework.processor.ResultSetProcessor;
import nc.vo.jzinv.inv0510.OpenHVO;
import nc.vo.jzinv.pub.IJzinvBillType;
import nc.vo.jzinv.pub.tool.InSqlManager;
import nc.vo.jzinv.pub.utils.SafeCompute;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
import nc.vo.jzinv.vat0510.VatProjtaxsetVO;
import nc.vo.jzinv.vat1045.VatIncretaxpreVO;
import nc.vo.jzinv.vat1050.VatProjanalyBVO;
import nc.vo.jzinv.vat1050.VatProjanalyVO;
import nc.vo.jzinv.vatpub.VatPubMetaNameConsts;
import nc.vo.jzpm.in2005.InIncomeVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

import org.apache.commons.lang.StringUtils;

public class VatProjanalyServiceImpl implements IVatProjanalyService{
	/**
	 * 纳税组织定义orgSetVO
	 */
    private VatTaxorgsetVO orgSetVO;
	private BaseDAO dao = null;
	private BaseDAO getDao(){
		if(null == dao){
			dao = new BaseDAO();
		}
		return dao;
	}
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getOpenInvMnyInfo(String pk_corp, 
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_collect.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_collect.nthapplytaxmny, 0.0)) nsalemny, ");
		sql.append(" sum(COALESCE(b_collect.nthapplymny, 0.0)) ntaxablesalemny, ");
		sql.append(" sum(COALESCE(b_collect.nthapplytaxmny, 0.0) - COALESCE(b_collect.nthapplymny, 0.0)) nsaletaxmny ");
		sql.append(" FROM jzinv_open_collect b_collect ");
		sql.append(" inner join jzinv_open h ");
		sql.append(" on b_collect.pk_open = h.pk_open ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_collect.dr, 0) = 0 ");	
    
        sql.append(" and (h.bisopen = 'Y' or h.bisgoldopen = 'Y') ");    //过滤出是否开票成功为Y和是否传金税开票成功的单据
        sql.append(" and b_collect.pk_appcorp = '" + pk_corp + "' ");//过滤开票的开票申请公司
        //yuliang2 add 对【发票已作废】标识的过滤 2017-03-15
        sql.append(" and isnull(h.bisabolish, 'N') = 'N' ");
        
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_collect.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
 
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.dopendate like '" + vperiod + "%' ");
        }
        sql.append(" group by  b_collect.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nsalemny = rs.getString("nsalemny");
							String ntaxablesalemny = rs.getString("ntaxablesalemny");
							String nsaletaxmny = rs.getString("nsaletaxmny");
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NSALEMNY, new UFDouble(nsalemny));
								inMap.put(VatProjanalyVO.NTAXABLESALEMNY, new UFDouble(ntaxablesalemny));
								inMap.put(VatProjanalyVO.NSALETAXMNY, new UFDouble(nsaletaxmny));
								outMap.put(pk_projectbase, inMap);
							}							
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getReceInvMnyInfo(String pk_corp,
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_detail.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_detail.nthrecemny, 0.0)) ntaxableinmny, ");
		sql.append(" sum(COALESCE(b_detail.nthrecetaxmny, 0.0) - COALESCE(b_detail.nthrecemny, 0.0)) nintaxmny ");
		sql.append(" FROM jzinv_receive_detail b_detail ");
		sql.append(" inner join jzinv_receive h ");
		sql.append(" on b_detail.pk_receive = h.pk_receive ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_detail.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_detail.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.vtaxperiod like '" + vperiod + "%' ");
        }
        sql.append(" group by  b_detail.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String ntaxableinmny = rs.getString("ntaxableinmny");
							String nintaxmny = rs.getString("nintaxmny");
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NTAXABLEINMNY, new UFDouble(ntaxableinmny));
								inMap.put(VatProjanalyVO.NINTAXMNY, new UFDouble(nintaxmny));
								outMap.put(pk_projectbase, inMap);
							}							
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getIncataxmnyInfo(String pk_corp, List<String> pk_projectbaseList,
			String vperiod) throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_detail.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_detail.nthrecetaxmny, 0.0) - COALESCE(b_detail.nthrecemny, 0.0)) nthhadauthtaxmny ");
		sql.append(" FROM jzinv_receive_detail b_detail ");
		sql.append(" inner join jzinv_receive h ");
		sql.append(" on b_detail.pk_receive = h.pk_receive ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_detail.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        //sql.append(" and h.bisauthen = 'Y' ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_detail.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.vtaxperiod like '" + vperiod + "%' ");//纳税期是认证通过后才有的值
        }
        sql.append(" group by  b_detail.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nthhadauthtaxmny = rs.getString("nthhadauthtaxmny");
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NTHHADAUTHTAXMNY, new UFDouble(nthhadauthtaxmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getShincataxmnyInfo(String pk_corp, List<String> pk_projectbaseList,
			String vperiod) throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_detail.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_detail.nthrecetaxmny, 0.0) - COALESCE(b_detail.nthrecemny, 0.0)) nshincataxmny ");
		sql.append(" FROM jzinv_receive_detail b_detail ");
		sql.append(" inner join jzinv_receive h ");
		sql.append(" on b_detail.pk_receive = h.pk_receive ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_detail.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_detail.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and (");
        	sql.append("        h.dbilldate <= '" + vperiod + "-31 23:59:59' ");
        	sql.append("        and ((h.dauthendate >= '" + vperiod + "-01 00:00:00' ");
        	sql.append("        and h.bisauthen = 'Y') or (h.bisauthen = 'N'))");
        	sql.append("     )");
        }
        sql.append(" group by  b_detail.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nshincataxmny = rs.getString("nshincataxmny");		
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NSHINCATAXMNY, new UFDouble(nshincataxmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getLastresttaxmnyInfo(String className, String pk_corp, 
			List<String> pk_projectbaseList, String vperiod) throws Exception {
		//1. 根据当前期间+项目找出离当前期间最近的上期期间
		//2. 取出上期期间的留抵税额
		SuperVO vo = (SuperVO) Class.forName(className).newInstance();
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT pk_projectbase, vperiod, COALESCE(nresttaxmny, 0.0) nresttaxmny, ");
		sql.append(" COALESCE(nthrestcutmny, 0.0) nthrestcutmny ");
		//sql.append(" FROM " + vo.getTableName());
		sql.append(" FROM jzinv_vat_projanaly ");
		sql.append(" where isnull(dr, 0) = 0 ");
		sql.append(" and vbillstatus = 1 ");
		sql.append(" and pk_corp = '" + pk_corp + "' ");
		if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
		if(!StringUtils.isEmpty(vperiod)){
			sql.append(" and vperiod < '" + vperiod + "' ");
		}
		sql.append(" order by pk_projectbase, vperiod desc ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nresttaxmny = rs.getString("nresttaxmny");
							String nthrestcutmny = rs.getString("nthrestcutmny");
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NLRESTTAXMNY, new UFDouble(nresttaxmny));
								inMap.put(VatProjanalyVO.NLRESTCUTMNY, new UFDouble(nthrestcutmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getIntoOutMnyInfo(String pk_corp,
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_detail.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_detail.nintranmny, 0.0)) nintooutmny ");//进项抓出
		sql.append(" FROM jzinv_vat_intran_b b_detail ");
		sql.append(" inner join jzinv_vat_intran h ");
		sql.append(" on b_detail.pk_intran = h.pk_intran ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_detail.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        sql.append(" and h.pk_billtype='99ZI' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_detail.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.duration = '" + vperiod + "' ");
        }
        sql.append(" group by  b_detail.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nintooutmny = rs.getString("nintooutmny");		
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NINTOOUTMNY, new UFDouble(nintooutmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getIntoOutMnyNoCutInfo(String pk_corp,
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_detail.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_detail.nintranmny, 0.0)) nthauthnocutmny ");//认证申报不可抵扣
		sql.append(" FROM jzinv_vat_intran_b b_detail ");
		sql.append(" inner join jzinv_vat_intran h ");
		sql.append(" on b_detail.pk_intran = h.pk_intran ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_detail.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        sql.append(" and h.pk_billtype='99ZN' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_detail.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.duration = '" + vperiod + "' ");
        }
        sql.append(" group by  b_detail.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nthauthnocutmny = rs.getString("nthauthnocutmny");		
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NTHAUTHNOCUTMNY, new UFDouble(nthauthnocutmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}
	/**
	 * 获取前期认证本期抵扣额
	 * <项目basePK, <前期认证本期抵扣字段, 前期认证本期抵扣额>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getIntoOutMnyThCutInfo(String pk_corp,
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT b_detail.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(b_detail.nintranmny, 0.0)) nlauththcutmny ");//认证申报抵扣转回
		sql.append(" FROM jzinv_vat_intran_b b_detail ");
		sql.append(" inner join jzinv_vat_intran h ");
		sql.append(" on b_detail.pk_intran = h.pk_intran ");
		sql.append(" where isnull(h.dr,0) = 0 ");
        sql.append(" and isnull(b_detail.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        sql.append(" and h.pk_billtype='99ZO' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b_detail.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.duration = '" + vperiod + "' ");
        }
        sql.append(" group by  b_detail.pk_projectbase ");
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = null;
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nlauththcutmny = rs.getString("nlauththcutmny");		
							if(!outMap.containsKey(pk_projectbase)){
								inMap = new HashMap<String, UFDouble>();
								inMap.put(VatProjanalyVO.NLAUTHTHCUTMNY, new UFDouble(nlauththcutmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, List<SuperVO>> getSubReceVOList(String pk_corp,
			List<String> pk_projectbaseList, String vperiod) throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT ");
		sql.append(" h.pk_corp pk_rececorp, ");//收票公司
		sql.append(" h.pk_projectbase pk_projectbase, ");//项目基本主键
		sql.append(" h.pk_project pk_project, ");//项目
		sql.append(" h.pk_supplier pk_supplier, ");//开票单位
		sql.append(" h.pk_supplierbase pk_supplierbase, ");//开票单位基本主键
		sql.append(" h.vtaxsuppliernumber vtaxsuppliernumber, ");//开票单位纳税人识别号
		sql.append(" h.vinvcode vinvcode, ");//凭证代码
		sql.append(" h.vinvno vinvno, ");//凭证号码
		sql.append(" COALESCE(h.ntaxrate, 0.0) ntaxrate, ");//扣除类型
		
		sql.append(" h.ninvtaxmny nmny, ");//金额
		sql.append(" COALESCE(h.nfhaddducttaxmny, 0.0) nhadcutmny, ");//费用已扣除金额
		sql.append(" (COALESCE(h.ninvtaxmny, 0.0)-COALESCE(h.nfhaddducttaxmny, 0.0)) nthcutmny, ");//本期费用扣除金额
		sql.append(" 0.0 nthrestcutmny, ");//本期费用留抵扣除金额
		sql.append(" h.dopendate dopendate ");//开票日期
		sql.append(" FROM jzinv_receive h where ");
        sql.append(" isnull(h.dr, 0) = 0 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "'");
        sql.append(" and h.pk_billtype in ('" +IJzinvBillType.JZINV_RECEIVE_SUB + "','"+ IJzinvBillType.JZINV_RECEIVE_SUB_W + "') ");
        sql.append(" and (isnull(h.ninvtaxmny,0)-isnull(h.nfhaddducttaxmny,0)) > 0 ");//分包收票发票金额-费用已抵扣金额 > 0
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and h.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.dbilldate <= '" + vperiod + "-31 23:59:59' ");//单据日期<=期间
        }
        Map<String, List<SuperVO>> map = (Map<String, List<SuperVO>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, List<SuperVO>> map = new HashMap<String, List<SuperVO>>();
						List<SuperVO> bvoList = null;
						while (rs.next()) {	
							VatProjanalyBVO bvo = new VatProjanalyBVO();
							String pk_rececorp = rs.getString("pk_rececorp");
							String pk_project = rs.getString("pk_project");
							String pk_projectbase = rs.getString("pk_projectbase");
							String pk_supplier = rs.getString("pk_supplier");
							String pk_supplierbase = rs.getString("pk_supplierbase");
							String vtaxsuppliernumber = rs.getString("vtaxsuppliernumber");
							String vinvcode = rs.getString("vinvcode");
							String vinvno = rs.getString("vinvno");
							String ntaxrate = rs.getString("ntaxrate");
							String nmny = rs.getString("nmny");
							String nhadcutmny = rs.getString("nhadcutmny");
							String nthcutmny = rs.getString("nthcutmny");
							String nthrestcutmny = rs.getString("nthrestcutmny");
							String dopendate = rs.getString("dopendate");
							
							bvo.setPk_rececorp(pk_rececorp);
							bvo.setPk_project(pk_project);
							bvo.setPk_projectbase(pk_projectbase);
							bvo.setPk_supplier(pk_supplier);
							bvo.setPk_supplierbase(pk_supplierbase);
							bvo.setVtaxsuppliernumber(vtaxsuppliernumber);
							bvo.setVinvcode(vinvcode);
							bvo.setVinvno(vinvno);
							bvo.setNtaxrate(new UFDouble(ntaxrate));
							bvo.setNmny(new UFDouble(nmny));
							bvo.setNhadcutmny(new UFDouble(nhadcutmny));
							bvo.setNthcutmny(new UFDouble(nthcutmny));
							bvo.setNthrestcutmny(new UFDouble(nthrestcutmny));
							bvo.setDopendate(new UFDate(dopendate));
							bvo.setIlegalvalidintype(0);
							bvo.setVcutproject("提供建筑服务");		
							
							if(map.containsKey(pk_projectbase)){
								bvoList = map.get(pk_projectbase);
							}
							else{
								bvoList = new ArrayList<SuperVO>();							
							}
							bvoList.add(bvo);
							map.put(pk_projectbase, bvoList);
						}
						return map.size() > 0 ? map : null;
					}
				});
		return map;
	}
	
	public Map<String, Map<String, UFDouble>> getProjsaleInfoMny(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException{
		Map<String, UFDouble> gnlSetleMap = getGnlSetleMny(pk_corp, pk_projectbaseList, vperiod);
		Map<String, UFDouble> othSetleMap = getOthSetleMny(pk_corp, pk_projectbaseList, vperiod);
		Map<String, UFDouble> prereceMap = getPrereceMny(pk_corp, pk_projectbaseList, vperiod);
	    
		Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
		setProjSaleMny(map, gnlSetleMap);
		setProjSaleMny(map, othSetleMap);
		setProjSaleMny(map, prereceMap);
		return map;
	}
	private void setProjSaleMny(Map<String, Map<String, UFDouble>> map, Map<String, UFDouble> dataMap){
		Map<String, UFDouble> inMap = null;
		if(null != dataMap && dataMap.size() > 0){
			for(Entry<String, UFDouble> entry : dataMap.entrySet()){
				String pk_projectbase = entry.getKey();
				UFDouble nsettlemny = entry.getValue();
				if(map.containsKey(pk_projectbase)){
					inMap = map.get(pk_projectbase);
					inMap.put(VatPubMetaNameConsts.NPROJSALEMNY, SafeCompute.add(inMap.get(VatPubMetaNameConsts.NPROJSALEMNY), nsettlemny));
				}
				else{
					inMap = new HashMap<String, UFDouble>();
					inMap.put(VatPubMetaNameConsts.NPROJSALEMNY, nsettlemny);
				}
				map.put(pk_projectbase, inMap);
			}
		}
	}
	/**
	 * 获取总承包合同结算的<项目, 本次结算金额-预收款扣回金额>的Sql
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusiessException 
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	private Map<String, UFDouble> getGnlSetleMny(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException{
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT h.pk_projectbase pk_projectbase, ");
        sql.append(" sum(COALESCE(h.nthsettletaxmny, 0.0) - COALESCE(h.nthpregetbacktaxmny, 0.0)) nsettlemny ");
        sql.append(" FROM jzinv_cm_gnrlcontsettle h ");
        sql.append(" where ");
        sql.append(" isnull(h.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and h.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.dbilldate like '%" + vperiod + "%' ");
        }
        sql.append(" group by h.pk_projectbase ");
        Map<String, UFDouble> map = (Map<String, UFDouble>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}						
						Map<String, UFDouble> map = new HashMap<String, UFDouble>();
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nsettlemny = rs.getString("nsettlemny");		
							if(!map.containsKey(pk_projectbase)){
								map.put(pk_projectbase, new UFDouble(nsettlemny));
							}
						}
						return map.size() > 0 ? map : null;
					}

				});
		return map;
	}
	/**
	 * 获取其他收入合同结算的<项目, 本次结算金额-预收款扣回金额>的Sql
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException 
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	private Map<String, UFDouble> getOthSetleMny(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException{
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT h.pk_projectbase pk_projectbase, ");
        sql.append(" sum(COALESCE(h.ncusettlebasetaxmny, 0.0) - COALESCE(h.ncuyfkkhbasetaxmny, 0.0)) nsettlemny ");
        sql.append(" FROM jzinv_cm_othcontsettle h ");
        sql.append(" where ");
        sql.append(" isnull(h.dr, 0) = 0 ");	
        sql.append(" and h.vbillstatus = 1 ");
        sql.append(" and h.pk_corp = '" + pk_corp + "' ");
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and h.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        if(!StringUtils.isEmpty(vperiod)){
        	sql.append(" and h.dbilldate like '%" + vperiod + "%' ");
        }
        sql.append(" group by h.pk_projectbase ");
        Map<String, UFDouble> map = (Map<String, UFDouble>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}						
						Map<String, UFDouble> map = new HashMap<String, UFDouble>();
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nsettlemny = rs.getString("nsettlemny");		
							if(!map.containsKey(pk_projectbase)){
								map.put(pk_projectbase, new UFDouble(nsettlemny));
							}
						}
						return map.size() > 0 ? map : null;
					}

				});
		return map;
	}
	/**
	 * 获取收款单的<项目, 本期预收款金额>的Sql
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException 
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	private Map<String, UFDouble> getPrereceMny(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException{
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT a.pk_projectbase pk_projectbase, ");
		sql.append(" sum(COALESCE(ninbasemny, 0.0)) ninbasemny ");
		sql.append(" FROM "); 
        sql.append(" (SELECT oth.pk_projectbase, sum(oth.ninbasemny) ninbasemny ");
        sql.append("   FROM jzinv_pr_in_oth oth ");
        sql.append("   inner join jzinv_pr_in h ");
        sql.append("   on oth.pk_in = h.pk_in ");
        sql.append("   where isnull(oth.dr, 0) = 0 ");
        sql.append("   and isnull(h.dr, 0) = 0 ");
        sql.append("   and oth.ipaytype = 1 ");
        sql.append("   and h.pk_corp = '" + pk_corp + "' ");
        sql.append("   and h.vbillstatus = 1 ");
        if(!StringUtils.isEmpty(vperiod)){
            sql.append("   and h.dbilldate like '%" + vperiod + "%'");
        }
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and oth.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        sql.append("   group by oth.pk_projectbase ");
        sql.append("   ");
        sql.append("   union all ");
        sql.append(" SELECT b.pk_projectbase, sum(b.ninbasemny) ninbasemny ");
        sql.append("   FROM jzinv_pr_in_b b ");
        sql.append("   inner join jzinv_pr_in h ");
        sql.append("   on b.pk_in = h.pk_in ");
        sql.append("   where isnull(b.dr, 0) = 0 ");
        sql.append("   and isnull(h.dr, 0) = 0 ");
        sql.append("   and b.bispayment = 'Y' ");
        sql.append("   and h.pk_corp = '" + pk_corp + "' ");
        sql.append("   and h.vbillstatus = 1 ");
        if(!StringUtils.isEmpty(vperiod)){
            sql.append("   and h.dbilldate like '%" + vperiod + "%'");
        }
        if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and b.pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        sql.append("   group by b.pk_projectbase ");
        sql.append("  )a");

        sql.append("   group by a.pk_projectbase ");
       
        Map<String, UFDouble> map = (Map<String, UFDouble>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}						
						Map<String, UFDouble> map = new HashMap<String, UFDouble>();
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String ninbasemny = rs.getString("ninbasemny");		
							if(!map.containsKey(pk_projectbase)){
								map.put(pk_projectbase, new UFDouble(ninbasemny));
							}
						}
						return map.size() > 0 ? map : null;
					}

				});
		return map;
	}
	public Map<String, Map<String, UFDouble>> getMnyMapByProj(String className, String pk_corp, 
			List<String> pk_projectbaseList, String vperiod) throws Exception {
		//项目对应的纳税组织定义
		orgSetVO = getTaxorgsetVOByCorp(pk_corp);
		Map<String, Map<String, UFDouble>> openInvMnyMap = getOpenInvMnyInfo(pk_corp, pk_projectbaseList, vperiod);
		Map<String, Map<String, UFDouble>> receInvMnyMap = getReceInvMnyInfo(pk_corp, pk_projectbaseList, vperiod);
		
		Map<String, Map<String, UFDouble>> inAuthMap = getIncataxmnyInfo(pk_corp, pk_projectbaseList, vperiod);
		Map<String, Map<String, UFDouble>> shAuthInMap = getShincataxmnyInfo(pk_corp, pk_projectbaseList, vperiod);// 可认证进项税额
		Map<String, Map<String, UFDouble>> inToOutMap = getIntoOutMnyInfo(pk_corp, pk_projectbaseList, vperiod);// 证进转出
		Map<String, Map<String, UFDouble>> inToOutZcMap = getIntoOutMnyNoCutInfo(pk_corp, pk_projectbaseList, vperiod);//认证申报抵扣转出
		Map<String, Map<String, UFDouble>> inToOutZhMap = getIntoOutMnyThCutInfo(pk_corp, pk_projectbaseList, vperiod);//认证申报抵扣转回
		Map<String, Map<String, UFDouble>> projSaleMnyMap = getProjsaleInfoMny(pk_corp, pk_projectbaseList, vperiod);
		Map<String, Map<String,UFDouble>> lastRestMap = getLastresttaxmnyInfo(className, pk_corp, pk_projectbaseList, vperiod);// 上期留抵税额
		
		Map<String, Map<String, UFDouble>> projMnyMap = new HashMap<String, Map<String, UFDouble>>();	
		Map<String, UFDouble> mnyMap = new HashMap<String, UFDouble>();
		if(null != pk_projectbaseList){
			for(String pk_projectbase : pk_projectbaseList){
				setProjMnyMap(pk_projectbase, openInvMnyMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, receInvMnyMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, inAuthMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, shAuthInMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, inToOutMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, inToOutZcMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, inToOutZhMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, lastRestMap, projMnyMap, mnyMap);
				setProjMnyMap(pk_projectbase, projSaleMnyMap, projMnyMap, mnyMap);
			}
		}		
		return projMnyMap;	
	}
	
	private void setProjMnyMap(String pk_project, Map<String, Map<String, UFDouble>> dataMap, 
			Map<String, Map<String, UFDouble>> projMnyMap, Map<String, UFDouble> mnyMap){
			if(projMnyMap.containsKey(pk_project)){
				Map<String, UFDouble> map = projMnyMap.get(pk_project);
				if(null == map){
					map = new HashMap<String, UFDouble>();
				}
				if(null != dataMap && dataMap.containsKey(pk_project)){
					for (Entry<String, UFDouble> in : dataMap.get(pk_project).entrySet()) {						
						map.put(in.getKey(), in.getValue());
					}	
				}
				projMnyMap.put(pk_project, map);
			}
			else{
				projMnyMap.put(pk_project, null == dataMap ? null : dataMap.get(pk_project));
			}
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, UFDouble>> getBeginRestMnyByProj(String className, String pk_corp, List<String> pk_projectbaseList) throws Exception{
		SuperVO vo = (SuperVO) Class.forName(className).newInstance();
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT pk_projectbase, COALESCE(nresttaxmny, 0.0) nresttaxmny, ");
		sql.append(" COALESCE(nthrestcutmny, 0.0) nthrestcutmny ");
		sql.append(" FROM " + vo.getTableName());
		sql.append(" where isnull(dr, 0) = 0 ");
		sql.append(" and vbillstatus = 1 ");
		sql.append(" and pk_corp = '" + pk_corp + "' ");
		sql.append(" and bisbegin = 'Y' ");
		if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
        	sql.append(" and pk_projectbase in " + InSqlManager.getInSQLValue(pk_projectbaseList));
        }
        Map<String, Map<String, UFDouble>> map = new HashMap<String, Map<String, UFDouble>>();
        map = (Map<String, Map<String, UFDouble>>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, Map<String, UFDouble>> outMap = new HashMap<String, Map<String, UFDouble>>();
						Map<String, UFDouble> inMap = new HashMap<String, UFDouble>();
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String nthrestcutmny = rs.getString("nthrestcutmny");
							String nresttaxmny = rs.getString("nresttaxmny");		
							if(!outMap.containsKey(pk_projectbase)){
								inMap.put(VatProjanalyVO.NLRESTTAXMNY, new UFDouble(nresttaxmny));
								inMap.put(VatProjanalyVO.NLRESTCUTMNY, new UFDouble(nthrestcutmny));
								outMap.put(pk_projectbase, inMap);
							}
						}
						return outMap.size() > 0 ? outMap : null;
					}

				});
		return map;
		
	}
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, VatProjtaxsetVO> getProjtaxsetMap(String pk_projectbase, Integer itaxtype, UFBoolean bisprepay) throws BusinessException{		
			StringBuffer sql = new StringBuffer();
			sql.append(" SELECT projset.pk_projectbase pk_projectbase, ");//项目基本主键
			sql.append(" projset.pk_project pk_project, ");//项目主键
			sql.append(" projset.itaxtype itaxtype, ");//项目计税方式
			sql.append(" projset.icorptaxtype icorptaxtype, ");//公司纳税人类别
			sql.append(" projset.ntaxrate ntaxrate, ");//税率
			sql.append(" projset.nlevyingrate nlevyingrate, ");//征收率
			sql.append(" projset.vbuildpermitno vbuildpermitno, ");//施工许可证号
			sql.append(" projset.pk_corp pk_corp ");//公司
			sql.append(" FROM jzinv_vat_projtaxset projset ");
			sql.append(" inner join jzinv_vat_taxorgset orgset ");
			sql.append(" on projset.pk_corp = orgset.pk_org ");
			sql.append(" where isnull(projset.dr, 0)=0 ");
			sql.append(" and isnull(orgset.dr, 0)=0 ");
			sql.append(" and projset.pk_corp = '" + InvocationInfoProxy.getInstance().getCorpCode() + "' ");
			if(!StringUtils.isEmpty(pk_projectbase)){
				sql.append(" and projset.pk_projectbase = '" + pk_projectbase + "' ");
			}
			if(null != itaxtype){
				sql.append(" and projset.itaxtype = " + itaxtype);
			}
			if(null != bisprepay && bisprepay.booleanValue()){
				sql.append(" and projset.bisprepay = 'Y'");
			}
			
			Map<String, VatProjtaxsetVO> map = (Map<String, VatProjtaxsetVO>) getDao().executeQuery(
					sql.toString(), new ResultSetProcessor() {
						public Object handleResultSet(ResultSet rs)
								throws SQLException {
							if (rs == null) {
								return null;
							}
							Map<String, VatProjtaxsetVO> map = new HashMap<String, VatProjtaxsetVO>();
							while (rs.next()) {
								String pk_projectbase = rs.getString("pk_projectbase");
								String pk_project = rs.getString("pk_project");
								Integer itaxtype = rs.getInt("itaxtype");
								Integer icorptaxtype = rs.getInt("icorptaxtype");
								String ntaxrate = rs.getString("ntaxrate");
								String nlevyingrate = rs.getString("nlevyingrate");
								String vbuildpermitno = rs.getString("vbuildpermitno");
								String pk_corp = rs.getString("pk_corp");
								if(!map.containsKey(pk_projectbase)){
									VatProjtaxsetVO setVO = new VatProjtaxsetVO();
									setVO.setPk_projectbase(pk_projectbase);
									setVO.setPk_project(pk_project);
									setVO.setItaxtype(itaxtype);
									setVO.setIcorptaxtype(icorptaxtype);
									setVO.setNtaxrate(new UFDouble(ntaxrate));
									setVO.setNlevyingrate(new UFDouble(nlevyingrate));
									setVO.setVbuildpermitno(vbuildpermitno);
									setVO.setPk_corp(pk_corp);
									map.put(pk_projectbase, setVO);
								}							
							}
							return map.size() > 0 ? map : null;
						}

					});
			return map;
	}
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, VatTaxorgsetVO> getVatTaxorgsetMap(String pk_corp, List<String> pk_projectbaseList) throws BusinessException{
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT proj.pk_jobbasfil pk_projectbase, ");
		sql.append(" orgset.pk_org pk_org, ");//公司
		sql.append(" orgset.bistaxorg bistaxorg, ");//是否税务组织
		sql.append(" orgset.pk_taxorg pk_taxorg, ");//对应纳税组织
		sql.append(" orgset.pk_parent pk_parent, ");//对应上级税务汇报组织
		sql.append(" orgset.vtaxpayernumber vtaxpayernumber, ");//纳税人名称
		sql.append(" orgset.itaxtype itaxtype, ");//纳税人类别
		sql.append(" orgset.ntaxrate ntaxrate ");//税率
		sql.append(" FROM bd_jobmngfil proj ");
		sql.append(" inner join jzinv_vat_taxorgset orgset ");
		sql.append(" on proj.pk_corp = orgset.pk_org ");
		sql.append(" where isnull(proj.dr,0) = 0 ");
		sql.append(" and isnull(orgset.dr,0) = 0 ");
		sql.append(" and orgset.bissealed = 'N' ");
		sql.append(" and orgset.pk_org = '" + pk_corp + "' ");
		if(null != pk_projectbaseList && pk_projectbaseList.size() > 0){
			sql.append(" and proj.pk_jobbasfil in " + InSqlManager.getInSQLValue(pk_projectbaseList));
		}
        Map<String, VatTaxorgsetVO> map = new HashMap<String, VatTaxorgsetVO>();
        map = (Map<String, VatTaxorgsetVO>) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						Map<String, VatTaxorgsetVO> map = new HashMap<String, VatTaxorgsetVO>();
						while (rs.next()) {
							String pk_projectbase = rs.getString("pk_projectbase");
							String pk_org = rs.getString("pk_org");
							String bistaxorg = rs.getString("bistaxorg");
							String pk_taxorg = rs.getString("pk_taxorg");
							String pk_parent = rs.getString("pk_parent");
							Integer itaxtype = rs.getInt("itaxtype");
							String ntaxrate = rs.getString("ntaxrate");
							if(!map.containsKey(pk_projectbase)){
								VatTaxorgsetVO setVO = new VatTaxorgsetVO();
								setVO.setPk_org(pk_org);
								setVO.setBistaxorg(bistaxorg.equals('N')? UFBoolean.FALSE : UFBoolean.TRUE);
								setVO.setPk_taxorg(pk_taxorg);
								setVO.setPk_parent(pk_parent);
								setVO.setItaxtype(itaxtype);
								setVO.setNtaxrate(new UFDouble(ntaxrate));
								map.put(pk_projectbase, setVO);
							}							
						}
						return map.size() > 0 ? map : null;
					}

				});
		return map;
	}
	
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, String> getProjMap(List<String> pk_projectBaseList) throws BusinessException{
		if(null == pk_projectBaseList || pk_projectBaseList.size() == 0){
			return null;
		}
		Map<String, String> projMap = new HashMap<String, String>();
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT pk_jobbasfil, jobname FROM bd_jobbasfil ");
		sql.append(" where isnull(dr, 0) = 0 ");
		sql.append(" and pk_jobbasfil in " + InSqlManager.getInSQLValue(pk_projectBaseList));
		projMap = (Map<String, String>)getDao().executeQuery(sql.toString(),  new ResultSetProcessor(){
			public Object handleResultSet(ResultSet rs) throws SQLException {
				Map<String, String> projMap = new HashMap<String, String>();
				while(rs.next()){
					String pk_jobbasfil = rs.getString("pk_jobbasfil");
					String jobname = rs.getString("jobname");
					if(!projMap.containsKey(pk_jobbasfil)){
						projMap.put(pk_jobbasfil, jobname);
					}
				}
				return projMap.size() > 0 ? projMap : null;
			}
		});
		return projMap;
	}

	public String getProjName(SuperVO vos) throws BusinessException {
		String pk_projectbase = (String) vos.getAttributeValue(VatPubMetaNameConsts.PK_PROJECTBASE);
		List<String> pk_projectBaseList = new ArrayList<String>();
		pk_projectBaseList.add(pk_projectbase);
		Map<String, String> projMap = getProjMap(pk_projectBaseList);
		String vprojname = null;
		if(projMap.containsKey(pk_projectbase)){
			vprojname = projMap.get(pk_projectbase);
		}
		return vprojname;
	}
	
	@SuppressWarnings("serial")
	public String getFreeTs(SuperVO vos) throws BusinessException {
		String pk_project = (String) vos.getAttributeValue(VatPubMetaNameConsts.PK_PROJECT);
		String vperiod = (String) vos.getAttributeValue(VatPubMetaNameConsts.VPERIOD);
		UFBoolean bisbegin = (UFBoolean) vos.getAttributeValue(VatPubMetaNameConsts.BISBEGIN);
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT ts  FROM " + vos.getTableName());
		sql.append(" where isnull(dr, 0) = 0 ");
		
		sql.append(" and vbillstatus = 8 ");
		sql.append(" and pk_project = '" + pk_project + "' ");
		if(!bisbegin.booleanValue()){//期间
			sql.append(" and bisbegin = 'N' ");
			sql.append(" and vperiod = '" + vperiod + "' ");
		}
		else{
			sql.append(" and bisbegin = 'Y' ");
		}
		String ts = (String)getDao().executeQuery(sql.toString(),  new ResultSetProcessor(){
			public Object handleResultSet(ResultSet rs) throws SQLException {
				while(rs.next()){
					return rs.getString("ts");
				}
				return null;
			}
		});
		return ts;
	}
	@SuppressWarnings("serial")
	public String getFreePk(SuperVO vos) throws BusinessException {
		String pk_project = (String) vos.getAttributeValue(VatPubMetaNameConsts.PK_PROJECT);
		String vperiod = (String) vos.getAttributeValue(VatPubMetaNameConsts.VPERIOD);
		UFBoolean bisbegin = (UFBoolean) vos.getAttributeValue(VatPubMetaNameConsts.BISBEGIN);
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT " + vos.getPKFieldName() + " FROM " + vos.getTableName());
		sql.append(" where isnull(dr, 0) = 0 ");
		if(bisbegin.booleanValue()){
			sql.append(" and bisbegin = 'Y' ");
		}
		else{
			sql.append(" and bisbegin = 'N' ");
		}
		sql.append(" and vbillstatus = 8 ");
		sql.append(" and pk_project = '" + pk_project + "' ");
		if(!bisbegin.booleanValue()){
			sql.append(" and vperiod = '" + vperiod + "' ");
		}
		final String pk_fieldName = vos.getPKFieldName();
		String pk_projanaly = (String)getDao().executeQuery(sql.toString(),  new ResultSetProcessor(){
			public Object handleResultSet(ResultSet rs) throws SQLException {
				while(rs.next()){
					return rs.getString(pk_fieldName);
				}
				return null;
			}
		});
		return pk_projanaly;
	}
	/**
	 * 根据公司找对应的纳税组织定义
	 * @param pk_corp
	 * @return
	 * @throws BusinessException
	 */
	@SuppressWarnings("serial")
	private VatTaxorgsetVO getTaxorgsetVOByCorp(String pk_corp) throws BusinessException {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT orgset.pk_org pk_org, ");//公司
		sql.append(" orgset.bistaxorg bistaxorg, ");//是否税务组织
		sql.append(" orgset.pk_taxorg pk_taxorg, ");//对应纳税组织
		sql.append(" orgset.itaxtype itaxtype, ");//纳税人类别
		sql.append(" orgset.ntaxrate ntaxrate ");//征收率
		sql.append(" FROM jzinv_vat_taxorgset orgset ");
		sql.append(" where isnull(orgset.dr,0) = 0 ");
		sql.append(" and orgset.bissealed = 'N' ");
		sql.append(" and orgset.pk_org = '" + pk_corp + "' ");
		VatTaxorgsetVO orgsetVO = (VatTaxorgsetVO) getDao().executeQuery(
				sql.toString(), new ResultSetProcessor() {
					public Object handleResultSet(ResultSet rs)
							throws SQLException {
						if (rs == null) {
							return null;
						}
						VatTaxorgsetVO setVO = null;
						while (rs.next()) {
							setVO = new VatTaxorgsetVO();
							String pk_org = rs.getString("pk_org");
							String bistaxorg = rs.getString("bistaxorg");
							String pk_taxorg = rs.getString("pk_taxorg");
							Integer itaxtype = rs.getInt("itaxtype");
							String ntaxrate = rs.getString("ntaxrate");
							setVO.setPk_org(pk_org);
							setVO.setBistaxorg(bistaxorg.equals('N')? UFBoolean.FALSE : UFBoolean.TRUE);
							setVO.setPk_taxorg(pk_taxorg);
							setVO.setItaxtype(itaxtype);
							setVO.setNtaxrate(new UFDouble(ntaxrate));					
						}
						return setVO;
					}

				});
		return orgsetVO;
	}
	
	@SuppressWarnings({ "unchecked", "serial" })
	public Map<String, Map<String, String>> getProjMap(String pk_corp, String pk_project) throws BusinessException{
		//纳税人识别号、税款起始时间、税款结束时间、项目编码(施工许可证号)、项目地址(项目经营地)
				StringBuffer sql = new StringBuffer();
				sql.append(" SELECT projset.pk_project pk_project, ");
				sql.append(" orgset.vtaxpayernumber vtaxpayernumber, ");
				sql.append(" projset.vbuildpermitno vbuildpermitno, ");
				sql.append(" projset.vprojadd vprojadd ");
				sql.append(" FROM jzinv_vat_taxorgset orgset ");
		        sql.append(" left join jzinv_vat_projtaxset projset ");
		        sql.append(" on orgset.pk_org = projset.pk_corp ");
		        sql.append(" where isnull(orgset.dr, 0) = 0 ");
		        sql.append(" and isnull(projset.dr, 0) = 0 ");
		        sql.append(" and projset.pk_corp = '" + pk_corp + "' ");
		        sql.append(" and projset.pk_project = '" + pk_project + "'");
		        BaseDAO dao = new BaseDAO();
		        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) dao.executeQuery(
						sql.toString(), new ResultSetProcessor() {
							public Object handleResultSet(ResultSet rs)
									throws SQLException {
								if (rs == null) {
									return null;
								}
								Map<String, Map<String, String>> outMap = new HashMap<String, Map<String, String>>();
								Map<String, String> inMap = null;
								while (rs.next()) {
									String pk_project = rs.getString("pk_project");
									String vtaxpayernumber = rs.getString("vtaxpayernumber");
									String vbuildpermitno = rs.getString("vbuildpermitno");
									String vprojadd = rs.getString("vprojadd");
									if(outMap.containsKey(pk_project)){
										inMap = outMap.get(pk_project);
									}
									else{
										inMap = new HashMap<String, String>();
									}
									inMap.put(VatTaxorgsetVO.VTAXPAYERNUMBER, vtaxpayernumber);
									inMap.put(VatProjtaxsetVO.VBUILDPERMITNO, vbuildpermitno);
									inMap.put(VatProjtaxsetVO.VPROJADD, vprojadd);
									outMap.put(pk_project, inMap);
								}
								return outMap.size() > 0 ? outMap : null;
							}

						});
		        return map;
	}
	
	/**
	 * 根据项目+公司+期间取得【预缴税额】
	 * @param pk_project
	 * @param vperiod
	 * @param pk_corp
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getNprepaytaxmny(String pk_project, String vperiod,
			String pk_corp,UFBoolean bisbegin) throws BusinessException {
		try {
			BaseDAO dao = new BaseDAO();
			UFDouble nprepaytaxmny = UFDouble.ZERO_DBL;
			String sqlWhere = " isnull(dr,0) = 0 and vbillstatus NOT IN(4,8) and pk_project = '"
					+ pk_project + "' and vperiod = '" + vperiod + "' and pk_corp = '"+ pk_corp + "' and bisbegin = '" + bisbegin + "'";
			Collection<VatIncretaxpreVO> retrieveByClause = dao.retrieveByClause(
					VatIncretaxpreVO.class, sqlWhere);
			for (VatIncretaxpreVO cmcontractvo : retrieveByClause) {
				nprepaytaxmny = SafeCompute.add(nprepaytaxmny,
						cmcontractvo.getNprelevytaxsummny());
			}
			return nprepaytaxmny;
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}
	/* (non-Javadoc)
	 * @see nc.itf.jzinv.vat1050.IVatProjanalyService#queryInIncomeHeadVOsByCond(java.lang.String, java.lang.String, java.lang.String)
	 */
	public List<InIncomeVO> queryInIncomeHeadVOsByCond(String pk_project, String pk_corp) throws BusinessException {
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append(" select * from jzpm_in_income r ");
		sqlStr.append(" where pk_project = '" + pk_project + "'");
		sqlStr.append(" and pk_corp = '" + pk_corp + "'");
		sqlStr.append(" and dr = 0 ");
		List<InIncomeVO> result = (List<InIncomeVO>) new BaseDAO().executeQuery(sqlStr.toString(),
				new BeanListProcessor(InIncomeVO.class));
		InIncomeVO[] vos = result.toArray(new InIncomeVO[0]);
		List<InIncomeVO> voList = new ArrayList<InIncomeVO>();
		voList.addAll(Arrays.asList(vos));
		return voList;
	}
	
	/* (non-Javadoc)
	 * @see nc.itf.jzinv.vat1050.IVatProjanalyService#queryOpenHVOsByCond(java.lang.String, java.lang.String)
	 */
	public List<OpenHVO> queryOpenHVOsByCond(String pk_project, String pk_corp) throws BusinessException {
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append(" select * from jzinv_open r ");
		sqlStr.append(" where pk_project = '" + pk_project + "'");
		sqlStr.append(" and pk_corp = '" + pk_corp + "'");
		sqlStr.append(" and dr = 0 ");
		List<OpenHVO> result = (List<OpenHVO>) new BaseDAO().executeQuery(sqlStr.toString(),
				new BeanListProcessor(OpenHVO.class));
		OpenHVO[] vos = result.toArray(new OpenHVO[0]);
		List<OpenHVO> voList = new ArrayList<OpenHVO>();
		voList.addAll(Arrays.asList(vos));
		return voList;
	}
}
