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
 * ��Ʊ��֤��ť�Ĵ���
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
		//��Ʊ����
		String vinvcode = getHeadItemValue(ReceiveVO.VINVCODE).toString();
		//��Ʊ����
		String vinvno = getHeadItemValue(ReceiveVO.VINVNO).toString();
		if(!vinvno.matches("^[0-9]+$")){
			MessageDialog.showHintDlg(getClientUI(), "��ʾ", "��Ʊ�ű���Ϊ���֣�");
			return false;
		}
		vinvno=String.format("%08d",Long.valueOf(vinvno));
		getClientUI().getBillCardPanel().setHeadItem(ReceiveVO.VINVNO, vinvno);
		String pk_receicve = getHeadItemValue(ReceiveVO.PK_RECEIVE).toString();
		SuperVO[] vos = JZINVProxy.getJZUifService().queryByConditionAtJZ(ReceiveVO.class, 
				" dr=0 and vinvcode='"+vinvcode+"' and vinvno='"+vinvno+"' and pk_receive <>'"+pk_receicve+"'");
		if(vos.length>0){
			MessageDialog.showHintDlg(getClientUI(), "��ʾ", "��Ʊ����+��Ʊ���Ѵ��ڣ�");
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
		//��ѡ
		case IJzinvButton.CHECKAUTH:
			checkAuthBtn();
			break;
		//������ѡ
		case IJzinvButton.UNCHECKAUTH:
			uncheckAuthBtn();
			break;
		//��ѡ����֤
		case IJzinvButton.CHECKCONFIRMAUTH:
			checkConfrimAuthBtn();
			break;
		//ˢ�¹�ѡ��֤״̬
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
			throw new BusinessException("��ѡ����Ҫˢ����֤״̬�ĵ���");
		for(AggReceiveVO aggReceiveVO : receiveVOs){
			ReceiveVO receiveVO = (ReceiveVO)aggReceiveVO.getParentVO();
			UFBoolean bisselected = receiveVO.getBisselected() == null ? UFBoolean.FALSE : receiveVO.getBisselected();
			if(!bisselected.booleanValue()){
				throw new BusinessException("����δ��ѡ��֤�ĵ��ݣ��޷�������֤״̬���");
			}
		}
		
		if(checkRefAdapter == null){
			throw new BusinessException("�������֤�ӿ�!");
		}else{
			checkRefAdapter.uploadInvoiceAuthRef();
		}
		onBoRefresh();
	}

	/**
	 * @throws Exception 
	 * @Description: ��ѡ������¼��������ѹ�ѡ������Ϊ����ѡ��
	 * @Title: checkAuthBtn
	 */
	private void checkAuthBtn() throws Exception{
		setBillPanelBisselected(UFBoolean.TRUE);
	}
	
	/**
	 * @throws Exception 
	 * @Description: ������ѡ������¼���������ѡ������Ϊ������ѡ��
	 * @Title: checkAuthBtn
	 */
	private void uncheckAuthBtn() throws Exception{
		setBillPanelBisselected(UFBoolean.FALSE);
	}
	/**
	 * @Description: ��ѡ��ȷ�ϰ�ť����¼���������ѡ������Ϊ������ѡ������pyt�ӿ�
	 * @Title: checkConfrimAuthBtn  
	 * @throws Exception
	 */
	private void checkConfrimAuthBtn() throws Exception{
		setBillPanelBisselected(UFBoolean.TRUE);
		invUploadAdapter = InvoiceAuthenUploadCallFactory.getInvUploadCaller(getClientUI());
		if(invUploadAdapter == null){
			throw new BusinessException("����빴ѡ��֤�ӿ�!");
		}else{
			if(invUploadAdapter instanceof BwCheckAuthenCallAdapter){
				//���� IJzinvTemplateConsts JZINV_AUTHEN
				AuthenDlg dlg = new AuthenDlg(getClientUI());
				if (dlg.showModal() == UIDialog.ID_OK) {
					String auther = (String) dlg.getValueByCode(ReceiveVO.PK_AUTHENPSN);//��֤��
					String authdate = (String) dlg.getValueByCode(ReceiveVO.DAUTHENDATE);//��Ŀ����	dauthendate
					String taxperiod = (String) dlg.getValueByCode(ReceiveVO.VTAXPERIOD);//��Ŀ����	vtaxperiod
					invUploadAdapter.uploadInvoiceAuth(auther, authdate, taxperiod);
				}
			}else{
				invUploadAdapter.uploadInvoiceAuth(null, null, null);
			}
			
		}
		
		onBoRefresh();
	}
	
	
	/**
	 * @Description: ���ÿ�Ƭ���б����ġ��ѹ�ѡ���ֶε�ֵ
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
	 * ��Ʊ��--����ƾ֤
	 * @throws Exception
	 */
	private void queryaccountbtn() throws Exception {
		// ����ƾ֤
		super.linkQueryPf();
	}
	
	/**
	 * ��Ʊ��--ȡ����ƾ֤
	 * @throws Exception
	 */
	private void unaccountbtn() throws Exception {
		IJzinvMaintain jzinvMaintain= NCLocator.getInstance().lookup(IJzinvMaintain.class);
		// ȡ�ý�������
		String tempPKreceive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		// ȡ�õ�ǰ�򿪱����aggVO
		String pk_receive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		String[] obj = new String[] { AggReceiveVO.class.getName(), ReceiveVO.class.getName(), ReceiveBVO.class.getName(),
				ReceiveDetailVO.class.getName() };
		AggregatedValueObject billVo = JZProxy.getIUifService().queryBillVOByPrimaryKey(obj, pk_receive);
		// ȡ�õ�������
		String billtype = getClientUI().getBillCardPanel().getBillType();
		// ȡ�ý����Ӧ���ݡ���Ʊ��֤��˾��
		String pkfinance = getClientUI().getHeadItemString(ReceiveVO.PK_FINANCE);
		// ����VO
		ReceiveVO receiveVO = (ReceiveVO) billVo.getParentVO();
		receiveVO.setPk_corp(pkfinance);
		receiveVO.setBisupload(new UFBoolean(true));
		billVo.setParentVO(receiveVO);
		// ��������������ȡ��ƾ֤���ݣ�Ȼ��ȡ����ƾ֤
		jzinvMaintain.unaccountplatfrom(tempPKreceive,billVo,billtype);
		// ���½��桾��֤�Ѵ�ƾ֤��
		getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.BISACCOUNTFLG, new UFBoolean(false));
		// ���水ť�ĸ���
		getClientUI().updateCheckBtnenble();
		super.onBoRefresh();
	}
	
	/**
	 * ��Ʊ��--�����ƽ̨
	 * @throws Exception
	 */
	private void accountbtn() throws Exception {
		IJzinvMaintain jzinvMaintain= NCLocator.getInstance().lookup(IJzinvMaintain.class);
		// ȡ�õ�ǰ�򿪱����aggVO
		String[] obj = new String[] { AggReceiveVO.class.getName(), ReceiveVO.class.getName(), ReceiveBVO.class.getName(),
				ReceiveDetailVO.class.getName() };
		String pk_receive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		AggregatedValueObject billVo = JZProxy.getIUifService().queryBillVOByPrimaryKey(obj, pk_receive);
		// ȡ�ý����Ӧ���ݡ���Ʊ��֤��˾��
		String pkfinance = getClientUI().getHeadItemString(ReceiveVO.PK_FINANCE);
		// ȡ�õ�������
		String billtype = getClientUI().getBillCardPanel().getBillType();
		// ����VO
		ReceiveVO receiveVO = (ReceiveVO) billVo.getParentVO();
		receiveVO.setPk_corp(pkfinance);
		// ���ݹ�˾�͵�������
		boolean isupload = InvParamTool.isToGLByBilltype(_getCorp().getPk_corp(), billtype);
		if(isupload){
			receiveVO.setBisupload(new UFBoolean(true));
		}else{
			MessageDialog.showHintDlg(getClientUI(), "��ʾ", "��Ʊ��֤�ڡ��������á��в��ɴ�ƾ֤!");
			return;
		}
		billVo.setParentVO(receiveVO);
		// ���ú�̨�������ݡ������ƽ̨��
		jzinvMaintain.accountplatfrom(billtype,billVo);
		// ���½��桾��֤�Ѵ�ƾ֤��
		getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.BISACCOUNTFLG, new UFBoolean(true));
		// ���水ť�ĸ���
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
			MessageDialog.showHintDlg(getClientUI(), "��ʾ", "�÷�Ʊ��Ӱ��ɼ�����!");
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
			throw new BusinessException("�������֤�ӿ�!");
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
			throw new BusinessException("�հ����Ĳ�����֤");
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
	 * ��һ�����󱣴浽�ļ���
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
			oos.writeObject(saveAuthModel); // �����ڲ���ΪҪ����java����
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
	 * ���ļ��ж�ȡһ������
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
			return;// �û������˲�ѯ
		strWhere.append(" and (jzinv_receive.vbillstatus = '1' )");// ���ӵ���״̬Ϊ����ͨ��
		String pk_corp = getBillManageUI().getEnvironment().getCorporation()
				.getPrimaryKey();
		strWhere.append(" and (jzinv_receive.pk_finance = '" + pk_corp + "')");// ���ӷ�Ʊ��֤��˾Ϊ��ǰ��˾
		strWhere.append(" and (jzinv_receive.iinvtype = 0)");
		SuperVO[] queryVos = queryHeadVOs(strWhere.toString());

		getBufferData().clear();
		// �������ݵ�Buffer
		addDataToBuffer(queryVos);
		updateBuffer();
		

	}

	protected String getHeadCondition() {
		// ��˾
		return null;
	}

	/**
	 * ��֤��ť����
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
					throw new BusinessException("��ѡ��Ҫ��֤������");
				} else if (bisHaveAuthened(objs)) {
					// ��ѡ��û����֤��������
					throw new BusinessException("��ѡ��û����֤��û�д���˰������");
				} else {
					//linan 20171028 ����ǲ�ֵĵ�����֤����Ҫ�ж����е��ݶ�����ͨ�����ݲ�ֽ���Ƿ�����˰��֮��=Ʊ����˰�𣬲��Ҵ�ʱҲ�����з�����ͨ��̬�ĵ��ݴ���
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
					// ��ѡ��û����֤��������
					throw new BusinessException("��ѡ��û����֤��û�д���˰������");
				}
				//linan 20171028 ����ǲ�ֵĵ�����֤����Ҫ�ж����е��ݶ�����ͨ�����ݲ�ֽ���Ƿ�����˰��֮��=Ʊ����˰�𣬲��Ҵ�ʱҲ�����з�����ͨ��̬�ĵ��ݴ���
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
				MessageDialog.showHintDlg(getClientUI(), "��֤", "��֤�ɹ���");
			}
		} else {
			throw new BusinessException("��ѡ��û����֤��û�д���˰������");
		}
	}

	/**
	 * ȡ����֤��ť����
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
					throw new BusinessException("��ѡ��Ҫȡ����֤������");
				} else if (bisUnAuthen(objs)) {
					// ��ѡ����֤��������
					throw new BusinessException("��ѡ��û�д���˰�ҷ�Ʊ��֤��������");
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
			throw new BusinessException("��ѡ��û�д���˰�ҷ�Ʊ��֤��������");
		}
	}

	private void authenSelectedVOs(Object[] objs, String pk_authenpsn,
			String dauthendate, String vtaxperiod) throws Exception {
		ReceiveVO[] hvos = new ReceiveVO[objs.length];
		for (int i = 0; i < objs.length; i++) {
			ReceiveVO hvo = (ReceiveVO) ((HYBillVO) objs[i]).getParentVO();
			hvo.setAttributeValue(ReceiveVO.PK_AUTHENPSN, pk_authenpsn);
			hvo.setAttributeValue(ReceiveVO.DAUTHENDATE, new UFDate(dauthendate));
			hvo.setAttributeValue(ReceiveVO.VTAXPERIOD, vtaxperiod);
			hvo.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.TRUE);
			hvo.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_AUTHSUCESS);
			hvos[i] = hvo;
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
		updateSelectedVOs(hvos);
		updateBatchSelectRows(hvos);
	}

	private void unAuthenSelectedVOs(Object[] objs, int button)
			throws Exception {
		ReceiveVO[] hvos = new ReceiveVO[objs.length];
		for (int i = 0; i < objs.length; i++) {
			ReceiveVO hvo = (ReceiveVO) ((HYBillVO) objs[i]).getParentVO();
			hvo.setAttributeValue(ReceiveVO.PK_AUTHENPSN, null);
			hvo.setAttributeValue(ReceiveVO.DAUTHENDATE, null);
			hvo.setAttributeValue(ReceiveVO.VTAXPERIOD, null);
			hvo.setAttributeValue(ReceiveVO.BISAUTHEN, UFBoolean.FALSE);
			hvo.setAttributeValue(ReceiveVO.IAUTHSTATUS, InvConsts.AUTHSTATUS_NOAUTH);
			hvos[i] = hvo;
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
		updateSelectedVOs(hvos);
		updateBatchSelectRows(hvos);
	}
	/**
	 * ����������ѡ����
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
	 * ����������ѡ����
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
	 * ������ѡ����
	 * 
	 * @param hvos
	 * @throws Exception
	 */
	private void updateSelectedVOs(ReceiveVO[] hvos) throws Exception {
		boolean existNoAproove = bisExistNoApprovePass(hvos);
		if (existNoAproove) {
			throw new BusinessException("���ڷ�����ͨ��������, �����²�ѯ����!");
		}
		IReceiveService service = (IReceiveService) NCLocator.getInstance()
				.lookup(IReceiveService.class.getName());
		service.updateSelectVOs(hvos);
	}

	/**
	 * �Ƿ��Ѿ���֤
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
	 * �Ƿ�û����֤
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
	 * ��ť�Ƿ����
	 */
	protected boolean isButtonEnable(int button) {
		boolean isList = getBillManageUI().isListPanelSelected();
		if (isList) {
			// �б����ж�
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
	 * ����������ѡ��
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
	 * ����������ѡ��
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
	 * �ж��Ƿ���ڷ�����ͨ��������
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
	 * �Ƿ��б���
	 * 
	 * @return
	 */
	private boolean isList() {
		return getBillManageUI().isListPanelSelected();
	}

	/**
	 * �����б�����
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
	 * ֧�ֶ�ҳǩ��ӡ
	 */
	public boolean isEnableMultiTablePRT() {
		return true;
	}
	
	/**
	 * ����ǰ���򣬺� ��֤�ˣ���֤���ڣ���˰��
	 * TODO
	 * sujbc2016-7-4����6:47:45
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
					return "��ʾ";
				}

				public String getWaitContent() {
					return "���ڵ��룬���Ժ�.....";
				}
			});
			List<ReceiveVO> receiveVOs = null;
			if (bodyVOList != null && !bodyVOList.isEmpty()) {
				String str = NCLocator.getInstance().lookup(IInvAuthenSevice.class).updateImportData(bodyVOList);
				if(str != null){
					if(MessageDialog.showYesNoDlg(getClientUI(), null, str + ",�Ƿ�ȡ����֤��") == MessageDialog.ID_YES){
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
	* @Description: �ж��뱾��������ͬһ��Ʊ�Ĳ�ֵ����Ƿ�����ͨ���������в�ֵ��ݵ�˰��֮��=Ʊ����˰��
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
		//���ŵ��ݵ�˰��
		UFDouble ntaxmny = receiveVO.getNtaxmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtaxmny();						
		/*if (ntaxmny == null) {
			throw new BusinessException("���ݱ��" + receiveVO.getVbillno() + "����˰��Ϊ�գ�");
		}*/
		//Ʊ����˰��
		UFDouble ntotalinvoicetax = receiveVO.getNtotalinvoicetax();
		//�ж��Ƿ�ѡ�У�ѡ��������ж�
		if (bissplit != null && UFBoolean.TRUE.equals(bissplit)) {
			//���˱��ŵ���֮����ͬ��Ʊ��ֵ��������ݵ�˰��֮��
			UFDouble othSumTax = UFDouble.ZERO_DBL;
			//ȡ��ͬһ����Ʊ��ֵ����е��ݣ��������Լ�
			List<ReceiveVO> receiveVOList = NCLocator.getInstance()
					.lookup(IReceiveService.class)
					.querySplitHeadVOsByCond(vinvcode, vinvno, pk_receive);
			if (receiveVOList != null && !receiveVOList.isEmpty()) {
				for (ReceiveVO othReceiveVO : receiveVOList) {
					if (IBillStatus.CHECKPASS != othReceiveVO.getVbillstatus()) {
						//��Ϊ��֤��Ҫ����ͬһ��Ʊ��ֵ���������
						throw new BusinessException("�뵥�ݱ��Ϊ" + vbillno + "�ĵ�������ͬһ��Ʊ�Ĳ�ֵ��� " 
								+ othReceiveVO.getVbillno() + "û������ͨ�������Ա����ݲ�����֤��");
					}
					//�ۼ��������ݵĽ��
					othSumTax = othSumTax.add(othReceiveVO.getNtaxmny());
				}
			}
			//�ж���˰���Ƿ����
			//���е��ݵ�˰��ϼ�
			UFDouble sumTax = othSumTax.add(ntaxmny);
			if (!sumTax.equals(ntotalinvoicetax)) {
				throw new BusinessException("���ݱ��Ϊ" + vbillno + "�ĵ��������ķ�Ʊ��Ʊ����˰�𲻵������в�ֵ���˰��֮�ͣ�������֤��");
			}	
		}		
	}
	
	
}
