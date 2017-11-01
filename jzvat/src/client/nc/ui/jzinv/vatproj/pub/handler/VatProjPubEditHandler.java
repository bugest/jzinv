package nc.ui.jzinv.vatproj.pub.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nc.ui.jzinv.vat1050.tool.VatProjMnyTool;
import nc.ui.jzinv.vatpub.handler.VatCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
import nc.vo.jzinv.vat0510.VatProjtaxsetVO;
import nc.vo.jzinv.vat1045.VatIncretaxLevyVO;
import nc.vo.jzinv.vat1050.VatProjanalyBVO;
import nc.vo.jzinv.vat1050.VatProjanalyVO;
import nc.vo.jzinv.vatpub.VatPubMetaNameConsts;
import nc.vo.jzinv.vatpub.VatPubProxy;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;

import org.apache.commons.lang.StringUtils;

public class VatProjPubEditHandler extends VatCardEditHandler{
	/**
	 * ��Ŀ��˰��ʽVO
	 */
	private VatProjtaxsetVO projsetVO;
	/**
	 * ��˰��֯����VO
	 */
	private VatTaxorgsetVO orgsetVO;
	public VatProjPubEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}

	public void cardHeadAfterEdit(BillEditEvent e) {
        super.cardHeadAfterEdit(e);
        String pk_corp = getClientUI()._getCorp().pk_corp;
        String pk_projectbase = (String) getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECTBASE).getValueObject();
        projsetVO = getProjsetVOByProj(pk_corp, pk_projectbase);
        orgsetVO = getVatTaxorgsetVO(pk_corp, pk_projectbase);
	}
	public void setHeadItem(VatProjtaxsetVO projsetVO, VatTaxorgsetVO orgsetVO){
		getCardPanel().setHeadItem(VatProjanalyVO.ITAXTYPE, projsetVO == null ? null : projsetVO.getItaxtype());
    	getCardPanel().setHeadItem(VatProjanalyVO.NAVGTAX, projsetVO == null ? null : projsetVO.getNtaxrate());
    	getCardPanel().setHeadItem(VatProjanalyVO.PK_TAXORG, orgsetVO == null ? null : orgsetVO.getPk_taxorg());
    	getCardPanel().setHeadItem(VatProjanalyVO.PK_UPPERTAXORG, orgsetVO == null ? null : orgsetVO.getPk_parent());
	}
	/**
	 * �����Ŀ+�ڳ�/�ڼ����ݵĺϷ���
	 * @param pk_project
	 * @param vperiod
	 * @throws BusinessException
	 */
	public void checkData(String pk_project, String vperiod, BillEditEvent e) throws Exception{
		SuperVO vos = (SuperVO) Class.forName(getHeadClassName()).newInstance();
		UFBoolean bisbegin = getCardPanel().getHeadItem(VatPubMetaNameConsts.BISBEGIN) == null ? UFBoolean.FALSE : 
			new UFBoolean((String)getCardPanel().getHeadItem(VatProjanalyVO.BISBEGIN).getValueObject());
		String pk_projectbase = (String) getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECTBASE).getValueObject();	
		String pk_billtype = (String) getCardPanel().getHeadItem(VatProjanalyVO.PK_BILLTYPE).getValueObject();	
		vos.setAttributeValue(VatPubMetaNameConsts.PK_PROJECT, pk_project);
		vos.setAttributeValue(VatPubMetaNameConsts.VPERIOD, vperiod);		
		vos.setAttributeValue(VatPubMetaNameConsts.BISBEGIN, bisbegin);		
		vos.setAttributeValue(VatPubMetaNameConsts.PK_BILLTYPE, pk_billtype);
		vos.setAttributeValue(VatProjanalyVO.PK_PROJECTBASE, pk_projectbase);
		
		String vprojname = VatPubProxy.getProjService().getProjName(vos);	
		try{
			if(bisbegin.booleanValue()){
				this.checkBeginValid(vos, vprojname);
			}
			else if(!StringUtils.isEmpty(vperiod)){
				this.checkTermValid(vos, vprojname);
			}		
		}catch(BusinessException e1){
			getCardPanel().getHeadItem(e.getKey()).setValue(null);
			getClientUI().showErrorMessage(e1.getMessage());
			throw new BusinessException(e1.getMessage());
		}			
	}
	/**
	 * �����Ŀ+�ڳ��ĺϷ���
	 * @param vos
	 * @param vprojname
	 * @throws Exception
	 */
	public void checkBeginValid(SuperVO vos, String vprojname)  throws Exception {
		VatPubProxy.getProjCheckService().checkBisExistBeginDataWhenBegin(vos, vprojname);
		boolean bisExistFreeBegin = VatPubProxy.getProjCheckService().bisExistFreeBeginDataWhenBegin(vos);
		if(bisExistFreeBegin){
			int iRet = getClientUI().showYesNoMessage("��Ŀ��" + vprojname + "����������̬�ڳ����ݣ��Ƿ񸲸����е���?");
			if (iRet != nc.ui.pub.beans.MessageDialog.ID_YES){
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECT).setValue(null);
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECTBASE).setValue(null);
				throw new BusinessException("������ѡ����Ŀ��");
			}
			String ts = VatPubProxy.getProjService().getFreeTs(vos);
			getCardPanel().getHeadItem("ts").setValue(new UFDateTime(ts));
		}
	}
	/**
	 * �����Ŀ+�ڼ�ĺϷ���
	 * @param vos
	 * @param vprojname
	 * @throws Exception
	 */
	public void checkTermValid(SuperVO vos, String vprojname)  throws Exception {
		List<String> pk_projectbaseList = new ArrayList<String>();						
		pk_projectbaseList.add((String) vos.getAttributeValue(VatPubMetaNameConsts.PK_PROJECTBASE));
		VatPubProxy.getProjCheckService().checkBisExistTermDataWhenTerm(vos, vprojname);
		boolean bisExistFreeTerm = VatPubProxy.getProjCheckService().bisExistFreeTermDataWhenTerm(vos);
		if(bisExistFreeTerm){
			int iRet = getClientUI().showYesNoMessage("��Ŀ��" + vprojname + "����������̬���ڵ��ݣ��Ƿ񸲸����е���?");
			if (iRet != nc.ui.pub.beans.MessageDialog.ID_YES){
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECT).setValue(null);
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECTBASE).setValue(null);
				throw new BusinessException("������ѡ����Ŀ��");
			}
			else{						
				this.setMnyValue(pk_projectbaseList, (String)vos.getAttributeValue(VatPubMetaNameConsts.VPERIOD), vos);
			}
			String ts = VatPubProxy.getProjService().getFreeTs(vos);
			getCardPanel().getHeadItem("ts").setValue(new UFDateTime(ts));
		}
		else{
			this.setMnyValue(pk_projectbaseList, (String) vos.getAttributeValue(VatPubMetaNameConsts.VPERIOD), vos);
		}
	}
	/**
	 * ������������˰���Լ��༭��
	 * @param vos
	 * @throws Exception
	 */
	private void setLastRestMnyAndEditable(Map<String, UFDouble> mnyMap, List<String> pk_projectbaseList, SuperVO vos) throws Exception{
        boolean bisExistLastTerm = VatPubProxy.getProjCheckService().bisExistPassLastTermData(vos);
        boolean bisExistPassBegin = false;
        if(bisExistLastTerm){//���������ڼ�
        	//��mnyMap��ȡ��
        	getCardPanel().setHeadItem(VatProjanalyVO.NLRESTTAXMNY, mnyMap == null ? null : mnyMap.get(VatProjanalyVO.NLRESTTAXMNY));
        	getCardPanel().setHeadItem(VatProjanalyVO.NLRESTCUTMNY, mnyMap == null ? null : mnyMap.get(VatProjanalyVO.NLRESTCUTMNY));
        }
        else{
        	bisExistPassBegin = VatPubProxy.getProjCheckService().bisExistPassBeginDataWhenTerm(vos);
        	if(bisExistPassBegin){//�����ڳ�
        		//ȡ�ڳ����ݸ�ֵ
        		String pk_corp = getClientUI()._getCorp().pk_corp;
        		Map<String, Map<String, UFDouble>> lastRestMap = VatPubProxy.getProjService().getBeginRestMnyByProj(getHeadClassName(), 
        				pk_corp, pk_projectbaseList);
        		Map<String, UFDouble> restMnyMap = lastRestMap.get(pk_projectbaseList.get(0));
        		getCardPanel().setHeadItem(VatProjanalyVO.NLRESTTAXMNY, restMnyMap == null ? null : restMnyMap.get(VatProjanalyVO.NLRESTTAXMNY));
        		getCardPanel().setHeadItem(VatProjanalyVO.NLRESTCUTMNY, restMnyMap == null ? null : restMnyMap.get(VatProjanalyVO.NLRESTCUTMNY));
        	}
        }
		boolean bisEditable = !bisExistLastTerm && !bisExistPassBegin;
		if(bisEditable){
			if(null != getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTTAXMNY)){
				getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTTAXMNY).setEdit(true);
			}
			if(null != getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTCUTMNY)){
				getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTCUTMNY).setEdit(true);
			}
		}
		else{
			if(null != getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTTAXMNY)){
				getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTTAXMNY).setEdit(false);
			}
			if(null != getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTCUTMNY)){
				getClientUI().getBillCardPanel().getHeadItem(VatProjanalyVO.NLRESTCUTMNY).setEdit(false);
			}
		}
	}
	/**
	 * ����ͷ����ֶθ�ֵ
	 * @param pk_projectbaseList
	 * @param vperiodcode
	 * @param vos
	 * @throws Exception
	 */
	public void setHeadMnyValue(Map<String, UFDouble> mnyMap) throws Exception{
		VatProjMnyTool.setProjsalemny(mnyMap, getCardPanel());
		VatProjMnyTool.setOpenInvMnyField(mnyMap, getCardPanel());
		VatProjMnyTool.setReceInvMnyField(mnyMap, getCardPanel());// ����ת��
		VatProjMnyTool.setInAuthMnyField(mnyMap, getCardPanel());
		VatProjMnyTool.setIntoOutmnyField(mnyMap, getCardPanel());
		VatProjMnyTool.setThcutmnyField(mnyMap, getCardPanel());
		VatProjMnyTool.setThrestcutmnyField(mnyMap, getCardPanel());
		Integer iprojtaxtype = projsetVO.getItaxtype();
		UFDouble nprojtaxrate = projsetVO.getNtaxrate();
		VatProjMnyTool.setTaxablemnyAndRest(getCardPanel(), iprojtaxtype, nprojtaxrate);	
	}
	
	public void setMnyValue(List<String> pk_projectbaseList, String vperiodcode, SuperVO vos) throws Exception{
		UFBoolean bisbegin = getCardPanel().getHeadItem(VatPubMetaNameConsts.BISBEGIN) == null ? UFBoolean.FALSE :
			new UFBoolean((String)getCardPanel().getHeadItem(VatPubMetaNameConsts.BISBEGIN).getValueObject());
	    if(bisbegin.booleanValue()){
		    return;
	    }
	    String pk_corp = getClientUI()._getCorp().pk_corp;
		Map<String, Map<String, UFDouble>> projMnyMap = VatPubProxy.getProjService().getMnyMapByProj(getHeadClassName(), 
				pk_corp, pk_projectbaseList, vperiodcode);
		Map<String, UFDouble> mnyMap = projMnyMap.get(pk_projectbaseList.get(0));
		if(!"nc.vo.jzinv.vat1045.VatIncretaxpreVO".equals(getHeadClassName())){
			setLastRestMnyAndEditable(mnyMap, pk_projectbaseList, vos);
		}
		setBodyMnyValue(mnyMap);
		setHeadMnyValue(mnyMap);		
	}
	/**
	 * ����ͷ����ֶθ�ֵ
	 * @param pk_projectbaseList
	 * @param vperiodcode
	 * @param vos
	 * @throws BusinessException 
	 * @throws Exception
	 */
	public void setBodyMnyValue(Map<String, UFDouble> mnyMap) throws BusinessException{
		UFBoolean bisbegin = getCardPanel().getHeadItem(VatPubMetaNameConsts.BISBEGIN) == null ? UFBoolean.FALSE :
			new UFBoolean((String)getCardPanel().getHeadItem(VatPubMetaNameConsts.BISBEGIN).getValueObject());
		String pk_projectbase = (String) getCardPanel().getHeadItem(VatPubMetaNameConsts.PK_PROJECTBASE).getValueObject();
		String vperiod = (String) getCardPanel().getHeadItem(VatPubMetaNameConsts.VPERIOD).getValueObject();
		if(bisbegin.booleanValue()){
			return;
		}
		String pk_corp = getClientUI()._getCorp().pk_corp;
		Map<String, List<SuperVO>> bvoMap = null;
		List<String> pk_projectbaseList = new ArrayList<String>();
		pk_projectbaseList.add(pk_projectbase);
		if(StringUtils.equals(getTablecode(), VatProjanalyBVO.TABCODE)){
			bvoMap = VatPubProxy.getProjService().getSubReceVOList(pk_corp, pk_projectbaseList, vperiod);
		}
		else if(StringUtils.equals(getTablecode(), VatIncretaxLevyVO.TABCODE)){
			bvoMap = VatPubProxy.getProjPrePaySerivce().getSubReceVOList(pk_corp, pk_projectbaseList, vperiod);
		}
		if(null != bvoMap && bvoMap.size() > 0){
			if(null != getCardPanel().getBillModel(getTablecode())){
				List<SuperVO> bvoList = bvoMap.get(pk_projectbase);
				getCardPanel().getBillModel(getTablecode()).setBodyDataVO(bvoList.toArray(new SuperVO[0]));
				getCardPanel().getBillModel(getTablecode()).execLoadFormula();
			}		
		}
		else{
			if(null != getCardPanel().getBillModel(getTablecode())){
				getCardPanel().getBillModel(getTablecode()).setBodyDataVO(null);
			}
		}
	}
	/**
	 * ��ȡ��Ŀ��˰��ʽ
	 * @param pk_project
	 * @return
	 */
	private VatProjtaxsetVO getProjsetVOByProj(String pk_corp, String pk_projectbase){
		List<String> pk_projectbaseList = new ArrayList<String>();
		pk_projectbaseList.add(pk_projectbase);
		try {
			Map<String, VatProjtaxsetVO> taxtypeMap = VatPubProxy.getProjService().getProjtaxsetMap(pk_projectbase, null, null);
			if(null != taxtypeMap && taxtypeMap.size() > 0){
				VatProjtaxsetVO setVO = taxtypeMap.get(pk_projectbase);				
				return setVO;
			}
		} catch (BusinessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * ��ȡ��˰��֯����VO
	 * @param pk_project
	 * @return
	 */
	private VatTaxorgsetVO getVatTaxorgsetVO(String pk_corp, String pk_projectbase){
		List<String> pk_projectbaseList = new ArrayList<String>();
		pk_projectbaseList.add(pk_projectbase);
		try {
			Map<String, VatTaxorgsetVO> taxOrgMap = VatPubProxy.getProjService().getVatTaxorgsetMap(pk_corp, pk_projectbaseList);
			if(null != taxOrgMap && taxOrgMap.size() > 0){
				return taxOrgMap.get(pk_projectbase);
			}
		} catch (BusinessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public VatProjtaxsetVO getProjsetVO() {
		return projsetVO;
	}

	public VatTaxorgsetVO getOrgsetVO() {
		return orgsetVO;
	}
	/**
	 * ���������д(Ĭ��Ϊ��Ŀ˰�����)
	 * @return
	 */
	public String getHeadClassName(){
		return VatProjanalyVO.class.getName();
	}
	/**
	 * ���������д(Ĭ��Ϊ��Ŀ˰�����)
	 * @return
	 */
	public String getTablecode(){
		return VatProjanalyBVO.TABCODE;
	}
}
