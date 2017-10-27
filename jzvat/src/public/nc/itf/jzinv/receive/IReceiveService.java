package nc.itf.jzinv.receive;

import java.util.List;

import nc.vo.jzinv.inv0510.OpenHVO;
import nc.vo.jzinv.invpub.AuthenConditionVO;
import nc.vo.jzinv.receive.AggReceiveVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.jzinv.vatinvoice.AggVatGtInvoiceVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;

public interface IReceiveService {

	public AggReceiveVO queryAggVoByPK(String pk_receive) throws Exception;
	
	public void updateSelectVOs(ReceiveVO[] hvos) throws Exception;
	
	public List<ReceiveVO> queryHeadVOsByCond(AuthenConditionVO conditionVO)throws Exception;

	public void goldTaxAuthen(List<AggReceiveVO> aggVos, String pk_corp, String pk_user, String code, String password) throws Exception;

	public void fetchGoldTaxAuthResult(String pk_corp, String pk_operator) throws BusinessException;

	public void shhzxAuthen(List<AggReceiveVO> aggVos, String pk_corp, String pk_user, String code, String password) throws Exception;

	public void fetchShhzxAuthResult(String pk_corp, String pk_operator) throws BusinessException;
	
	public void jzshxgtAuthen(List<ReceiveVO> receiveVos) throws BusinessException;
	
	public void writeBackInvoiceAndOpenInfo(AggVatGtInvoiceVO aggInvoiceVo, OpenHVO headVo) throws BusinessException;

	public void writeBackCancelInvoice(OpenHVO openHeadVo) throws BusinessException;
	
	public void writeBackUploadInfo(List<OpenHVO> headVoList) throws BusinessException;
	
	public void writeBackMQResult2Open(OpenHVO openHeadVo) throws BusinessException;
	
	public String getCubasDoc(String taxpayerid) throws BusinessException;
	
	public String getCumandoc(String pk_corp,String cubasdoc) throws BusinessException;
	
	/**
	 * �����⾭֤��������ȡ��˰������
	 * 
	 * @throws BusinessException
	 */
	SuperVO[] queryManageBbyPKManage(Class class1,String pk_obl_manage)throws BusinessException;

}
