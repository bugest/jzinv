package nc.bs.jzinv.receive.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.jdbc.framework.processor.BeanListProcessor;
import nc.jdbc.framework.processor.ColumnListProcessor;
import nc.vo.jzinv.invpub.InvCheckVO;
import nc.vo.jzinv.pub.JzinvPubMetaNameConsts;
import nc.vo.jzinv.pub.tool.InSqlManager;
import nc.vo.jzinv.receive.AggReceiveVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDateTime;

import org.apache.commons.lang.StringUtils;
/**
 * ��Ʊ����-��Ʊ����ǰRule
 * @author mayyc
 *
 */
public class ReceiveSaveBeforeRule {
	private BaseDAO dao = new BaseDAO();
	/**
	 * ����ǰУ��������
	 * @param vos
	 * @throws BusinessException
	 */
	public void process(AggregatedValueObject vos) throws BusinessException {		
		checkInvNoUnique(vos);
		checkBlueBeforeSave(vos);
		checkRedRece(vos);
		checkBillBodyBisRefer(vos);
	}
	
	/**
	 * ��鵥�ݱ����Ƿ�δ����̬��������������
	 * @param vos
	 * @throws BusinessException
	 */
	private void checkBillBodyBisRefer(AggregatedValueObject vos) throws BusinessException {
		if(null == vos){
			return;
		}
		AggReceiveVO aggVO = (AggReceiveVO) vos;
		ReceiveVO receHVO = (ReceiveVO) aggVO.getParentVO();
		SuperVO[] appBVOs = (SuperVO[])aggVO.getChildVOsByParentId(ReceiveDetailVO.TABCODE, receHVO.getPrimaryKey());
		if(null == appBVOs || appBVOs.length <= 0){
			return;
		}
		Map<String, Integer> keyRowMap = new HashMap<String, Integer>();
		Map<String, String> keyBIllTypeMap = new HashMap<String, String>();
		for(int i = 1; i <= appBVOs.length; i++){
			if(appBVOs[i-1].getStatus() == VOStatus.DELETED){
				continue;
			}
			keyRowMap.put((String)appBVOs[i-1].getAttributeValue(JzinvPubMetaNameConsts.VLASTBILLID), i);
			keyBIllTypeMap.put((String)appBVOs[i-1].getAttributeValue(JzinvPubMetaNameConsts.VLASTBILLID), 
					(String)appBVOs[i-1].getAttributeValue(JzinvPubMetaNameConsts.VLASTBILLTYPE));
		}
		String warnString = checkContract(receHVO, keyRowMap, keyBIllTypeMap);
		if(!StringUtils.isEmpty(warnString)){
			throw new BusinessException(warnString);
		}
	}
	@SuppressWarnings("unchecked")
	private String checkContract(SuperVO hvo, Map<String, Integer> keyRowMap,Map<String, String> keyBIllTypeMap) 
			throws BusinessException {
		StringBuffer sqlBuffer=new StringBuffer();		
		sqlBuffer.append(" SELECT jzinv_receive.pk_receive pk_primary, jzinv_receive.vbillno vbillno,")
				 .append(" jzinv_receive_b.vlastbillid vlastbillid,jzinv_receive_b.vlastbilltype vlastbilltype ")		
				 .append(" FROM jzinv_receive_b inner join jzinv_receive on jzinv_receive_b.pk_receive = jzinv_receive.pk_receive ")
				 .append(" where jzinv_receive.vbillstatus !=1 and jzinv_receive.dr=0 and jzinv_receive_b.dr=0 ");
		if(hvo.getPrimaryKey()!=null){
			sqlBuffer.append(" and jzinv_receive.pk_receive !='"+hvo.getPrimaryKey()+"'");
		}
		sqlBuffer.append(" and jzinv_receive_b.vlastbillid in "+InSqlManager.getInSQLValue(keyRowMap.keySet()));
		BaseDAO baseDao=new BaseDAO();
		StringBuffer warnBuffer=new StringBuffer();
		try {
			List<InvCheckVO> checkVOs = (List<InvCheckVO>)baseDao.
					executeQuery(sqlBuffer.toString(), new BeanListProcessor(InvCheckVO.class));
			for(InvCheckVO checkVO : checkVOs){
				String vlastbillid = (String) checkVO.getVlastbillid();
				String vlastbilltype = (String) checkVO.getVlastbilltype();
				if(keyBIllTypeMap.containsKey(vlastbillid) && keyBIllTypeMap.get(vlastbillid).equals(vlastbilltype)){
					int row = keyRowMap.get(vlastbillid);
					warnBuffer.append("�����" + row + "��,��δ����̬����Ʊ����,���ݱ���Ϊ" + checkVO.getAttributeValue("vbillno")+" \n");
				}
			}
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
		return warnBuffer.toString();
	}

	/**
	 * ��鷢Ʊ��Ψһ��
	 * @param headVO
	 * @throws BusinessException
	 */
	@SuppressWarnings("unchecked")
	private void checkInvNoUnique(AggregatedValueObject billVo) throws BusinessException{
		ReceiveVO headVO = (ReceiveVO) billVo.getParentVO();
		String pk_receive = headVO.getPk_receive();
		//��Ʊ����
		String vinvno = headVO.getVinvno();
		//��Ʊ����
		String vinvcode = headVO.getVinvcode();
		StringBuffer sql1 = new StringBuffer();
		sql1.append("select pk_receive from jzinv_receive where dr=0 and vinvno='").append(vinvno).append("'"); 
		sql1.append(" and vinvcode='").append(vinvcode).append("'"); 
		if(null != pk_receive){
			sql1.append(" and pk_receive != '").append(pk_receive).append("'");
		}
		List<String> oldPks = (List<String>) dao.executeQuery(sql1.toString(), new ColumnListProcessor());
		if(null != oldPks && !oldPks.isEmpty()){
			throw new BusinessException("��Ʊ����ͷ�Ʊ�����ظ������������룡");
		}
	}

	/**
	 * ����Ӧ���ַ�Ʊ�Ƿ��޸�
	 * @param billVo
	 * @throws BusinessException
	 */
	@SuppressWarnings("unchecked")
	private void checkBlueBeforeSave(AggregatedValueObject billVo) throws BusinessException{
		ReceiveVO headVO = (ReceiveVO) billVo.getParentVO();
		String pk_receive_ref = headVO.getPk_receive_ref();
		String pk_receive = headVO.getPk_receive();
		if(null == pk_receive_ref || null != pk_receive)
			return;
		
		StringBuffer sql1 = new StringBuffer();
		sql1.append("select ts from jzinv_receive where dr=0 and pk_receive='").append(pk_receive_ref).append("'"); 
		List<String> oldPks = (List<String>) dao.executeQuery(sql1.toString(), new ColumnListProcessor());
		if(null == oldPks || oldPks.isEmpty()){
			throw new BusinessException("��Ӧ���ַ�Ʊ�ѱ�ɾ��, ��ˢ�½�������!");
		}
		else{
			UFDateTime oldTs  = new UFDateTime( oldPks.get(0));
			if(0 != oldTs.compareTo(headVO.getRedts())){
				throw new BusinessException("��Ӧ���ַ�Ʊ�ѱ��޸�, ��ˢ�½�������!");
			}
		}
	}
	/**
	 * ����ǰУ���Ӧ���ַ�Ʊ�Ƿ�δ����̬����Ʊ����
	 * @param vos
	 * @throws BusinessException
	 */
	@SuppressWarnings("unchecked")
	private void checkRedRece(AggregatedValueObject vos) throws BusinessException{
		SuperVO receHVO = (SuperVO) vos.getParentVO();
		String pk_blue = (String) receHVO.getAttributeValue(ReceiveVO.PK_RECEIVE_REF);
		if(StringUtils.isEmpty(pk_blue)){
			return;
		}
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT pk_receive pk_receive, vbillno vbillno ");
		sql.append(" FROM jzinv_receive "); 
		sql.append(" where jzinv_receive.vbillstatus !=1 and jzinv_receive.dr = 0 ");
		sql.append(" and pk_receive_ref = '" + pk_blue + "' ");
		String pk_receive = receHVO.getPrimaryKey();
		if(!StringUtils.isEmpty(pk_receive)){
			sql.append(" and jzinv_receive.pk_receive != '" + pk_receive + "' ");
		}
		BaseDAO baseDao = new BaseDAO();
		StringBuffer message = new StringBuffer();
		try {
			List<InvCheckVO> checkVOs = (List<InvCheckVO>)baseDao.
					executeQuery(sql.toString(), new BeanListProcessor(InvCheckVO.class));
			for(InvCheckVO checkVO : checkVOs){
				String vbillno = checkVO.getVbillno();
				if(!StringUtils.isEmpty(vbillno)){
				    message.append("��Ӧ���ַ�Ʊ��δ����̬����Ʊ����, ���ݱ���Ϊ" + vbillno + "\n");
				}
			}
			if(!StringUtils.isEmpty(message.toString())){
				throw new BusinessException(message.toString());
			}
		} catch (DAOException e) {
			throw new BusinessException(e);
		}	
	}
}