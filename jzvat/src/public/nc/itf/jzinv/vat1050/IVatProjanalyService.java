package nc.itf.jzinv.vat1050;

import java.util.List;
import java.util.Map;

import nc.vo.jzinv.inv0510.OpenHVO;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
import nc.vo.jzinv.vat0510.VatProjtaxsetVO;
import nc.vo.jzinv.vat1050.VatProjanalyVO;
import nc.vo.jzpm.in2005.InIncomeVO;
import nc.vo.jzpm.in2010.InRecdetailVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;

/**
 * 项目税金计算服务接口
 * @author mayyc
 *
 */
public interface IVatProjanalyService {
	/**
	 * 获取项目对应的开票mny信息(销售额、应税销售额、销项税额)
	 * <项目basePK, <金额字段, 数值>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getOpenInvMnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException ;
	
	/**
	 * 获取项目对应的收票mny信息(应税进项额、进项税额)
	 * <项目basePK, <金额字段, 数值>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getReceInvMnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException ;
	
	/**
	 * 获取本期已认证税额
	 * <项目basePK, <本期已认证额字段, 本期已认证额>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getIncataxmnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * 获取可认证进项税额
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getShincataxmnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * 获取上期留抵税额
	 * <项目basePK,<上期留抵税额字段/上期费用留抵扣除金额字段, 上期留抵税额/上期费用留抵扣除金额>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getLastresttaxmnyInfo(String className, String pk_corp, 
			List<String> pk_projectbaseList, String vperiod) throws Exception;
	/**
	 * 获取期初的留抵税额
	 * <项目basePK, <上期留抵税额字段/上期费用留抵扣除金额字段, 期初的留抵税额/本期费用留抵扣除金额>>
	 * @param projVatVO
	 * @param pk_projectbaseList
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getBeginRestMnyByProj(String className, String pk_corp, List<String> pk_projectbaseList) throws Exception;
	/**
	 * 获取进项转出额
	 * <项目basePK, <进项转出字段, 进项转出额>>
	 * @param pk_corp
	 * @param bishaslower
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getIntoOutMnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * 获取本期认证不可抵扣额
	 * <项目basePK, <本期认证不可抵扣字段, 本期认证不可抵扣额>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getIntoOutMnyNoCutInfo(String pk_corp,
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException;
	/**
	 * 获取前期认证本期抵扣额
	 * <项目basePK, <前期认证本期抵扣字段, 前期认证本期抵扣额>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getIntoOutMnyThCutInfo(String pk_corp,
			List<String> pk_projectbaseList, String vperiod)
			throws BusinessException;
	/**
	 * 获取项目销售额
	 * <项目basePK, <项目销售额字段, 项目销售额>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getProjsaleInfoMny(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * 根据项目获取各个金额字段的数值
	 * <项目basePK, <字段, 数值>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getMnyMapByProj(String className, String pk_corp, 
			List<String> pk_projectbaseList, String vperiod) throws Exception;
	/**
	 * 获得分包收票数据：差额征税扣除项目清单
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException 
	 */
	public Map<String, List<SuperVO>> getSubReceVOList(String pk_corp,
			List<String> pk_projectbaseList, String vperiod) throws BusinessException; 
	/**
	 * 获取项目计税方式
	 * <项目basePK, 项目计税方式VO>
	 * @param pk_projectBaseList
	 * @return
	 */
	public Map<String, VatProjtaxsetVO> getProjtaxsetMap(String pk_projectbase, Integer itaxtype, UFBoolean bisprepay) throws BusinessException;

	/**
	 * 获取项目的纳税信息
	 * <项目basePK, 纳税组织定义VO>
	 * @param pk_corp
	 * @param pk_projectBaseList
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, VatTaxorgsetVO> getVatTaxorgsetMap(String pk_corp, List<String> pk_projectBaseList) throws BusinessException;
	/**
	 * 根据项目PK获取项目Map<项目PK, 项目名称>
	 * @param pk_projectBaseList
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, String> getProjMap(List<String> pk_projectBaseList) throws BusinessException;

	/**
	 * 获得项目名称
	 * @param vos
	 * @return
	 * @throws BusinessException
	 */
	public String getProjName(SuperVO vos) throws BusinessException;
	/**
	 * 获得自由态期初/期间的TS
	 * @param vos
	 * @return
	 * @throws BusinessException
	 */
	public String getFreeTs(SuperVO vos) throws BusinessException;
	/**
	 * 获得自由态期初/期间的Pk
	 * @param vos
	 * @return
	 * @throws BusinessException
	 */
	public String getFreePk(SuperVO vos) throws BusinessException;
	/**
	 * 根据公司+项目获取项目的计税方式中信息
	 * @param pk_corp
	 * @param pk_project
	 * @return
	 */
	public Map<String, Map<String, String>> getProjMap(String pk_corp, String pk_project) throws BusinessException;
	
	/**
	 * 根据项目+公司+期间取得【预缴税额】
	 * @param pk_project
	 * @param vperiod
	 * @param pk_corp
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getNprepaytaxmny( String pk_project,String vperiod,String pk_corp,UFBoolean bisbegin) throws BusinessException;
	
	/** 
	* @Title: queryInIncomeHeadVOsByCond 
	* @Description: 根据条件查询收款申请单信息 
	* @param @param pk_project
	* @param @param vperiod
	* @param @param pk_corp
	* @param @return
	* @param @throws BusinessException    
	* @return List<InIncomeVO>    
	* @throws 
	*/
	public List<InIncomeVO> queryInIncomeHeadVOsByCond(String pk_project, String pk_corp) throws BusinessException;
	
	/** 
	* @Title: queryOpenHVOsByCond 
	* @Description: 查询开票信息 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @return
	* @param @throws BusinessException    
	* @return List<OpenHVO>    
	* @throws 
	*/
	public List<OpenHVO> queryOpenHVOsByCond(String pk_project, String pk_corp) throws BusinessException;
	
	/** 
	* @Title: queryInRecdetailVOByCond 
	* @Description: 根据查询条件查询收款明细
	* @param @param pk_project 
	* @param @param pk_corp
	* @param @param vbillstatus 如果不填默认选全部
	* @param @param vperiod 如果不填默认选全部
	* @param @return
	* @param @throws BusinessException    
	* @return List<InRecdetailVO>    
	* @throws 
	*/
	public List<InRecdetailVO> queryInRecdetailVOByCond(String pk_project, String pk_corp,
			Integer vbillstatus, String vperiod) throws BusinessException; 
	
	/** 
	* @Title: queryVatProjanalyVOsByCond 
	* @Description: 查询项目税金计算单据 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @return
	* @param @throws BusinessException    
	* @return List<VatProjanalyVO>    
	* @throws 
	*/
	public List<VatProjanalyVO> queryVatProjanalyVOsByCond(String pk_project, String pk_corp) throws BusinessException;
}