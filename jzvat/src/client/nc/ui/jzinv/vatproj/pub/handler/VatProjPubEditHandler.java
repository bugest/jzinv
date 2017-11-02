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
	 * 项目计税方式VO
	 */
	private VatProjtaxsetVO projsetVO;
	/**
	 * 纳税组织定义VO
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
	 * 检查项目+期初/期间数据的合法性
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
	 * 检查项目+期初的合法性
	 * @param vos
	 * @param vprojname
	 * @throws Exception
	 */
	public void checkBeginValid(SuperVO vos, String vprojname)  throws Exception {
		VatPubProxy.getProjCheckService().checkBisExistBeginDataWhenBegin(vos, vprojname);
		boolean bisExistFreeBegin = VatPubProxy.getProjCheckService().bisExistFreeBeginDataWhenBegin(vos);
		if(bisExistFreeBegin){
			int iRet = getClientUI().showYesNoMessage("项目【" + vprojname + "】存在自由态期初单据，是否覆盖已有单据?");
			if (iRet != nc.ui.pub.beans.MessageDialog.ID_YES){
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECT).setValue(null);
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECTBASE).setValue(null);
				throw new BusinessException("请重新选择项目！");
			}
			String ts = VatPubProxy.getProjService().getFreeTs(vos);
			getCardPanel().getHeadItem("ts").setValue(new UFDateTime(ts));
		}
	}
	/**
	 * 检查项目+期间的合法性
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
			int iRet = getClientUI().showYesNoMessage("项目【" + vprojname + "】存在自由态当期单据，是否覆盖已有单据?");
			if (iRet != nc.ui.pub.beans.MessageDialog.ID_YES){
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECT).setValue(null);
				getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECTBASE).setValue(null);
				throw new BusinessException("请重新选择项目！");
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
	 * 设置上期留抵税额以及编辑性
	 * @param vos
	 * @throws Exception
	 */
	private void setLastRestMnyAndEditable(Map<String, UFDouble> mnyMap, List<String> pk_projectbaseList, SuperVO vos) throws Exception{
        boolean bisExistLastTerm = VatPubProxy.getProjCheckService().bisExistPassLastTermData(vos);
        boolean bisExistPassBegin = false;
        if(bisExistLastTerm){//存在上期期间
        	//从mnyMap中取数
        	getCardPanel().setHeadItem(VatProjanalyVO.NLRESTTAXMNY, mnyMap == null ? null : mnyMap.get(VatProjanalyVO.NLRESTTAXMNY));
        	getCardPanel().setHeadItem(VatProjanalyVO.NLRESTCUTMNY, mnyMap == null ? null : mnyMap.get(VatProjanalyVO.NLRESTCUTMNY));
        }
        else{
        	bisExistPassBegin = VatPubProxy.getProjCheckService().bisExistPassBeginDataWhenTerm(vos);
        	if(bisExistPassBegin){//存在期初
        		//取期初数据赋值
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
	 * 给表头金额字段赋值
	 * @param pk_projectbaseList
	 * @param vperiodcode
	 * @param vos
	 * @throws Exception
	 */
	public void setHeadMnyValue(Map<String, UFDouble> mnyMap) throws Exception{
		VatProjMnyTool.setProjsalemny(mnyMap, getCardPanel());
		VatProjMnyTool.setOpenInvMnyField(mnyMap, getCardPanel());
		VatProjMnyTool.setReceInvMnyField(mnyMap, getCardPanel());// 进项转出
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
	 * 给表头金额字段赋值
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
	 * 获取项目计税方式
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
	 * 获取纳税组织定义VO
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
	 * 子类可以重写(默认为项目税金计算)
	 * @return
	 */
	public String getHeadClassName(){
		return VatProjanalyVO.class.getName();
	}
	/**
	 * 子类可以重写(默认为项目税金计算)
	 * @return
	 */
	public String getTablecode(){
		return VatProjanalyBVO.TABCODE;
	}
	
	/** 
	* @Title: setNtaxablesalemnyByWhoHigher 
	* @Description: 项目或区间改变时根据孰高原则设置应税销售额和一些默认值
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
		//合同结算金额
		UFDouble ncontractsettlemny = null;
		//累计开票金额
		UFDouble ncumulativeinvoicemny = null;
		//累计收款金额
		UFDouble naccumulatedreceivemny = null;
		//当期收款金额
		UFDouble currentreceivemny = null;
		//前期累计销项
		UFDouble naccumulativemny = null;
		//如果项目或者区间的信息改变成空的话就把相关信息都清空
		if (pk_project == null || pk_project.trim().equals("") 
				|| vperiod == null || vperiod.trim().equals("")) {
			//合同结算金额
			ncontractsettlemny = null;
			//累计开票金额
			ncumulativeinvoicemny = null;
			//累计收款金额
			naccumulatedreceivemny = null;
			//当期收款金额
			currentreceivemny = null;
			//前期累计销项
			naccumulativemny = null;
		} else {
			//目前所有的查找先不判断区间的问题，只用审批过的单子就ok ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！，以后有需求再加
			//当项目和期间都有时则先计算一些根据项目管理出来的值
			//1.“合同结算金额”： 取【收款申请单】（本期结算金额汇总，用项目去关联）
			ncontractsettlemny = getNcontractsettlemny(pk_project, pk_corp);
			//2“ 累计开票金额”： 取项目累计开票金额（开票or开票无合同发票金额总和，用项目去关联）
			ncumulativeinvoicemny = getNcumulativeinvoicemny(pk_project, pk_corp);
			//3 “ 累计收款金额”： 取【收款单】收款明细子表“本次收款金额”历史数据汇总（sum（本次收款金额） 项目关联，要求审批通过的，月份＜=当期最晚日期）
			naccumulatedreceivemny = getNaccumulatedreceivemny(pk_project, pk_corp);
			//4“当期收款金额”： 取【收款单】收款明细子表“本次收款金额”的汇总（取本期间所有的汇总，sum（本次收款金额） 项目关联，要求审批通过的）
			currentreceivemny = getCurrentreceivemny(pk_project, pk_corp, vperiod);
			//5“前期累计销项”：应税销售额的历史数据累计（取本单据 sum(应税销售额),审批通过，同项目）
			naccumulativemny = getNaccumulativemny(pk_project, pk_corp);
		}
		//将值填入前台-----
		//合同结算金额
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NCONTRACTSETTLEMNY).setValue(ncontractsettlemny);
		//累计开票金额
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NCUMULATIVEINVOICEMNY).setValue(ncumulativeinvoicemny);
		//累计收款金额
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NACCUMULATEDRECEIVEMNY).setValue(naccumulatedreceivemny);
		//当期收款金额
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.CURRENTRECEIVEMNY).setValue(currentreceivemny);
		//前期累计销项
		getClientUI().getBillCardPanel().
			getHeadItem(VatProjanalyVO.NACCUMULATIVEMNY).setValue(naccumulativemny);
		//根据选择的金额
	}
	
	/** 
	* @Title: setNcontractsettlemny 
	* @Description: 设置合同结算金额 ,目前不考虑区间
	* @param     
	* @return void    
	* @throws 
	*/
	private UFDouble getNcontractsettlemny(String pk_project, String pk_corp) {
		UFDouble ncontractsettlemny = UFDouble.ZERO_DBL;
		//找收款申请单 ----本期结算金额汇总，用项目去关联
		try {
			List<InIncomeVO> inIncomeVOVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryInIncomeHeadVOsByCond(pk_project, pk_corp);
			if (inIncomeVOVOList != null && !inIncomeVOVOList.isEmpty()) {
				for (InIncomeVO inIncomeVO : inIncomeVOVOList) {
					//只统计审批通过的
					if (IBillStatus.CHECKPASS == inIncomeVO.getVbillstatus()) {
						ncontractsettlemny = ncontractsettlemny.add(inIncomeVO.getNreporigintaxmny() == null ?
								UFDouble.ZERO_DBL : inIncomeVO.getNreporigintaxmny());
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error("查询收款申请单信息错误！", e);
			getClientUI().showErrorMessage("查询收款申请单信息错误!");
		}
		return ncontractsettlemny;
	}
	
	/** 
	* @Title: getNcumulativeinvoicemny 
	* @Description: 计算 累计开票金额
	* @param @param pk_project
	* @param @param pk_corp
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getNcumulativeinvoicemny(String pk_project, String pk_corp) {
		UFDouble ncumulativeinvoicemny = UFDouble.ZERO_DBL;
		//找收款申请单 ----本期结算金额汇总，用项目去关联
		try {
			List<OpenHVO> openHVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryOpenHVOsByCond(pk_project, pk_corp);
			if (openHVOList != null && !openHVOList.isEmpty()) {
				for (OpenHVO openHVO : openHVOList) {
					//只统计审批通过的
					if (IBillStatus.CHECKPASS == openHVO.getVbillstatus()) {
						ncumulativeinvoicemny = ncumulativeinvoicemny.add(openHVO.getNthopentaxmny() == null ?
								UFDouble.ZERO_DBL : openHVO.getNthopentaxmny());
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error("查询收款申请单信息错误！", e);
			getClientUI().showErrorMessage("查询收款申请单信息错误!");
		}
		return ncumulativeinvoicemny;		
	}
	
	/** 
	* @Title: getNaccumulatedreceivemny 
	* @Description: 累计收款金额 
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getNaccumulatedreceivemny(String pk_project, String pk_corp) {
		UFDouble naccumulatedreceivemny = UFDouble.ZERO_DBL;
		//找收款申请单 ----本期结算金额汇总，用项目去关联
		try {
			List<InRecdetailVO> recdetailVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryInRecdetailVOByCond(pk_project, pk_corp, IBillStatus.CHECKPASS, null);
			if (recdetailVOList != null && !recdetailVOList.isEmpty()) {
				for (InRecdetailVO recdetailVO : recdetailVOList) {
					//只统计审批通过的
					naccumulatedreceivemny = naccumulatedreceivemny.add(recdetailVO.getNrecoriginmny() == null ?
							UFDouble.ZERO_DBL : recdetailVO.getNrecoriginmny());
				}
			}
		} catch (BusinessException e) {
			Logger.error("查询累计收款金额信息错误！", e);
			getClientUI().showErrorMessage("查询累计收款金额信息错误!");
		}
		return naccumulatedreceivemny;			
	}
	
	/** 
	* @Title: getCurrentreceivemny 
	* @Description: 当期收款金额 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @param vperiod
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getCurrentreceivemny(String pk_project, String pk_corp, String vperiod) {
		UFDouble currentreceivemny = UFDouble.ZERO_DBL;
		//找收款申请单 ----本期结算金额汇总，用项目去关联
		try {
			List<InRecdetailVO> recdetailVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryInRecdetailVOByCond(pk_project, pk_corp, IBillStatus.CHECKPASS, vperiod);
			if (recdetailVOList != null && !recdetailVOList.isEmpty()) {
				for (InRecdetailVO recdetailVO : recdetailVOList) {
					//只统计审批通过的
					currentreceivemny = currentreceivemny.add(recdetailVO.getNrecoriginmny() == null ?
							UFDouble.ZERO_DBL : recdetailVO.getNrecoriginmny());
				}
			}
		} catch (BusinessException e) {
			Logger.error("查询当期收款金额信息错误！", e);
			getClientUI().showErrorMessage("查询当期收款金额信息错误!");
		}
		return currentreceivemny;			
	}
	
	/** 
	* @Title: getNaccumulativemny 
	* @Description: 统计应税销售额 
	* @param @param pk_project
	* @param @param pk_corp
	* @param @param vperiod
	* @param @return    
	* @return UFDouble    
	* @throws 
	*/
	private UFDouble getNaccumulativemny(String pk_project, String pk_corp) {
		UFDouble naccumulativemny = UFDouble.ZERO_DBL;
		//找收款申请单 ----本期结算金额汇总，用项目去关联
		try {
			List<VatProjanalyVO> vatProjanalyVOList = NCLocator.getInstance().lookup(IVatProjanalyService.class)
				.queryVatProjanalyVOsByCond(pk_project, pk_corp);
			if (vatProjanalyVOList != null && !vatProjanalyVOList.isEmpty()) {
				for (VatProjanalyVO vatProjanalyVO : vatProjanalyVOList) {
					//只统计审批通过的
					if (IBillStatus.CHECKPASS == vatProjanalyVO.getVbillstatus()) {
						naccumulativemny = naccumulativemny.add(vatProjanalyVO.getNtaxablesalemny() == null ?
								UFDouble.ZERO_DBL : vatProjanalyVO.getNtaxablesalemny());
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error("查询项目税金计算信息错误！", e);
			getClientUI().showErrorMessage("查询项目税金计算信息错误!");
		}
		return naccumulativemny;			
	}	
	
}
