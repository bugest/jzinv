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
 * ��Ŀ˰��������ӿ�
 * @author mayyc
 *
 */
public interface IVatProjanalyService {
	/**
	 * ��ȡ��Ŀ��Ӧ�Ŀ�Ʊmny��Ϣ(���۶Ӧ˰���۶����˰��)
	 * <��ĿbasePK, <����ֶ�, ��ֵ>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getOpenInvMnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException ;
	
	/**
	 * ��ȡ��Ŀ��Ӧ����Ʊmny��Ϣ(Ӧ˰��������˰��)
	 * <��ĿbasePK, <����ֶ�, ��ֵ>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getReceInvMnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException ;
	
	/**
	 * ��ȡ��������֤˰��
	 * <��ĿbasePK, <��������֤���ֶ�, ��������֤��>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getIncataxmnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * ��ȡ����֤����˰��
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getShincataxmnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * ��ȡ��������˰��
	 * <��ĿbasePK,<��������˰���ֶ�/���ڷ������ֿ۳�����ֶ�, ��������˰��/���ڷ������ֿ۳����>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getLastresttaxmnyInfo(String className, String pk_corp, 
			List<String> pk_projectbaseList, String vperiod) throws Exception;
	/**
	 * ��ȡ�ڳ�������˰��
	 * <��ĿbasePK, <��������˰���ֶ�/���ڷ������ֿ۳�����ֶ�, �ڳ�������˰��/���ڷ������ֿ۳����>>
	 * @param projVatVO
	 * @param pk_projectbaseList
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getBeginRestMnyByProj(String className, String pk_corp, List<String> pk_projectbaseList) throws Exception;
	/**
	 * ��ȡ����ת����
	 * <��ĿbasePK, <����ת���ֶ�, ����ת����>>
	 * @param pk_corp
	 * @param bishaslower
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getIntoOutMnyInfo(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * ��ȡ������֤���ɵֿ۶�
	 * <��ĿbasePK, <������֤���ɵֿ��ֶ�, ������֤���ɵֿ۶�>>
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
	 * ��ȡǰ����֤���ڵֿ۶�
	 * <��ĿbasePK, <ǰ����֤���ڵֿ��ֶ�, ǰ����֤���ڵֿ۶�>>
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
	 * ��ȡ��Ŀ���۶�
	 * <��ĿbasePK, <��Ŀ���۶��ֶ�, ��Ŀ���۶�>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getProjsaleInfoMny(String pk_corp, List<String> pk_projectbaseList, String vperiod) throws BusinessException;
	/**
	 * ������Ŀ��ȡ��������ֶε���ֵ
	 * <��ĿbasePK, <�ֶ�, ��ֵ>>
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Map<String, UFDouble>> getMnyMapByProj(String className, String pk_corp, 
			List<String> pk_projectbaseList, String vperiod) throws Exception;
	/**
	 * ��÷ְ���Ʊ���ݣ������˰�۳���Ŀ�嵥
	 * @param pk_corp
	 * @param pk_projectbaseList
	 * @param vperiod
	 * @return
	 * @throws BusinessException 
	 */
	public Map<String, List<SuperVO>> getSubReceVOList(String pk_corp,
			List<String> pk_projectbaseList, String vperiod) throws BusinessException; 
	/**
	 * ��ȡ��Ŀ��˰��ʽ
	 * <��ĿbasePK, ��Ŀ��˰��ʽVO>
	 * @param pk_projectBaseList
	 * @return
	 */
	public Map<String, VatProjtaxsetVO> getProjtaxsetMap(String pk_projectbase, Integer itaxtype, UFBoolean bisprepay) throws BusinessException;

	/**
	 * ��ȡ��Ŀ����˰��Ϣ
	 * <��ĿbasePK, ��˰��֯����VO>
	 * @param pk_corp
	 * @param pk_projectBaseList
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, VatTaxorgsetVO> getVatTaxorgsetMap(String pk_corp, List<String> pk_projectBaseList) throws BusinessException;
	/**
	 * ������ĿPK��ȡ��ĿMap<��ĿPK, ��Ŀ����>
	 * @param pk_projectBaseList
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, String> getProjMap(List<String> pk_projectBaseList) throws BusinessException;

	/**
	 * �����Ŀ����
	 * @param vos
	 * @return
	 * @throws BusinessException
	 */
	public String getProjName(SuperVO vos) throws BusinessException;
	/**
	 * �������̬�ڳ�/�ڼ��TS
	 * @param vos
	 * @return
	 * @throws BusinessException
	 */
	public String getFreeTs(SuperVO vos) throws BusinessException;
	/**
	 * �������̬�ڳ�/�ڼ��Pk
	 * @param vos
	 * @return
	 * @throws BusinessException
	 */
	public String getFreePk(SuperVO vos) throws BusinessException;
	/**
	 * ���ݹ�˾+��Ŀ��ȡ��Ŀ�ļ�˰��ʽ����Ϣ
	 * @param pk_corp
	 * @param pk_project
	 * @return
	 */
	public Map<String, Map<String, String>> getProjMap(String pk_corp, String pk_project) throws BusinessException;
	
	/**
	 * ������Ŀ+��˾+�ڼ�ȡ�á�Ԥ��˰�
	 * @param pk_project
	 * @param vperiod
	 * @param pk_corp
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getNprepaytaxmny( String pk_project,String vperiod,String pk_corp,UFBoolean bisbegin) throws BusinessException;
	
	/** 
	* @Title: queryInIncomeHeadVOsByCond 
	* @Description: ����������ѯ�տ����뵥��Ϣ 
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
	* @Description: ��ѯ��Ʊ��Ϣ 
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
	* @Description: ���ݲ�ѯ������ѯ�տ���ϸ
	* @param @param pk_project 
	* @param @param pk_corp
	* @param @param vbillstatus �������Ĭ��ѡȫ��
	* @param @param vperiod �������Ĭ��ѡȫ��
	* @param @return
	* @param @throws BusinessException    
	* @return List<InRecdetailVO>    
	* @throws 
	*/
	public List<InRecdetailVO> queryInRecdetailVOByCond(String pk_project, String pk_corp,
			Integer vbillstatus, String vperiod) throws BusinessException; 
	
	/** 
	* @Title: queryVatProjanalyVOsByCond 
	* @Description: ��ѯ��Ŀ˰����㵥�� 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @return
	* @param @throws BusinessException    
	* @return List<VatProjanalyVO>    
	* @throws 
	*/
	public List<VatProjanalyVO> queryVatProjanalyVOsByCond(String pk_project, String pk_corp) throws BusinessException;
}