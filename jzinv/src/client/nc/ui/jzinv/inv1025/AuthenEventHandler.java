package nc.ui.jzinv.inv1025;

import java.awt.Container;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.framework.common.NCLocator;
import nc.bs.framework.exception.ComponentException;
import nc.itf.jzinv.inv1025.IInvAuthenSevice;
import nc.itf.jzinv.invpub.IJzinvMaintain;
import nc.itf.jzinv.pub.IProcessable;
import nc.itf.jzinv.pub.JZProxy;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.AuthenSysCallAdapter;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.AuthenSystemCallFactory;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.CheckAuthenRefCallAdapter;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.CheckAuthenRefCallFactory;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.InvoiceAuthenUploadCallAdapter;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.InvoiceAuthenUploadCallFactory;
import nc.itf.jzinv.pub.outsys.call.adapter.authen.impl.BwCheckAuthenCallAdapter;
import nc.itf.jzinv.pub.outsys.call.adapter.ocr.OcrOutSysCallAdapter;
import nc.itf.jzinv.pub.outsys.call.adapter.ocr.OcrSystemCallFactory;
import nc.itf.jzinv.receive.IReceiveService;
import nc.ui.jzinv.inv1025.dlg.AuthInfoModel;
import nc.ui.jzinv.inv1025.dlg.AuthenDlg;
import nc.ui.jzinv.pub.dlg.InfoDialog;
import nc.ui.jzinv.pub.eventhandler.ManageEventHandler;
import nc.ui.jzinv.pub.excel2.importer.NCExcelImportAdapter;
import nc.ui.jzinv.pub.tool.ProcessTool;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIDialog;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillListPanel;
import nc.ui.pub.bill.BillModel;
import nc.ui.trade.base.IBillOperate;
import nc.ui.trade.controller.IControllerBase;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.inv0305.BlankInvoiceVO;
import nc.vo.jzinv.inv1025.InvExcelImportVO;
import nc.vo.jzinv.invpub.InvConsts;
import nc.vo.jzinv.param.InvParamTool;
import nc.vo.jzinv.pub.IJzinvButton;
import nc.vo.jzinv.pub.JZINVProxy;
import nc.vo.jzinv.pub.tool.InSqlManager;
import nc.vo.jzinv.receive.AggReceiveVO;
import nc.vo.jzinv.receive.ReceiveBVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.trade.pub.HYBillVO;
import nc.vo.trade.pub.IBillStatus;

import org.apache.commons.lang.StringUtils;

/**
 * 发票认证按钮的处理
 * 
 * @author mayyc
 * 
 */
public class AuthenEventHandler extends ManageEventHandler {
	private AuthInfoModel authModel = null;
	private AuthenSysCallAdapter adapter = null;
	private CheckAuthenRefCallAdapter checkRefAdapter = null;
	private InvoiceAuthenUploadCallAdapter invUploadAdapter = null;


	private InfoDialog dialog;
	private Map<String, Integer> selecRowMap = new HashMap<String, Integer>();
	private Map<String, String> toImportNeedMap = new HashMap<String, String>();
	private OcrOutSysCallAdapter ocrAdapter = OcrSystemCallFactory.getOcrSysCaller(getClientUI(), getClientUI().getPk_corp());

	private AuthenClientUI getClientUI() {
		return (AuthenClientUI) getBillUI();
	}

	public AuthenEventHandler(BillManageUI billUI, IControllerBase control,
			InfoDialog dialog) {
		super(billUI, control);
		this.dialog = dialog;
		this.adapter = AuthenSystemCallFactory.getAuthenCaller(getClientUI());
		this.checkRefAdapter = CheckAuthenRefCallFactory.getInvAuthRefCaller(getClientUI());
	}
	
	@Override
	protected void onBoSave() throws Exception {
		if(checkBeforeSave()){
			ReceiveVO editedVO = (ReceiveVO) getClientUI().getBillCardPanelWrapper().getBillVOFromUI().getParentVO();
			ReceiveVO headVO = (ReceiveVO) JZINVProxy.getIUifService().queryByPrimaryKey(ReceiveVO.class, editedVO.getPk_receive());
			headVO.setVinvcode(editedVO.getVinvcode());
			headVO.setVinvno(editedVO.getVinvno());
			headVO.setDopendate(editedVO.getDopendate());
			headVO.setVtaxsuppliernumber(editedVO.getVtaxsuppliernumber());
			headVO.setVtaxpayernumber(editedVO.getVtaxpayernumber());
			headVO.setVsecret(editedVO.getVsecret());
			JZINVProxy.getIUifService().update(headVO);
			setSaveOperateState();
			onBoRefresh();
		}
	}
	private boolean checkBeforeSave() throws ComponentException, BusinessException{
		//发票代码
		String vinvcode = getHeadItemValue(ReceiveVO.VINVCODE).toString();
		//发票号码
		String vinvno = getHeadItemValue(ReceiveVO.VINVNO).toString();
		if(!vinvno.matches("^[0-9]+$")){
			MessageDialog.showHintDlg(getClientUI(), "提示", "发票号必须为数字！");
			return false;
		}
		vinvno=String.format("%08d",Long.valueOf(vinvno));
		getClientUI().getBillCardPanel().setHeadItem(ReceiveVO.VINVNO, vinvno);
		String pk_receicve = getHeadItemValue(ReceiveVO.PK_RECEIVE).toString();
		SuperVO[] vos = JZINVProxy.getJZUifService().queryByConditionAtJZ(ReceiveVO.class, 
				" dr=0 and vinvcode='"+vinvcode+"' and vinvno='"+vinvno+"' and pk_receive <>'"+pk_receicve+"'");
		if(vos.length>0){
			MessageDialog.showHintDlg(getClientUI(), "提示", "发票代码+发票号已存在！");
			return false;
		}
		return true;
	}
	private Object getHeadItemValue(String key){
		return getClientUI().getBillCardPanel().getHeadItem(key).getValueObject();
	}

	@Override
	protected void onBoElse(int intBtn) throws Exception {
		super.onBoElse(intBtn);

		switch (intBtn) {
		case IJzinvButton.AuthenBtn:
			authen(IJzinvButton.AuthenBtn);
			break;
		case IJzinvButton.UnauthenBtn:
			unauthen(IJzinvButton.UnauthenBtn);
			break;
		case IJzinvButton.INVOICE_REGEISTER:
			registerInvoice();
			break;
		case IJzinvButton.FETCH_INVOICE_REGEISTER_RESULT:
			fetchRegisterResult();
			break;
		case IJzinvButton.INV_BATCHIMPORT:
			onBoExcelImport();
			break;
		case IJzinvButton.IMAGE_LOAD:
			viewReceiveImage();
			break;
		case IJzinvButton.SUPPLEMENTINFO:
			addInfo();
		case IJzinvButton.BTN_ACCOUNT:
			accountbtn();
			break;
		case IJzinvButton.BTN_UNACCOUNT:
			unaccountbtn();
			break;
		case IJzinvButton.BTN_QUERYACCOUNT:
			queryaccountbtn();
			break;
		//勾选
		case IJzinvButton.CHECKAUTH:
			checkAuthBtn();
			break;
		//撤销勾选
		case IJzinvButton.UNCHECKAUTH:
			uncheckAuthBtn();
			break;
		//勾选并认证
		case IJzinvButton.CHECKCONFIRMAUTH:
			checkConfrimAuthBtn();
			break;
		//刷新勾选认证状态
		case IJzinvButton.CHECKAURHENREF:
			checkAuthRefBtn();
			break;
		default:
			break;
		}
	}
	
	private void checkAuthRefBtn() throws Exception {
		List<AggReceiveVO> receiveVOs = getClientUI().getSelectVOs();
		if(receiveVOs.size() == 0)
			throw new BusinessException("请选中需要刷新认证状态的单据");
		for(AggReceiveVO aggReceiveVO : receiveVOs){
			ReceiveVO receiveVO = (ReceiveVO)aggReceiveVO.getParentVO();
			UFBoolean bisselected = receiveVO.getBisselected() == null ? UFBoolean.FALSE : receiveVO.getBisselected();
			if(!bisselected.booleanValue()){
				throw new BusinessException("存在未勾选认证的单据，无法进行认证状态检查");
			}
		}
		
		if(checkRefAdapter == null){
			throw new BusinessException("请接入认证接口!");
		}else{
			checkRefAdapter.uploadInvoiceAuthRef();
		}
		onBoRefresh();
	}

	/**
	 * @throws Exception 
	 * @Description: 勾选点击后事件：将“已勾选”设置为“勾选”
	 * @Title: checkAuthBtn
	 */
	private void checkAuthBtn() throws Exception{
		setBillPanelBisselected(UFBoolean.TRUE);
	}
	
	/**
	 * @throws Exception 
	 * @Description: 撤销勾选点击后事件：将“勾选”设置为“不勾选”
	 * @Title: checkAuthBtn
	 */
	private void uncheckAuthBtn() throws Exception{
		setBillPanelBisselected(UFBoolean.FALSE);
	}
	/**
	 * @Description: 勾选并确认按钮点击事件：将“勾选”设置为“不勾选”并传pyt接口
	 * @Title: checkConfrimAuthBtn  
	 * @throws Exception
	 */
	private void checkConfrimAuthBtn() throws Exception{
		setBillPanelBisselected(UFBoolean.TRUE);
		invUploadAdapter = InvoiceAuthenUploadCallFactory.getInvUploadCaller(getClientUI());
		if(invUploadAdapter == null){
			throw new BusinessException("请接入勾选认证接口!");
		}else{
			if(invUploadAdapter instanceof BwCheckAuthenCallAdapter){
				//弹框 IJzinvTemplateConsts JZINV_AUTHEN
				AuthenDlg dlg = new AuthenDlg(getClientUI());
				if (dlg.showModal() == UIDialog.ID_OK) {
					String auther = (String) dlg.getValueByCode(ReceiveVO.PK_AUTHENPSN);//认证人
					String authdate = (String) dlg.getValueByCode(ReceiveVO.DAUTHENDATE);//项目主键	dauthendate
					String taxperiod = (String) dlg.getValueByCode(ReceiveVO.VTAXPERIOD);//项目主键	vtaxperiod
					invUploadAdapter.uploadInvoiceAuth(auther, authdate, taxperiod);
				}
			}else{
				invUploadAdapter.uploadInvoiceAuth(null, null, null);
			}
			
		}
		
		onBoRefresh();
	}
	
	
	/**
	 * @Description: 设置卡片或列表界面的“已勾选”字段的值
	 * @Title: setBillPanelBisselected  
	 * @param ufb
	 * @throws Exception 
	 */
	private boolean setBillPanelBisselected(UFBoolean ufb) throws Exception{
		List<AggReceiveVO> receiveVOs = getClientUI().getSelectVOs();
		for(AggReceiveVO aggReceiveVO : receiveVOs){
			ReceiveVO receiveVO = (ReceiveVO)aggReceiveVO.getParentVO();
			UFBoolean bisselected = receiveVO.getBisselected() == null ? UFBoolean.FALSE : receiveVO.getBisselected();
			if(!bisselected.booleanValue() && ufb.booleanValue()){
				receiveVO.setBisselected(ufb);
			}
			else if(bisselected.booleanValue() && !ufb.booleanValue()){
				receiveVO.setBisselected(ufb);
			}
		}
		boolean listPanelSelected = getClientUI().isListPanelSelected();
		ReceiveVO[] hvos = new ReceiveVO[receiveVOs.size()];
		for(int i = 0; i < hvos.length; i++){
			ReceiveVO parentVO = (ReceiveVO) receiveVOs.get(i).getParentVO();
			hvos[i] = parentVO;
		}
			
		JZINVProxy.getIUifService().updateAry(hvos);
		updateBatchSelectRows(hvos);
		return listPanelSelected;
	}
	/**
	 * 收票类--联查凭证
	 * @throws Exception
	 */
	private void queryaccountbtn() throws Exception {
		// 联查凭证
		super.linkQueryPf();
	}
	
	/**
	 * 收票类--取消传凭证
	 * @throws Exception
	 */
	private void unaccountbtn() throws Exception {
		IJzinvMaintain jzinvMaintain= NCLocator.getInstance().lookup(IJzinvMaintain.class);
		// 取得界面主键
		String tempPKreceive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		// 取得当前打开表体的aggVO
		String pk_receive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		String[] obj = new String[] { AggReceiveVO.class.getName(), ReceiveVO.class.getName(), ReceiveBVO.class.getName(),
				ReceiveDetailVO.class.getName() };
		AggregatedValueObject billVo = JZProxy.getIUifService().queryBillVOByPrimaryKey(obj, pk_receive);
		// 取得单据类型
		String billtype = getClientUI().getBillCardPanel().getBillType();
		// 取得界面对应数据【发票认证公司】
		String pkfinance = getClientUI().getHeadItemString(ReceiveVO.PK_FINANCE);
		// 主表VO
		ReceiveVO receiveVO = (ReceiveVO) billVo.getParentVO();
		receiveVO.setPk_corp(pkfinance);
		receiveVO.setBisupload(new UFBoolean(true));
		billVo.setParentVO(receiveVO);
		// 根据数据主键，取得凭证数据，然后取消传凭证
		jzinvMaintain.unaccountplatfrom(tempPKreceive,billVo,billtype);
		// 更新界面【认证已传凭证】
		getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.BISACCOUNTFLG, new UFBoolean(false));
		// 界面按钮的更新
		getClientUI().updateCheckBtnenble();
		super.onBoRefresh();
	}
	
	/**
	 * 收票类--传会计平台
	 * @throws Exception
	 */
	private void accountbtn() throws Exception {
		IJzinvMaintain jzinvMaintain= NCLocator.getInstance().lookup(IJzinvMaintain.class);
		// 取得当前打开表体的aggVO
		String[] obj = new String[] { AggReceiveVO.class.getName(), ReceiveVO.class.getName(), ReceiveBVO.class.getName(),
				ReceiveDetailVO.class.getName() };
		String pk_receive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		AggregatedValueObject billVo = JZProxy.getIUifService().queryBillVOByPrimaryKey(obj, pk_receive);
		// 取得界面对应数据【发票认证公司】
		String pkfinance = getClientUI().getHeadItemString(ReceiveVO.PK_FINANCE);
		// 取得单据类型
		String billtype = getClientUI().getBillCardPanel().getBillType();
		// 主表VO
		ReceiveVO receiveVO = (ReceiveVO) billVo.getParentVO();
		receiveVO.setPk_corp(pkfinance);
		// 根据公司和单价类型
		boolean isupload = InvParamTool.isToGLByBilltype(_getCorp().getPk_corp(), billtype);
		if(isupload){
			receiveVO.setBisupload(new UFBoolean(true));
		}else{
			MessageDialog.showHintDlg(getClientUI(), "提示", "发票认证在【参数设置】中不可传凭证!");
			return;
		}
		billVo.setParentVO(receiveVO);
		// 调用后台程序数据【传会计平台】
		jzinvMaintain.accountplatfrom(billtype,billVo);
		// 更新界面【认证已传凭证】
		getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.BISACCOUNTFLG, new UFBoolean(true));
		// 界面按钮的更新
		getClientUI().updateCheckBtnenble();
		super.onBoRefresh();
	}
	
	private void addInfo() throws Exception {
		getClientUI().setBillOperate(IBillOperate.OP_EDIT);
		setBodyUnEnable();
		BillItem[] headItems = getBillCardPanelWrapper().getBillCardPanel().getHeadItems();
		ArrayList<String> editItems = getEditItemWhenInfoAdd();
		for (int i = 0; i < headItems.length; i++) {
			if(editItems.contains(headItems[i].getKey())){
				headItems[i].setEnabled(true);
			}else{
				headItems[i].setEnabled(false);
			}
		}
		super.onBoCard();
	}
	protected void setBodyUnEnable() throws Exception {
		String[] tableCodes = new String[]{"jzinv_receive_b","jzinv_receive_detail"};
		for (String tableCode : tableCodes) {
			BillModel billModel = getBillCardPanel().getBillModel(tableCode);
			if (billModel != null) {
				billModel.setEnabled(false);
			}
		}
	}
	protected BillCardPanel getBillCardPanel() {
		return getBillCardPanelWrapper().getBillCardPanel();
	}

	private ArrayList<String> getEditItemWhenInfoAdd() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(ReceiveVO.VINVCODE);
		list.add(ReceiveVO.VINVNO);
		list.add(ReceiveVO.DOPENDATE);
		list.add(ReceiveVO.VTAXSUPPLIERNUMBER);
		list.add(ReceiveVO.VTAXPAYERNUMBER);
		list.add(ReceiveVO.VSECRET);
		return list;
	}

	private void viewReceiveImage() {
		String pk_image = null;
		if ( getClientUI().isListPanelSelected() ) {
			BillListPanel listPanel = getBillListPanelWrapper().getBillListPanel();
			int selectedRow = listPanel.getHeadTable().getSelectedRow();

			BillModel bm = listPanel.getHeadBillModel();
			pk_image = (String) bm.getValueAt(selectedRow,"pk_image");
		} else {
			pk_image = (String) getBillCardPanelWrapper().getBillCardPanel().getHeadItem("pk_image").getValueObject();
		}
		if(StringUtils.isBlank(pk_image)){
			MessageDialog.showHintDlg(getClientUI(), "提示", "该发票非影像采集数据!");
			return;
		}
		
		if (ocrAdapter != null) {
			ocrAdapter.showReceiveImage(getBillUI(), pk_image);
		}
		
	}
	
	
	private void fetchRegisterResult() throws Exception {
		adapter.fetchRegisterResult();
	}

	private void registerInvoice() throws Exception {
//		loadCacheAuthModel();
//		
//		AuthenInfoDialog authDlg = new AuthenInfoDialog(getClientUI(), authModel);
//		int rtnCode = authDlg.showModal();
//		if ( rtnCode == AuthenInfoDialog.RETURN_CODE_AUTHEN_CODE ) {
//			authModel = authDlg.getAuthModel();
//
//		}
//		checkDataBeforeAuth();
		System.out.println("-------------"+System.getProperty("java.library.path"));
		if(adapter == null){
			throw new BusinessException("请接入认证接口!");
		}else{
			adapter.registerInvoice(authModel);
		}
		onBoRefresh();
	}
	private void checkDataBeforeAuth() throws BusinessException {
		Object obj = getClientUI().getBillCardPanel().getHeadItem("vsecret").getValueObject();
		String vsecret;
		if(obj==null){
			vsecret="";
		}else{
			vsecret=obj.toString();
		}
		if(StringUtils.isBlank(vsecret)){
			throw new BusinessException("空白密文不能认证");
		}
	}

	private String getCacheAuthModelFilePath() {
		String clientCodeDir = System.getProperty("client.code.dir",
				System.getProperty("user.home"));
		String pk_user = getClientUI().getClientEnvironment().getUser()
				.getPrimaryKey();

		String authenCachePath = clientCodeDir + File.separator + "CACHE"
				+ File.separator + "receive_authen_cache" + File.separator
				+ pk_user + ".dat";

		return authenCachePath;
	}

	private void loadCacheAuthModel() {
		if (authModel == null) {

			File cacheFile = new File(getCacheAuthModelFilePath());

			if (cacheFile.exists()) {
				authModel = (AuthInfoModel) readObjFrFile(cacheFile);
			} else {
				authModel = new AuthInfoModel();
			}
		}
	}

	/**
	 * 将一个对象保存到文件中
	 * 
	 * @param path
	 * @param map
	 */
	public void saveAuthModelFile(AuthInfoModel authModel) {
		if (authModel == null) {
			return;
		}

		AuthInfoModel saveAuthModel = new AuthInfoModel();
		saveAuthModel.setUserCode(authModel.getUserCode());

		File cacheFile = new File(getCacheAuthModelFilePath());
		if (!cacheFile.getParentFile().exists()) {
			cacheFile.getParentFile().mkdirs();
		}

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(cacheFile);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(saveAuthModel); // 括号内参数为要保存java对象
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				oos.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 从文件中读取一个对象
	 * 
	 * @param path
	 * @return
	 */
	private Object readObjFrFile(File file) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		Object obj = null;

		if (!file.canRead()) {
			return null;
		}

		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			obj = ois.readObject();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ois.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return obj;
	}

	@Override
	protected void onBoCard() throws Exception {
		super.onBoCard();
		onBoRefresh();
	}

	@Override
	protected void onBoReturn() throws Exception {
		super.onBoReturn();
		onBoRefresh();
	}

	@Override
	protected void onBoQuery() throws Exception {

		StringBuffer strWhere = new StringBuffer();

		if (askForQueryCondition(strWhere) == false)
			return;// 用户放弃了查询
		strWhere.append(" and (jzinv_receive.vbillstatus = '1' )");// 增加单据状态为审批通过
		String pk_corp = getBillManageUI().getEnvironment().getCorporation()
				.getPrimaryKey();
		strWhere.append(" and (jzinv_receive.pk_finance = '" + pk_corp + "')");// 增加发票认证公司为当前公司
		strWhere.append(" and (jzinv_receive.iinvtype = 0)");
		SuperVO[] queryVos = queryHeadVOs(strWhere.toString());

		getBufferData().clear();
		// 增加数据到Buffer
		addDataToBuffer(queryVos);
		updateBuffer();
		

	}

	protected String getHeadCondition() {
		// 公司
		return null;
	}

	/**
	 * 认证按钮动作
	 */
	private void authen(int button) throws Exception {
		boolean isEnable = isButtonEnable(button);
		if (isEnable) {
			AggReceiveVO[] objs = null;
			if (isList()) {
				objs = (AggReceiveVO[]) getBillManageUI()
						.getBillListPanel()
						.getBillListData()
						.getBillSelectValueVOs(AggReceiveVO.class.getName(),
								ReceiveVO.class.getName(),
								ReceiveBVO.class.getName());
				if (null == objs || objs.length == 0) {
					throw new BusinessException("请选择要认证的数据");
				} else if (bisHaveAuthened(objs)) {
					// 请选择没有认证过的数据
					throw new BusinessException("请选择没有认证且没有传金税的数据");
				} else {
					//linan 20171028 如果是拆分的单据认证，需要判断所有单据都审批通过单据拆分金额是否满足税金之和=票面总税金，并且此时也不能有非审批通过态的单据存在
					for (AggReceiveVO aggReceiveVO : objs) {
						checkIssplitTaxOK(aggReceiveVO);
					}
				}
			} else {
				AggReceiveVO selectedData = (AggReceiveVO) getBillManageUI()
						.getBillCardPanel().getBillValueVO(
								AggReceiveVO.class.getName(),
								ReceiveVO.class.getName(),
								ReceiveBVO.class.getName());
				objs = new AggReceiveVO[1];
				objs[0] = selectedData;
				if (bisHaveAuthened(objs)) {
					// 请选择没有认证过的数据
					throw new BusinessException("请选择没有认证且没有传金税的数据");
				}
				//linan 20171028 如果是拆分的单据认证，需要判断所有单据都审批通过单据拆分金额是否满足税金之和=票面总税金，并且此时也不能有非审批通过态的单据存在
				else {
					checkIssplitTaxOK(selectedData);
				}
			}
			int result = dialog.showModal();
			if (result == UIDialog.ID_OK) {
				String pk_authenpsn = (String) dialog.getBillItemByCode(
						ReceiveVO.PK_AUTHENPSN).getValueObject();
				String dauthendate = (String) dialog.getBillItemByCode(
						ReceiveVO.DAUTHENDATE).getValueObject();
				String vtaxperiod = (String) dialog.getBillItemByCode(
						ReceiveVO.VTAXPERIOD).getValueObject();
				authenSelectedVOs(objs, pk_authenpsn, dauthendate, vtaxperiod);
				MessageDialog.showHintDlg(getClientUI(), "认证", "认证成功！");
			}
		} else {
			throw new BusinessException("请选择没有认证且没有传金税的数据");
		}
	}

	/**
	 * 取消认证按钮动作
	 */
	private void unauthen(int button) throws Exception {
		boolean isEnable = isButtonEnable(button);
		if (isEnable) {
			AggReceiveVO[] objs = null;
			if (isList()) {
				objs = (AggReceiveVO[]) getBillManageUI()
						.getBillListPanel()
						.getBillListData()
						.getBillSelectValueVOs(AggReceiveVO.class.getName(),
								ReceiveVO.class.getName(),
								ReceiveBVO.class.getName());
				if (null == objs || objs.length == 0) {
					throw new BusinessException("请选择要取消认证的数据");
				} else if (bisUnAuthen(objs)) {
					// 请选择认证过的数据
					throw new BusinessException("请选择没有传金税且发票认证过的数据");
				}
			} else {
				AggReceiveVO selectedData = (AggReceiveVO) getBillManageUI()
						.getBillCardPanel().getBillValueVO(
								AggReceiveVO.class.getName(),
								ReceiveVO.class.getName(),
								ReceiveBVO.class.getName());
				objs = new AggReceiveVO[1];
				objs[0] = selectedData;
			}
			unAuthenSelectedVOs(objs, button);
		} else {
			throw new BusinessException("请选择没有传金税且发票认证过的数据");
		}
	}

	private void authenSelectedVOs(Object[] objs, String pk_authenpsn,
			String dauthendate, String vtaxperiod) throws Exception {
		ReceiveVO[] hvos = new ReceiveVO[objs.length];
		//linan add 记录同一发票下的其他拆分单据
		List<ReceiveVO> receiveList = new ArrayList<ReceiveVO>(); 
		for (int i = 0; i < objs.length; i++) {
			ReceiveVO hvo = (ReceiveVO) ((HYBillVO) objs[i]).getParentVO();
			hvo.setAttributeValue(ReceiveVO.PK_AUTHENPSN, pk_authenpsn);
			hvo.setAttributeValue(ReceiveVO.DAUTHENDATE, new UFDate(dauthendate));
			hvo.setAttributeValue(ReceiveVO.VTAXPERIOD, vtaxperiod);
			hvo.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.TRUE);
			hvo.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_AUTHSUCESS);
			//把vo都放进list里
			receiveList.add(hvo);
			hvos[i] = hvo;
			//linan add 如果是发票拆分的情况就同时更新同一发票下的拆分单据
			UFBoolean bissplit = hvo.getBissplit();
			String pk_receive = hvo.getPk_receive();
			String vinvno = hvo.getVinvno();
			String vinvcode = hvo.getVinvcode();
			if (UFBoolean.TRUE.equals(bissplit)) {
				//找到拆分的其他单据
				List<ReceiveVO> receiveVOList = NCLocator.getInstance()
						.lookup(IReceiveService.class)
						.querySplitHeadVOsByCond(vinvcode, vinvno, pk_receive);
				if (receiveVOList != null && !receiveVOList.isEmpty()) {
					for (ReceiveVO receiveVO : receiveVOList) {
						receiveVO.setAttributeValue(ReceiveVO.PK_AUTHENPSN, pk_authenpsn);
						receiveVO.setAttributeValue(ReceiveVO.DAUTHENDATE, new UFDate(dauthendate));
						receiveVO.setAttributeValue(ReceiveVO.VTAXPERIOD, vtaxperiod);
						receiveVO.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.TRUE);
						receiveVO.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_AUTHSUCESS);
					}
					receiveList.addAll(receiveVOList);
				}
				
			}
			
			
		}
		CircularlyAccessibleValueObject[] vos =getBufferData().getAllHeadVOsFromBuffer();
		if(vos.length>0){
			for(int i=0;i<vos.length;i++){
				CircularlyAccessibleValueObject vo = vos[i];
				for(int j = 0; j < objs.length; j++){
					ReceiveVO hvo = (ReceiveVO) ((HYBillVO) objs[j]).getParentVO();
					if(vo.getPrimaryKey().equals(hvo.getPrimaryKey())){
						vo.setAttributeValue(ReceiveVO.PK_AUTHENPSN, pk_authenpsn);
						vo.setAttributeValue(ReceiveVO.DAUTHENDATE, new UFDate(dauthendate));
						vo.setAttributeValue(ReceiveVO.VTAXPERIOD, vtaxperiod);
						vo.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.TRUE);
						vo.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_AUTHSUCESS);
					}
				}
			}
		}
		//linan注释 改为从list里边读取
		//updateSelectedVOs(hvos);
		ReceiveVO[] hvosAll = new ReceiveVO[receiveList.size()]; 
		updateSelectedVOs(receiveList.toArray(hvosAll));
		updateBatchSelectRows(receiveList.toArray(hvosAll));
	}

	private void unAuthenSelectedVOs(Object[] objs, int button)
			throws Exception {
		ReceiveVO[] hvos = new ReceiveVO[objs.length];
		//linan add 记录同一发票下的其他拆分单据
		List<ReceiveVO> receiveList = new ArrayList<ReceiveVO>(); 
		for (int i = 0; i < objs.length; i++) {
			ReceiveVO hvo = (ReceiveVO) ((HYBillVO) objs[i]).getParentVO();
			hvo.setAttributeValue(ReceiveVO.PK_AUTHENPSN, null);
			hvo.setAttributeValue(ReceiveVO.DAUTHENDATE, null);
			hvo.setAttributeValue(ReceiveVO.VTAXPERIOD, null);
			hvo.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.FALSE);
			hvo.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_NOAUTH);
			hvos[i] = hvo;
			//把vo都放进list里
			receiveList.add(hvo);
			//linan add 如果是发票拆分的情况就同时更新同一发票下的拆分单据
			UFBoolean bissplit = hvo.getBissplit();
			String pk_receive = hvo.getPk_receive();
			String vinvno = hvo.getVinvno();
			String vinvcode = hvo.getVinvcode();
			if (UFBoolean.TRUE.equals(bissplit)) {
				//找到拆分的其他单据
				List<ReceiveVO> receiveVOList = NCLocator.getInstance()
						.lookup(IReceiveService.class)
						.querySplitHeadVOsByCond(vinvcode, vinvno, pk_receive);
				if (receiveVOList != null && !receiveVOList.isEmpty()) {
					for (ReceiveVO receiveVO : receiveVOList) {
						receiveVO.setAttributeValue(ReceiveVO.PK_AUTHENPSN, null);
						receiveVO.setAttributeValue(ReceiveVO.DAUTHENDATE, null);
						receiveVO.setAttributeValue(ReceiveVO.VTAXPERIOD, null);
						receiveVO.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.FALSE);
						receiveVO.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_NOAUTH);
					}
					receiveList.addAll(receiveVOList);
				}
				
			}
		}
		CircularlyAccessibleValueObject[] vos =getBufferData().getAllHeadVOsFromBuffer();
		if(vos.length>0){
			for(int i=0;i<vos.length;i++){
				CircularlyAccessibleValueObject vo = vos[i];
				for(int j = 0; j < objs.length; j++){
					ReceiveVO hvo = (ReceiveVO) ((HYBillVO) objs[j]).getParentVO();
					if(vo.getPrimaryKey().equals(hvo.getPrimaryKey())){
						vo.setAttributeValue(ReceiveVO.PK_AUTHENPSN, null);
						vo.setAttributeValue(ReceiveVO.DAUTHENDATE, null);
						vo.setAttributeValue(ReceiveVO.VTAXPERIOD, null);
						vo.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.FALSE);
						vo.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_NOAUTH);
					}
				}
			}
		}
		//updateSelectedVOs(hvos);
		//linan注释 改为从list里边读取
		//updateSelectedVOs(hvos);
		ReceiveVO[] hvosAll = new ReceiveVO[receiveList.size()]; 
		updateSelectedVOs(receiveList.toArray(hvosAll));
		updateBatchSelectRows(receiveList.toArray(hvosAll));
	}
	/**
	 * 批量处理所选数据
	 * 
	 * @param hvos
	 * @throws Exception
	 */
	private void batchSetSelectVOs(ReceiveVO[] hvos) throws Exception {
		for (int i = 0; i < getBillManageUI().getBillListPanel()
				.getHeadBillModel().getRowCount(); i++) {
			if (getBillManageUI().getBillListPanel().getHeadBillModel()
					.getRowState(i) == BillModel.SELECTED) {
				AggReceiveVO billVO = (AggReceiveVO) getBillManageUI()
						.getBillListPanel().getBillValueVO(i,
								AggReceiveVO.class.getName(),
								ReceiveVO.class.getName(),
								ReceiveBVO.class.getName());
				selecRowMap.put(billVO.getParentVO().getPrimaryKey(), i);
			}
		}
		for (ReceiveVO hvo : hvos) {
			if (selecRowMap.containsKey(hvo.getPrimaryKey())) {
				int i = selecRowMap.get(hvo.getPrimaryKey());
				getBillManageUI().getBillListWrapper().updateListVo(hvo, i);
			}
		}
	}
	/**
	 * 批量处理所选数据
	 * 
	 * @param hvos
	 * @throws Exception
	 */
	private void batchSetSelectVOsForImport(ReceiveVO[] hvos) throws Exception {
		for (int i = 0; i < getBillManageUI().getBillListPanel()
				.getHeadBillModel().getRowCount(); i++) {
//			if (getBillManageUI().getBillListPanel().getHeadBillModel()
//					.getRowState(i) == BillModel.SELECTED) {
//				AggReceiveVO billVO = (AggReceiveVO) getBillManageUI()
//						.getBillListPanel().getBillValueVO(i,
//								AggReceiveVO.class.getName(),
//								ReceiveVO.class.getName(),
//								ReceiveBVO.class.getName());
//				selecRowMap.put(billVO.getParentVO().getPrimaryKey(), i);
//			}
			AggReceiveVO billVO = (AggReceiveVO) getBillManageUI()
					.getBillListPanel().getBillValueVO(i,
							AggReceiveVO.class.getName(),
							ReceiveVO.class.getName(),
							ReceiveBVO.class.getName());
			selecRowMap.put(billVO.getParentVO().getPrimaryKey(), i);
		}
		for (ReceiveVO hvo : hvos) {
			if (selecRowMap.containsKey(hvo.getPrimaryKey())) {
				int i = selecRowMap.get(hvo.getPrimaryKey());
				getBillManageUI().getBillListWrapper().updateListVo(hvo, i);
			}
		}
	}

	/**
	 * 更新所选数据
	 * 
	 * @param hvos
	 * @throws Exception
	 */
	private void updateSelectedVOs(ReceiveVO[] hvos) throws Exception {
		boolean existNoAproove = bisExistNoApprovePass(hvos);
		if (existNoAproove) {
			throw new BusinessException("存在非审批通过的数据, 请重新查询数据!");
		}
		IReceiveService service = (IReceiveService) NCLocator.getInstance()
				.lookup(IReceiveService.class.getName());
		service.updateSelectVOs(hvos);
	}

	/**
	 * 是否已经认证
	 * 
	 * @param rows
	 * @return
	 */
	public boolean bisHaveAuthened(Object[] objs) {
		for (int i = 0; i < objs.length; i++) {
			SuperVO hvo = (SuperVO) ((HYBillVO) objs[i]).getParentVO();
			UFBoolean bisauthen = (UFBoolean) hvo
					.getAttributeValue(ReceiveVO.BISAUTHEN);
			UFBoolean bisTranferTax = (UFBoolean) hvo
					.getAttributeValue(ReceiveVO.BISTRANSFERTAX);
			if (bisauthen.booleanValue() || bisTranferTax.booleanValue()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否没有认证
	 * 
	 * @param rows
	 * @return
	 */
	public boolean bisUnAuthen(Object[] objs) {
		for (int i = 0; i < objs.length; i++) {
			SuperVO hvo = (SuperVO) ((HYBillVO) objs[i]).getParentVO();
			UFBoolean bisauthen = (UFBoolean) hvo
					.getAttributeValue(ReceiveVO.BISAUTHEN);
			UFBoolean bisTranferTax = (UFBoolean) hvo
					.getAttributeValue(ReceiveVO.BISTRANSFERTAX);
			if (!bisauthen.booleanValue() && !bisTranferTax.booleanValue()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 按钮是否可用
	 */
	protected boolean isButtonEnable(int button) {
		boolean isList = getBillManageUI().isListPanelSelected();
		if (isList) {
			// 列表下判断
			Object[] objs = (Object[]) getBillManageUI()
					.getBillListPanel()
					.getBillListData()
					.getBillSelectValueVOs(AggReceiveVO.class.getName(),
							ReceiveVO.class.getName(),
							ReceiveBVO.class.getName());
			if (objs != null && objs.length > 1) {
				return true;
			}
			if (objs != null && objs.length == 0) {
				return false;
			}
			AggregatedValueObject selectedData = (AggregatedValueObject) getBillManageUI()
					.getBillListPanel()
					.getBillListData()
					.getBillSelectValueVO(AggReceiveVO.class.getName(),
							ReceiveVO.class.getName(),
							ReceiveBVO.class.getName());
			if (selectedData != null) {
				SuperVO hvo = (SuperVO) selectedData.getParentVO();
				UFBoolean bisAuthen = (UFBoolean) hvo
						.getAttributeValue(ReceiveVO.BISAUTHEN);
				if ((button == IJzinvButton.AuthenBtn && bisAuthen
						.booleanValue())
						|| (button == IJzinvButton.UnauthenBtn && !bisAuthen
								.booleanValue())) {
					return false;
				}
				return true;
			}
		} else {
			String bisAuthen = (String) getBillManageUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.BISAUTHEN).getValueObject();
			String pk_primary = (String) getBillManageUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_RECEIVE).getValueObject();
			if (StringUtils.isEmpty(pk_primary)) {
				return false;
			} else if ((button == IJzinvButton.AuthenBtn && "true"
					.equals(bisAuthen))
					|| (button == IJzinvButton.UnauthenBtn && !"true"
							.equals(bisAuthen))) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * 批量更新所选行
	 * 
	 * @param hvos
	 * @throws Exception
	 */
	private void updateBatchSelectRows(ReceiveVO[] hvos) throws Exception {
		if (isList()) {
			batchSetSelectVOs(hvos);
			super.onBoRefresh();
		} else {
			super.onBoRefresh();
		}
	}
	/**
	 * 批量更新所选行
	 * 
	 * @param hvos
	 * @throws Exception
	 */
	private void updateBatchSelectRowsForImport(ReceiveVO[] hvos) throws Exception {
		if (isList()) {
			batchSetSelectVOsForImport(hvos);
			super.onBoRefresh();
		} else {
			super.onBoRefresh();
		}
	}

	/**
	 * 判断是否存在非审批通过的数据
	 * 
	 * @param objs
	 * @return
	 */
	private boolean bisExistNoApprovePass(ReceiveVO[] hvos) {
		for (int i = 0; i < hvos.length; i++) {
			Integer fstatusflag = (Integer) hvos[i]
					.getAttributeValue(ReceiveVO.VBILLSTATUS);
			if (fstatusflag != 1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否列表下
	 * 
	 * @return
	 */
	private boolean isList() {
		return getBillManageUI().isListPanelSelected();
	}

	/**
	 * 更新列表数据
	 * 
	 * @param pkList
	 * @throws ClassNotFoundException
	 * @throws Exception
	 */
	public void updateVOByPks(List<String> pkList)
			throws ClassNotFoundException, Exception {
		if (pkList.isEmpty()) {
			return;
		}
		StringBuffer whereSql = new StringBuffer();
		String pkfiled = this.getBillManageUI().getUIControl().getPkField();
		whereSql.append(pkfiled);
		whereSql.append(" in ");
		whereSql.append(InSqlManager.getInSQLValue(pkList));

		SuperVO[] queryVos = queryHeadVOs(whereSql.toString());
		getBufferData().clear();
		addDataToBuffer(queryVos);
		updateBuffer();
	}

	public void updateClientData(List<ReceiveVO> voList) throws Exception {
		getBufferData().clear();
		addDataToBuffer(voList.toArray(new ReceiveVO[0]));
		updateBuffer();
	}
	
	
	private void onBoExcelImport() throws Exception {
		if(beforeImport()){
			NCExcelImportAdapter<? extends SuperVO> excelImporter = null;
			excelImporter = new NCExcelImportAdapter<InvExcelImportVO>(
			        getBillManageUI(), 
			        InvExcelImportVO.EXCEL_CFG_FACTROY.getExcelImportCfg(),
			        InvExcelImportVO.class);
			onImport(excelImporter);
		}
	}
	
	/**
	 * 支持多页签打印
	 */
	public boolean isEnableMultiTablePRT() {
		return true;
	}
	
	/**
	 * 导入前弹框，含 认证人，认证日期，纳税期
	 * TODO
	 * sujbc2016-7-4上午6:47:45
	 */
	private boolean beforeImport(){
		int result = dialog.showModal();
		if (result == UIDialog.ID_OK) {
			String pk_authenpsn = (String) dialog.getBillItemByCode(
					ReceiveVO.PK_AUTHENPSN).getValueObject();
			String dauthendate = (String) dialog.getBillItemByCode(
					ReceiveVO.DAUTHENDATE).getValueObject();
			String vtaxperiod = (String) dialog.getBillItemByCode(
					ReceiveVO.VTAXPERIOD).getValueObject();
			toImportNeedMap.clear();
			toImportNeedMap.put(ReceiveVO.PK_AUTHENPSN,pk_authenpsn);
			toImportNeedMap.put(ReceiveVO.DAUTHENDATE,dauthendate);
			toImportNeedMap.put(ReceiveVO.VTAXPERIOD,vtaxperiod);
			return true;
		}
		return false;
	}
	
	private void onImport(final NCExcelImportAdapter<? extends SuperVO> excelImporter) throws Exception {
		if (UIDialog.ID_OK == excelImporter.showMode()) {
			List<? extends SuperVO> bodyVOList = ProcessTool.process(new IProcessable<List<? extends SuperVO>>() {
				public List<? extends SuperVO> process() throws Exception {
					List<? extends SuperVO> vos = excelImporter.getImportData();
					return vos;
				}

				public Container getParent() {
					return getBillManageUI();
				}

				public String getWaitTitle() {
					return "提示";
				}

				public String getWaitContent() {
					return "正在导入，请稍后.....";
				}
			});
			List<ReceiveVO> receiveVOs = null;
			if (bodyVOList != null && !bodyVOList.isEmpty()) {
				String str = NCLocator.getInstance().lookup(IInvAuthenSevice.class).updateImportData(bodyVOList);
				if(str != null){
					if(MessageDialog.showYesNoDlg(getClientUI(), null, str + ",是否取消认证？") == MessageDialog.ID_YES){
						receiveVOs = NCLocator.getInstance().lookup(IInvAuthenSevice.class).updateImportDataImpl(bodyVOList,toImportNeedMap,true);
					}else{
						receiveVOs = NCLocator.getInstance().lookup(IInvAuthenSevice.class).updateImportDataImpl(bodyVOList,toImportNeedMap,false);
					}
				}else{
					receiveVOs = NCLocator.getInstance().lookup(IInvAuthenSevice.class).updateImportDataImpl(bodyVOList,toImportNeedMap,true);
				};
				if(null != receiveVOs){
					this.updateBatchSelectRowsForImport(receiveVOs.toArray(new ReceiveVO[0]));
				}
			}
		}
	}

	/** 
	* @Title: checkIssplitTaxOK 
	* @Description: 判断与本单据属于同一发票的拆分单据是否审批通过并且所有拆分单据的税金之和=票面总税金
	* @param     
	* @return void    
	* @throws 
	*/
	private void checkIssplitTaxOK(AggReceiveVO aggReceiveVO) throws BusinessException {
		ReceiveVO receiveVO = (ReceiveVO) aggReceiveVO.getParentVO();
		UFBoolean bissplit = receiveVO.getBissplit();
		String pk_receive = receiveVO.getPk_receive();
		String vinvno = receiveVO.getVinvno();
		String vinvcode = receiveVO.getVinvcode();
		String vbillno = receiveVO.getVbillno();
		//本张单据的税金
		UFDouble ntaxmny = receiveVO.getNtaxmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtaxmny();						
		/*if (ntaxmny == null) {
			throw new BusinessException("单据编号" + receiveVO.getVbillno() + "单据税额为空！");
		}*/
		//票面总税金
		UFDouble ntotalinvoicetax = receiveVO.getNtotalinvoicetax();
		//判断是否选中，选中则进行判断
		if (bissplit != null && UFBoolean.TRUE.equals(bissplit)) {
			//除了本张单据之外相同发票拆分的其他单据的税金之和
			UFDouble othSumTax = UFDouble.ZERO_DBL;
			//取到同一个发票拆分的所有单据，不包括自己
			List<ReceiveVO> receiveVOList = NCLocator.getInstance()
					.lookup(IReceiveService.class)
					.querySplitHeadVOsByCond(vinvcode, vinvno, pk_receive);
			if (receiveVOList != null && !receiveVOList.isEmpty()) {
				for (ReceiveVO othReceiveVO : receiveVOList) {
					if (IBillStatus.CHECKPASS != othReceiveVO.getVbillstatus()) {
						//因为认证需要更新同一发票拆分的其他单据
						throw new BusinessException("与单据编号为" + vbillno + "的单据属于同一发票的拆分单据 " 
								+ othReceiveVO.getVbillno() + "没有审批通过，所以本单据不能认证！");
					}
					//累计其他单据的金额
					othSumTax = othSumTax.add(othReceiveVO.getNtaxmny());
				}
			}
			//判断总税额是否相等
			//所有单据的税金合计
			UFDouble sumTax = othSumTax.add(ntaxmny);
			if (!sumTax.equals(ntotalinvoicetax)) {
				throw new BusinessException("单据编号为" + vbillno + "的单据所属的发票的票面总税金不等于所有拆分单据税金之和，不能认证！");
			}	
		}		
	}
	
	
}
