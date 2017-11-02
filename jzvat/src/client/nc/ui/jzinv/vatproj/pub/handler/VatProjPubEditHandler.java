package nc.ui.jzinv.vatproj.pub.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.jzinv.vat1050.IVatProjanalyService;
import nc.ui.jzinv.vat1050.tool.VatProjMnyTool;
import nc.ui.jzinv.vatpub.handler.VatCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.inv0510.OpenHVO;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
import nc.vo.jzinv.vat0510.VatProjtaxsetVO;
import nc.vo.jzinv.vat1045.VatIncretaxLevyVO;
import nc.vo.jzinv.vat1050.VatProjanalyBVO;
import nc.vo.jzinv.vat1050.VatProjanalyVO;
import nc.vo.jzinv.vatpub.VatPubMetaNameConsts;
import nc.vo.jzinv.vatpub.VatPubProxy;
import nc.vo.jzpm.in2005.InIncomeVO;
import nc.vo.jzpm.in2010.InRecdetailVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.trade.pub.IBillStatus;

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
	
	/** 
	* @Title: setNtaxablesalemnyByWhoHigher 
	* @Description: ��Ŀ������ı�ʱ�������ԭ������Ӧ˰���۶��һЩĬ��ֵ
	* @param     
	* @return void    
	* @throws 
	*/
	public void setNtaxablesalemnyByProjectOrPeriodChange() {
		String pk_project = (String) getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.PK_PROJECT).getValueObject();
		String vperiod = (String) getClientUI().getBillCardPanel().
				getHeadItem(VatProjanalyVO.VPERIOD).getValueObject();
		String pk_corp = getClientUI()._getCorp().pk_corp;
		//��ͬ������
		UFDouble ncontractsettlemny = null;
		//�ۼƿ�Ʊ���
		UFDouble ncumulativeinvoicemny = null;
		//�ۼ��տ���
		UFDouble naccumulatedreceivemny = null;
		//�����տ���
		UFDouble currentreceivemny = null;
		//ǰ���ۼ�����
		UFDouble naccumulativemny = null;
		//�����Ŀ�����������Ϣ�ı�ɿյĻ��Ͱ������Ϣ�����
		if (pk_project == null || pk_project.trim().equals("") 
				|| vperiod == null || vperiod.trim().equals("")) {
			//��ͬ������
			ncontractsettlemny = null;
			//�ۼƿ�Ʊ���
			ncumulativeinvoicemny = null;
			//�ۼ��տ���
			naccumulatedreceivemny = null;
			//�����տ���
			currentreceivemny = null;
			//ǰ���ۼ�����
			naccumulativemny = null;
		} else {
			//Ŀǰ���еĲ����Ȳ��ж���������⣬ֻ���������ĵ��Ӿ�ok �����������������������������������������������������������������Ժ��������ټ�
			//����Ŀ���ڼ䶼��ʱ���ȼ���һЩ������Ŀ���������ֵ
			//1.����ͬ������� ȡ���տ����뵥�������ڽ�������ܣ�����Ŀȥ������
			ncontractsettlemny = getNcontractsettlemny(pk_project, pk_corp);
			//2�� �ۼƿ�Ʊ���� ȡ��Ŀ�ۼƿ�Ʊ����Ʊor��Ʊ�޺�ͬ��Ʊ����ܺͣ�����Ŀȥ������
			ncumulativeinvoicemny = getNcumulativeinvoicemny(pk_project, pk_corp);
			//3 �� �ۼ��տ���� ȡ���տ���տ���ϸ�ӱ������տ����ʷ���ݻ��ܣ�sum�������տ�� ��Ŀ������Ҫ������ͨ���ģ��·ݣ�=�����������ڣ�
			naccumulatedreceivemny = getNaccumulatedreceivemny(pk_project, pk_corp);
			//4�������տ���� ȡ���տ���տ���ϸ�ӱ������տ���Ļ��ܣ�ȡ���ڼ����еĻ��ܣ�sum�������տ�� ��Ŀ������Ҫ������ͨ���ģ�
			currentreceivemny = getCurrentreceivemny(pk_project, pk_corp, vperiod);
			//5��ǰ���ۼ������Ӧ˰���۶����ʷ�����ۼƣ�ȡ������ sum(Ӧ˰���۶�),����ͨ����ͬ��Ŀ��
			naccumulativemny = getNaccumulativemny(pk_project, pk_corp);
		}
		//��ֵ����ǰ̨-----
		//��ͬ������
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NCONTRACTSETTLEMNY).setValue(ncontractsettlemny);
		//�ۼƿ�Ʊ���
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NCUMULATIVEINVOICEMNY).setValue(ncumulativeinvoicemny);
		//�ۼ��տ���
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NACCUMULATEDRECEIVEMNY).setValue(naccumulatedreceivemny);
		//�����տ���
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.CURRENTRECEIVEMNY).setValue(currentreceivemny);
		//ǰ���ۼ�����
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NACCUMULATIVEMNY).setValue(naccumulativemny);
		//����ѡ��Ľ��
	}
	
	/** 
	* @Title: setNcontractsettlemny 
	* @Description: ���ú�ͬ������ ,Ŀǰ����������
	* @param     
	* @return void    
	* @throws 
	*/
	private UFDouble getNcontractsettlemny(String pk_project, String pk_corp) {
		UFDouble ncontractsettlemny = UFDouble.ZERO_DBL;
		//���տ����뵥 ----���ڽ�������ܣ�����Ŀȥ����
		try {
			List<InIncomeVO> inIncomeVOVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryInIncomeHeadVOsByCond(pk_project, pk_corp);
			if (inIncomeVOVOList != null && !inIncomeVOVOList.isEmpty()) {
				for (InIncomeVO inIncomeVO : inIncomeVOVOList) {
					//ֻͳ������ͨ����
					if (IBillStatus.CHECKPASS == inIncomeVO.getVbillstatus()) {
						ncontractsettlemny = ncontractsettlemny.add(inIncomeVO.getNreporigintaxmny() == null ?
								UFDouble.ZERO_DBL : inIncomeVO.getNreporigintaxmny());
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error("��ѯ�տ����뵥��Ϣ����", e);
			getClientUI().showErrorMessage("��ѯ�տ����뵥��Ϣ����!");
		}
		return ncontractsettlemny;
	}
	
	/** 
	* @Title: getNcumulativeinvoicemny 
	* @Description: ���� �ۼƿ�Ʊ���
	* @param @param pk_project
	* @param @param pk_corp
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getNcumulativeinvoicemny(String pk_project, String pk_corp) {
		UFDouble ncumulativeinvoicemny = UFDouble.ZERO_DBL;
		//���տ����뵥 ----���ڽ�������ܣ�����Ŀȥ����
		try {
			List<OpenHVO> openHVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryOpenHVOsByCond(pk_project, pk_corp);
			if (openHVOList != null && !openHVOList.isEmpty()) {
				for (OpenHVO openHVO : openHVOList) {
					//ֻͳ������ͨ����
					if (IBillStatus.CHECKPASS == openHVO.getVbillstatus()) {
						ncumulativeinvoicemny = ncumulativeinvoicemny.add(openHVO.getNthopentaxmny() == null ?
								UFDouble.ZERO_DBL : openHVO.getNthopentaxmny());
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error("��ѯ�տ����뵥��Ϣ����", e);
			getClientUI().showErrorMessage("��ѯ�տ����뵥��Ϣ����!");
		}
		return ncumulativeinvoicemny;		
	}
	
	/** 
	* @Title: getNaccumulatedreceivemny 
	* @Description: �ۼ��տ��� 
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getNaccumulatedreceivemny(String pk_project, String pk_corp) {
		UFDouble naccumulatedreceivemny = UFDouble.ZERO_DBL;
		//���տ����뵥 ----���ڽ�������ܣ�����Ŀȥ����
		try {
			List<InRecdetailVO> recdetailVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryInRecdetailVOByCond(pk_project, pk_corp, IBillStatus.CHECKPASS, null);
			if (recdetailVOList != null && !recdetailVOList.isEmpty()) {
				for (InRecdetailVO recdetailVO : recdetailVOList) {
					//ֻͳ������ͨ����
					naccumulatedreceivemny = naccumulatedreceivemny.add(recdetailVO.getNrecoriginmny() == null ?
							UFDouble.ZERO_DBL : recdetailVO.getNrecoriginmny());
				}
			}
		} catch (BusinessException e) {
			Logger.error("��ѯ�ۼ��տ�����Ϣ����", e);
			getClientUI().showErrorMessage("��ѯ�ۼ��տ�����Ϣ����!");
		}
		return naccumulatedreceivemny;			
	}
	
	/** 
	* @Title: getCurrentreceivemny 
	* @Description: �����տ��� 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @param vperiod
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getCurrentreceivemny(String pk_project, String pk_corp, String vperiod) {
		UFDouble currentreceivemny = UFDouble.ZERO_DBL;
		//���տ����뵥 ----���ڽ�������ܣ�����Ŀȥ����
		try {
			List<InRecdetailVO> recdetailVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryInRecdetailVOByCond(pk_project, pk_corp, IBillStatus.CHECKPASS, vperiod);
			if (recdetailVOList != null && !recdetailVOList.isEmpty()) {
				for (InRecdetailVO recdetailVO : recdetailVOList) {
					//ֻͳ������ͨ����
					currentreceivemny = currentreceivemny.add(recdetailVO.getNrecoriginmny() == null ?
							UFDouble.ZERO_DBL : recdetailVO.getNrecoriginmny());
				}
			}
		} catch (BusinessException e) {
			Logger.error("��ѯ�����տ�����Ϣ����", e);
			getClientUI().showErrorMessage("��ѯ�����տ�����Ϣ����!");
		}
		return currentreceivemny;			
	}
	
	/** 
	* @Title: getNaccumulativemny 
	* @Description: ͳ��Ӧ˰���۶� 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @param vperiod
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getNaccumulativemny(String pk_project, String pk_corp) {
		UFDouble naccumulativemny = UFDouble.ZERO_DBL;
		//���տ����뵥 ----���ڽ�������ܣ�����Ŀȥ����
		try {
			List<VatProjanalyVO> vatProjanalyVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryVatProjanalyVOsByCond(pk_project, pk_corp);
			if (vatProjanalyVOList != null && !vatProjanalyVOList.isEmpty()) {
				for (VatProjanalyVO vatProjanalyVO : vatProjanalyVOList) {
					//ֻͳ������ͨ����
					if (IBillStatus.CHECKPASS == vatProjanalyVO.getVbillstatus()) {
						naccumulativemny = naccumulativemny.add(vatProjanalyVO.getNtaxablesalemny() == null ?
								UFDouble.ZERO_DBL : vatProjanalyVO.getNtaxablesalemny());
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error("��ѯ��Ŀ˰�������Ϣ����", e);
			getClientUI().showErrorMessage("��ѯ��Ŀ˰�������Ϣ����!");
		}
		return naccumulativemny;			
	}	
	
}
