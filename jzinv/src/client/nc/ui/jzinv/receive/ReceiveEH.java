package nc.ui.jzinv.receive;

import nc.bs.framework.common.NCLocator;
import nc.bs.framework.exception.ComponentException;
import nc.bs.logging.Logger;
import nc.bs.uap.sfapp.util.SFAppServiceUtil;
import nc.itf.jzinv.inv0503.IReceiveCollService;
import nc.itf.jzinv.invpub.IJzinvMaintain;
import nc.itf.jzinv.pub.IBillCode;
import nc.itf.jzinv.pub.JZProxy;
import nc.itf.jzinv.pub.outsys.call.adapter.ocr.OcrOutSysCallAdapter;
import nc.itf.jzinv.pub.outsys.call.adapter.ocr.OcrSystemCallFactory;
import nc.itf.jzinv.receive.IReceiveService;
import nc.itf.uif.pub.IUifService;
import nc.ui.jzinv.pub.eventhandler.ManageEventHandler;
import nc.ui.jzinv.receive.action.ReceAddAction;
import nc.ui.jzinv.receive.action.ReceCancelAction;
import nc.ui.jzinv.receive.action.ReceEditAction;
import nc.ui.jzinv.receive.action.ReceRedAction;
import nc.ui.jzinv.receive.action.ReceSaveAction;
import nc.ui.pub.ButtonObject;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillListPanel;
import nc.ui.pub.bill.BillModel;
import nc.ui.pub.pf.PfUtilClient;
import nc.ui.trade.base.IBillOperate;
import nc.ui.trade.bill.RefBillTypeChangeEvent;
import nc.ui.trade.button.IBillButton;
import nc.ui.trade.controller.IControllerBase;
import nc.ui.trade.manage.BillManageUI;
import nc.uif.pub.exception.UifException;
import nc.vo.jzinv.cmpub.CmContractVO;
import nc.vo.jzinv.inv0503.ReceiveCollBVO;
import nc.vo.jzinv.inv0503.ReceiveCollVO;
import nc.vo.jzinv.param.InvParamTool;
import nc.vo.jzinv.pub.IJzinvButton;
import nc.vo.jzinv.pub.JZINVProxy;
import nc.vo.jzinv.pub.utils.SafeCompute;
import nc.vo.jzinv.receive.AggReceiveVO;
import nc.vo.jzinv.receive.ReceiveBVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.SuperVO;
import nc.vo.pub.billcodemanage.BillCodeObjValueVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.trade.pub.IExAggVO;

import org.apache.commons.lang.StringUtils;

public class ReceiveEH extends ManageEventHandler {
	private OcrOutSysCallAdapter ocrAdapter = OcrSystemCallFactory.getOcrSysCaller(getClientUI(), getClientUI().getPk_corp());
	private IReceiveService receiveService;
	public ReceiveEH(BillManageUI billUI, IControllerBase control) {
		super(billUI, control);
	}

	@Override
	public boolean isEnableMultiTablePRT() {
		return true;
	}

	private void clearChildPk(CircularlyAccessibleValueObject[] vos) throws Exception {
		if (vos == null || vos.length == 0)
			return;
		for (int i = 0; i < vos.length; i++) {
			vos[i].setPrimaryKey(null);
		}
	}

	@Override
	protected void onBoCopy() throws Exception {
		IJzinvMaintain jzinvMaintain= NCLocator.getInstance().lookup(IJzinvMaintain.class);
		// �������
		AggregatedValueObject copyVo = getBufferData().getCurrentVOClone();
		// ����������մ���
		copyVo.getParentVO().setPrimaryKey(null);
		if (copyVo instanceof IExAggVO) {
			clearChildPk(((IExAggVO) copyVo).getAllChildrenVO());
		} else {
			clearChildPk(copyVo.getChildrenVO());
		}
		// ����Ϊ��������
		getBillUI().setBillOperate(IBillOperate.OP_ADD);
		// ���õ��ݺ�
		String noField = getBillUI().getBillField().getField_BillNo();
		BillItem noitem = getBillCardPanelWrapper().getBillCardPanel().getHeadItem(noField);
		if (noitem != null)
			copyVo.getParentVO().setAttributeValue(noField, noitem.getValueObject());
		// ���ý�������
		//getBillUI().setCardUIData(copyVo);
		AggregatedValueObject actVO = (AggregatedValueObject) new ReceiveUtil(this.getClientUI())
				.dealCopy((AggReceiveVO) copyVo);
		
		// ����VO
		ReceiveVO receiveVO = (ReceiveVO) actVO.getParentVO();
		receiveVO.setBisaccountflg(new UFBoolean(false));
		receiveVO.setIauthstatus(0);
		actVO.setParentVO(receiveVO);
//		// ȡ�õ�������
//		String billtype = getClientUI().getBillCardPanel().getBillType();
//		// ȡ�ý�������
//		String tempPKreceive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
//		// ��������������ȡ��ƾ֤���ݣ�Ȼ��ȡ����ƾ֤
//		jzinvMaintain.unaccountplatfrom(tempPKreceive,actVO,billtype);
		// ���ý�������
		getBillUI().setCardUIData(actVO);

	}

	@Override
	public void onBoAdd(ButtonObject bo) throws Exception {
		setHeadItemEditable(true);
		super.onBoAdd(bo);
		new ReceAddAction((BillManageUI) getBillManageUI()).doAction();
		getClientUI().setBuyerOrSellerInfo(null, null);
	}

	@Override
	protected void onBoElse(int intBtn) throws Exception {
		super.onBoElse(intBtn);
		switch (intBtn) {
		case IJzinvButton.REFADDCOLLBTN:
			onBillRef(intBtn);
			break;
		case IJzinvButton.INV_RED:
			onRed();
			break;
		case IJzinvButton.IMAGE_LOAD:
			viewReceiveImage();
			break;
		case IJzinvButton.SUPPLEMENTINFO:
			addInfo();
			break;
		case IJzinvButton.BTN_ACCOUNT:
			accountbtn();
			break;
		case IJzinvButton.BTN_UNACCOUNT:
			unaccountbtn();
			break;
		default:
			break;
		}
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
		String[] obj = new String[] { AggReceiveVO.class.getName(), ReceiveVO.class.getName(), ReceiveBVO.class.getName(),
				ReceiveDetailVO.class.getName() };
		String pk_receive = getClientUI().getHeadItemString(ReceiveVO.PK_RECEIVE);
		AggregatedValueObject billVo = JZProxy.getIUifService().queryBillVOByPrimaryKey(obj, pk_receive);
		// ȡ�õ�������
		String billtype = getClientUI().getBillCardPanel().getBillType();
		// ����VO
		ReceiveVO receiveVO = (ReceiveVO) billVo.getParentVO();
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
		// ȡ�õ�������
		String billtype = getClientUI().getBillCardPanel().getBillType();
		// ���ݹ�˾�͵�������
		boolean isupload = InvParamTool.isToGLByBilltype(_getCorp().getPk_corp(), "99Z7");
		// ����VO
		ReceiveVO receiveVO = (ReceiveVO) billVo.getParentVO();
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
		getBillCardPanel().setHeadItem(ReceiveVO.BISACCOUNTFLG, new UFBoolean(true));
		// ���水ť�ĸ���
		getClientUI().updateCheckBtnenble();
		super.onBoRefresh();
	}
	
	private void addInfo() throws Exception {
		getClientUI().setBillOperate(IBillOperate.OP_EDIT);
		getClientUI().setInfoSupply(true);
		getClientUI().updateBtnWhenInfoAdd();
		setBodyUnEnable();
		BillItem[] headItems = getBillCardPanelWrapper().getBillCardPanel().getHeadItems();
		for (BillItem billItem : headItems) {
			billItem.setEnabled(false);
		}

		String[] unEnbleItems = getUnEnbleItemWhenInfoAdd();
		for (String key : unEnbleItems) {
			getBillCardPanelWrapper().getBillCardPanel().getHeadItem(key)
					.setEnabled(true);
		}
		super.onBoCard();
	}
	protected void setBodyUnEnable() throws Exception {
		String[] tableCodes = new String[]{"jzinv_receive_b","jzinv_receive_detail"};
		for (String tableCode : tableCodes) {
			BillModel billModel = getBillCardPanel().getBillModel(tableCode);
			if (billModel != null) {
				billModel.setEnabled(false);
				ButtonObject lineBtn = getButtonManager().getButton(IBillButton.Line);
				lineBtn.setEnabled(false);
				getClientUI().updateButtonUI();
			}
		}
	}
	protected BillCardPanel getBillCardPanel() {
		return getBillCardPanelWrapper().getBillCardPanel();
	}
	
	private String[] getUnEnbleItemWhenInfoAdd() {
		return new String[]{ReceiveVO.VINVCODE,ReceiveVO.VINVNO,ReceiveVO.DOPENDATE,ReceiveVO.VTAXPAYERNUMBER,ReceiveVO.VTAXSUPPLIERNUMBER,ReceiveVO.VSECRET};
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

	@SuppressWarnings("deprecation")
	private void onRed() throws Exception {
		ButtonObject bo = new ButtonObject("�ڳ�");
		super.onBoAdd(bo);
		new ReceRedAction(getBillManageUI()).doAction();
	}

	public void onBillRef(int intBtn) throws Exception {
		ButtonObject btn = getButtonManager().getButton(intBtn);
		btn.setTag(getBillUI().getRefBillType() + ":");
		onBoBusiTypeAdd(btn, null);

		btn.setTag(String.valueOf(intBtn));
		if (!PfUtilClient.isCloseOK()) {
			return;
		} else {
			getClientUI().setDefaultData();
		}
	}

	private ReceiveUI getClientUI() {
		return (ReceiveUI) getBillUI();
	}

	@Override
	protected boolean isDataChange() {
		return false;
	}

	private void setHeadItemEditable(boolean edit) {
		String[] fields = new String[] { ReceiveVO.NINVMNY, ReceiveVO.NINVTAXMNY, ReceiveVO.NTAXMNY, ReceiveVO.NTAXRATE };
		for (String field : fields) {
			BillItem headItem = getBillManageUI().getBillCardPanel().getHeadItem(field);
			if(headItem!=null){
				getBillManageUI().getBillCardPanel().getHeadItem(field).setEdit(edit);
				if (field.equals(ReceiveVO.NTAXMNY)) {
					continue;
				}
				headItem.setNull(true);
			}
		}
	}

	private final void onBoBusiTypeAdd(ButtonObject bo, String sourceBillId) throws Exception {
		getBusiDelegator()
				.childButtonClicked(bo, _getCorp().getPrimaryKey(), getBillUI()._getModuleCode(), _getOperator(),
						getUIController().getBillType(), getBillUI(), getBillUI().getUserObject(), sourceBillId);
		if (nc.ui.pub.pf.PfUtilClient.makeFlag) {
			// ���õ���״̬
			getBillUI().setCardUIState();
			// ����
			getBillUI().setBillOperate(IBillOperate.OP_ADD);
		} else {
			if (PfUtilClient.isCloseOK()) {
				if (getBillBusiListener() != null) {
					String tmpString = bo.getTag();
					int findIndex = tmpString.indexOf(":");
					String newtype = tmpString.substring(0, findIndex);
					RefBillTypeChangeEvent e = new RefBillTypeChangeEvent(this, null, newtype);
					getBillBusiListener().refBillTypeChange(e);
				}
				if (isDataChange()){
					setRefData(PfUtilClient.getRetVos());
				} else{
					AggregatedValueObject[] tempaggVo = null;
					// ѡ��������ݵĳ���
					AggregatedValueObject[] tempaggreceivevo = (AggregatedValueObject[]) PfUtilClient.getRetOldVos();
					if(tempaggreceivevo == null)
						return;
					//��ǰ���ѡ�еķ�Ʊ�Ƿ��Ѿ�������Ʊ
					for(AggregatedValueObject aggVO : tempaggreceivevo){
						Object invcode = aggVO.getParentVO().getAttributeValue("vinvcode");
						Object invno = aggVO.getParentVO().getAttributeValue("vinvno");
						if(invcode != null && invno != null && 
								invcode.toString().length() > 0 && 
								invno.toString().length() > 0){
							SuperVO[] vos = JZINVProxy.getJZUifService().queryByConditionAtJZ(ReceiveVO.class, 
									" dr=0 and vinvcode='" + invcode + "' and vinvno='" + invno + "'");
							if(vos.length > 0){
								throw new BusinessException("ѡ�еķ�Ʊ���Ѵ����������Ʊ������");
							}
						}else{
							throw new BusinessException("ѡ�еķ�Ʊ�д��ڲ�����Ҫ��ĵ���");
						}
					}
					AggregatedValueObject checkVO = getBillUI().getVOFromUI();
					setTSFormBufferToVO(checkVO);
					if(tempaggreceivevo.length ==1){
						setRefData(tempaggreceivevo);
						// �������ݼ������֮���Զ�������֤��ֹ����
						// ȡ�ñ�ͷ����Ʊ���ڡ�
						UFDate dopendate = new UFDate(getBillCardPanelWrapper().getBillCardPanel().
								getHeadItem(ReceiveVO.DOPENDATE).getValueObject().toString());
						UFDate denddate = getDenddate(dopendate);
						String vtaxpayernumber = getBillCardPanelWrapper().getBillCardPanel().
								getHeadItem(ReceiveVO.VTAXPAYERNUMBER).getValueObject().toString();
						String vtaxsuppliernumber = getBillCardPanelWrapper().getBillCardPanel().
								getHeadItem(ReceiveVO.VTAXSUPPLIERNUMBER).getValueObject().toString();
						
						//ȡ����PKֵ
						String payer = getPayer(vtaxpayernumber);
						if(payer != null)
							getBillCardPanelWrapper().getBillCardPanel()
								.setHeadItem(ReceiveVO.VTAXPAYERNAME, payer);
						
						//ȡ�۷�PKֵ
						String[] suppliers = getSupplier(vtaxsuppliernumber);
						if(suppliers != null){
							getBillCardPanelWrapper().getBillCardPanel().setHeadItem(
									ReceiveVO.PK_SUPPLIER, suppliers[0]);
							getBillCardPanelWrapper().getBillCardPanel().setHeadItem(
									ReceiveVO.PK_SUPPLIERBASE, suppliers[1]);
							
						}
						// ���桾��֤��ֹ���ڡ�����
						getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.DENDDATE, denddate);
						
						//�����������Ի�
//						ReceiveCollVO parentVO = (ReceiveCollVO) tempaggreceivevo[0].getParentVO();
//						getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.VRESERVE9, parentVO.getVtaxpayername());
//						getBillCardPanelWrapper().getBillCardPanel().setHeadItem(ReceiveVO.VRESERVE10, parentVO.getVtaxsuppliename());
						getBillCardPanelWrapper().getBillCardPanel().execHeadLoadFormulas();
					}else{
						for(int i = 0; i<tempaggreceivevo.length;i++){
							setTSFormBufferToVO(tempaggreceivevo[i]);
							// ����һ���µ�AGG��VO���������ϱ�������
							tempaggVo = new AggregatedValueObject[1];
							tempaggVo[0] = tempaggreceivevo[i];
							AggregatedValueObject aggVo= refVOChange(tempaggVo);
							// ȡ�õ��ݱ��
							String tempCode = getvbillNObylbjval(aggVo);
							aggVo.getParentVO().setAttributeValue("vbillno", tempCode);
							IUifService tempservice = NCLocator.getInstance().lookup(IUifService.class);
							IReceiveCollService service = NCLocator.getInstance().lookup(IReceiveCollService.class);
							tempservice.saveBill(aggVo);
							
							//��д��Ʊ��Ϣ�ɼ�
							ReceiveVO headVO = (ReceiveVO) aggVo.getParentVO();
							String vlastID = headVO.getVlastbillid();
							String vbillno = headVO.getVbillno();
							String pk_billtype = headVO.getPk_billtype();
							if(!StringUtils.isEmpty(vlastID)){
								service.register(vlastID, vbillno, pk_billtype);
							}
							service.register(vlastID, vbillno, pk_billtype);
						}
						MessageDialog.showHintDlg(getClientUI(), "��ʾ", "������Ʊ���ӳɹ����������������");
					}
					
				}
			}
		}
	}
	
	private String getvbillNObylbjval(AggregatedValueObject aggVo) throws Exception{
		
		SuperVO headVO = (SuperVO) aggVo.getParentVO();
		BillCodeObjValueVO codeObjVO = new BillCodeObjValueVO();
		String[] billCodeArray = null;
		billCodeArray = ((IBillCode) headVO).getBillCodeArray();
		codeObjVO.setAttributeValue("��˾", _getCorp().getPrimaryKey());
		if (billCodeArray == null || billCodeArray.length != 4) {
			codeObjVO.setAttributeValue("������Ŀ����", headVO
					.getAttributeValue(IBillCode.PROJ_FIELD));
		} else {
			if (billCodeArray[IBillCode.DEPT_INDEX] != null)
				codeObjVO
						.setAttributeValue("����",headVO
										.getAttributeValue(billCodeArray[IBillCode.DEPT_INDEX]));
			if (billCodeArray[IBillCode.CONT_INDEX] != null)
				codeObjVO
						.setAttributeValue(
								 "������ͬ����"
,
								headVO
										.getAttributeValue(billCodeArray[IBillCode.CONT_INDEX]));
		}
		
		// �õ�ϵͳ���ɵ��ݺ�
	    String vbillno = SFAppServiceUtil.getBillcodeRuleService()
			.getBillCode_RequiresNew(getClientUI().getFilePane_pk_billtype(), getClientUI().getPk_corp(),null, codeObjVO);
	    return vbillno;
	}
	
	/**
	 * �������ݽ�������
	 */
	@Override
	protected AggregatedValueObject refVOChange(AggregatedValueObject[] vos) throws Exception {
		if (null == vos || vos.length == 0) {
			return null;
		}
		ReceiveCollVO collVO = (ReceiveCollVO) vos[0].getParentVO();
		ReceiveVO receHVO = new ReceiveVO();
		receHVO.setPk_project(collVO.getPk_project());//��Ŀ
		receHVO.setPk_projectbase(collVO.getPk_projectbase());//��Ŀ
		receHVO.setPk_supplier(collVO.getPk_supplier());//��Ӧ��(���۷�)
		receHVO.setPk_supplierbase(collVO.getPk_supplier_base());//��Ӧ��
		receHVO.setPk_contract(collVO.getPk_contract());//��ͬ
		// ��Ʊ�ࣨ�޺�ͬ����ͬ�ֶε�����
		receHVO.setPk_contractstr(collVO.getPk_contractstr());//��ͬ����
		receHVO.setPk_contractstr_name(collVO.getPk_contractstr_name());//��ͬ��������
		receHVO.setDopendate(collVO.getDopendate());//��Ʊ����
		
		//�Զ�������֤��ֹ����
		UFDate denddate = getDenddate(collVO.getDopendate());
		receHVO.setDenddate(denddate);
		
		//�����������Ի�
//		receHVO.setVreserve9(collVO.getVtaxpayername());
//		receHVO.setVreserve10(collVO.getVtaxsuppliename());
		
		receHVO.setDtodate(collVO.getDopendate());//Ʊ������
		receHVO.setVinvno(collVO.getVinvno());//��Ʊ��
		receHVO.setVinvcode(collVO.getVinvcode());//��Ʊ����
		receHVO.setVtaxctrlmachno(collVO.getVdef1());//���������   add  bu 20161230
		//��Դ����   ��Ʊ��Ϣ�ɼ� ����	pk_taxpayer
		receHVO.setVtaxpayername(collVO.getPk_taxpayer());//��������
		
		//ȡ����PKֵ
		String payer = getPayer(collVO.getVtaxpayerno());
		if(payer != null)
			receHVO.setVtaxpayername(payer);
		
		//ȡ����PKֵ
		String[] supplier = getSupplier(collVO.getVtaxsupplieno());
		if(supplier != null){
			receHVO.setPk_supplier(supplier[0]);
			receHVO.setPk_supplierbase(supplier[1]);
		}
		
		receHVO.setVtaxpayernumber(collVO.getVtaxpayerno());//������˰��ʶ���
		receHVO.setVtaxpayerphone(collVO.getVtaxpayeraddress());//������ַ�绰
		receHVO.setVbankaccount(collVO.getVtaxpayeraccount());//�����������м������ʺ�
		receHVO.setVtaxsuppliernumber(collVO.getVtaxsupplieno());//���۷���˰��ʶ���
		receHVO.setVsupplierphone(collVO.getVtaxsupplieaddr());////���۷���ַ�绰
		receHVO.setVsupbankaccount(collVO.getVtaxsupplieaccount());//���۷������м��˺�
		receHVO.setVsuppliername(collVO.getVtaxsuppliename());
		receHVO.setNinvmny(collVO.getNinvmny());//��Ʊ���(��˰)
		receHVO.setNinvtaxmny(collVO.getNinvtaxmny());//��Ʊ���(��˰)
		receHVO.setNoriginvmny(collVO.getNinvmny());//��Ʊ���(ԭ����˰)
		receHVO.setNoriginvtaxmny(collVO.getNinvtaxmny());//��Ʊ���(ԭ�Һ�˰)
		receHVO.setNtaxmny(collVO.getNtaxmny());//˰��
		receHVO.setPk_image(collVO.getPk_image());//��Ӱ��λ��   add by haojx 2016/8/24
		receHVO.setPk_paycorp(getClientUI()._getCorp().getPk_corp());
		//��Ʊ���շ�Ʊ��Ϣ�ɼ�ʱ  ��֤״̬  Ĭ�ϸ�ֵ    δ��֤
		receHVO.setIauthstatus(0);

		//ˮ��ʮ���ָ��Ի������ݷ�Ʊ��Ϣ�ɼ������ϼ���λ��ȡ���Ե���ģ�幫ʽ����ֵ�ķ�ʽ����
//		receHVO.setPk_supfinance(collVO.getPk_topcorp());
		//ˮ��ʮ���ָ��Ի�����Ʊ���кţ���������
//		receHVO.setVserialnum(collVO.getVserialnum());
		
		//˰��
		UFDouble ntaxrate = SafeCompute.div(collVO.getNtaxmny(), collVO.getNinvmny());
		receHVO.setNtaxrate(SafeCompute.multiply(new UFDouble(100), ntaxrate));
		receHVO.setVlastbillid(collVO.getPk_receive_coll());//��Ʊ��Ϣ�ɼ�����
		//��Ӧ������
		receHVO.setIsupplytype(collVO.getIsupplytype());
		
		//		��Ŀ����	vsecretcode
		receHVO.setVsecret(collVO.getVsecretcode());//����

		AggReceiveVO aggVo = new AggReceiveVO();

		String pk_corp = getClientUI().getPk_corp();
		// ���ն�ѡ����ֶ�
		receHVO.setPk_corp(getClientUI()._getCorp().getPk_corp());//
		receHVO.setBisauthen(new UFBoolean(false));//
		receHVO.setPk_billtype(getClientUI().getFilePane_pk_billtype());//
		receHVO.setVbillstatus(8);//
		receHVO.setDbilldate( _getDate());//
		receHVO.setVoperatorid(getBillUI()._getOperator());//
		receHVO.setDmakedate(_getDate());//
		receHVO.setBisopenred(new UFBoolean(false));
		receHVO.setBisred(new UFBoolean(false));
		
	    
		//IJzinvBillType.JZINV_RECEIVE_MT Ӧ��ȡֵ������Ʊ�ĵ�������
		boolean bisupload = nc.vo.jzinv.param.InvParamTool.isToGLByBilltype(pk_corp, getClientUI().getUIControl()
				.getBillType());
		receHVO.setBisupload(bisupload ? UFBoolean.TRUE : UFBoolean.FALSE);
		aggVo.setParentVO(receHVO);
		setBodyVO(aggVo,collVO.getPk_contract(),collVO.getPk_supplier(),collVO.getPk_supplier_base(),collVO.getPk_receive_coll());
		return aggVo;
	}
	
	/**
	 * <p>Title: getDenddate</p>
	 * <p>Description: ȡ����֤��ֹ����</p>
	 * @param opendate
	 * @return
	 */
	private UFDate getDenddate(UFDate opendate){
		UFDate date = null;
		if(opendate != null){
			//2017-7-1֮ǰ˳��180�죬����˳��360
			if(opendate.compareTo(new UFDate("2017-07-01")) < 0){
				date = opendate.getDateAfter(179);
			}else{
				date = opendate.getDateAfter(359);
			}
		}
		return date;
	}

	
	private void setBodyVO(AggReceiveVO aggVo, String pk_contract, String pk_supplier, String pk_supplier_base,String pk_receive_coll)  throws Exception {
		ReceiveVO hvo = (ReceiveVO) aggVo.getParentVO();
		ReceiveDetailVO detailVO = new ReceiveDetailVO();
		detailVO.setPk_project(hvo.getPk_project());
		detailVO.setNtaxmny(hvo.getNtaxmny());
		detailVO.setNtaxrate(hvo.getNtaxrate());
		detailVO.setNthrecemny(hvo.getNinvmny());
		detailVO.setNthrecetaxmny(hvo.getNinvtaxmny());
		detailVO.setPk_contract(pk_contract);
		detailVO.setPk_supplier(pk_supplier);
		detailVO.setPk_supplierbase(pk_supplier_base);
		
		CmContractVO con = getContractVO(pk_contract);
		if(con != null){
			//Լ��Ԥ����(��˰)  ��ֵʱ  �����ж�ֵ����
			UFDouble npreoriginmny = con.getNpreoriginmny();
			if(npreoriginmny == null){
				if(con.getNpreorigintaxmny() != null){
					UFDouble bilv = SafeCompute.add(new UFDouble(1), SafeCompute.div(con.getNtaxrate(), new UFDouble(100)));
					npreoriginmny = SafeCompute.div(con.getNpreorigintaxmny(), bilv);
				}
			}
			detailVO.setPk_supplier(con.getPk_second());
			detailVO.setPk_supplierbase(con.getPk_secondbase());
			detailVO.setPk_origintype(con.getPk_origintype());
			detailVO.setVcontractcode(con.getVbillno());
			detailVO.setVcontractname(con.getVname());
			detailVO.setDcontractdate(con.getDbilldate());
			//��ͬ���
			detailVO.setNorigcontramny(con.getNcurrcontoriginmny());
			detailVO.setNorigcontrataxmny(con.getNcurrcontorigintaxmny());
			detailVO.setNcontramny(con.getNcurrcontoriginmny());
			detailVO.setNcontrataxmny(con.getNcurrcontorigintaxmny());
			//Լ��Ԥ����
			detailVO.setNorigprepaymny(npreoriginmny);//Լ��Ԥ������
			detailVO.setNorigprepaytaxmny(con.getNpreorigintaxmny());
			detailVO.setNprepaymny(npreoriginmny);
			detailVO.setNprepaytaxmny(con.getNpreorigintaxmny());
			//�ۼ���Ʊ���
			detailVO.setNorigsumrecemny(con.getNinvoriginmny());//�ۼ���Ʊ���
			detailVO.setNorigsumrecetaxmny(con.getNinvorigintaxmny());
			detailVO.setNsumrecemny(con.getNinvbasemny());
			detailVO.setNsumrecetaxmny(con.getNinvbasetaxmny());
			detailVO.setNtaxrate(con.getNtaxrate());
			detailVO.setVlastbillid(con.getPk_contract());
			detailVO.setVlastbilltype(con.getPk_billtype());
		}
		// ȡ�÷�Ʊ��Ϣ�ɼ�������׷���� ---xuansha[��Ʊ��Ϣ�ɼ�]���ӡ�ֽ�ʷ�Ʊ���ݡ�ҳǩ
		// ���ݷ�Ʊ��Ϣ�ɼ���������ȡ�á�ֽ�ʷ�Ʊ���ݡ�����
		ReceiveCollBVO[] vos = (ReceiveCollBVO[]) JZINVProxy.getJZUifService().queryByConditionAtJZ(ReceiveCollBVO.class, 
				" dr=0 and pk_receive_coll='"+pk_receive_coll+"'");
		// �������ϡ�ֽ�ʷ�Ʊ���ݡ�
		if(vos != null && vos.length != 0){
			// ����ֽ�ʷ�Ʊ��������
			ReceiveCollBVO[] collbVO = new ReceiveCollBVO[vos.length];
			for(int i = 0; i<vos.length;i++){
				ReceiveCollBVO tempcollvo = new ReceiveCollBVO();
				tempcollvo.setVlastbillrowid(vos[i].getVlastbillrowid());
				tempcollvo.setVdef4(vos[i].getVdef4());
				tempcollvo.setVreserve5(vos[i].getVreserve5());
				tempcollvo.setVdef18(vos[i].getVdef18());
				tempcollvo.setVinvtype(vos[i].getVinvtype());
				tempcollvo.setNoriginvmny(vos[i].getNoriginvmny());
				tempcollvo.setNorigpricemny(vos[i].getNorigpricemny());
				tempcollvo.setVdef16(vos[i].getVdef16());
				tempcollvo.setVdef17(vos[i].getVdef17());
				tempcollvo.setVdef12(vos[i].getVdef12());
				tempcollvo.setVsourcebilltype(vos[i].getVsourcebilltype());
				tempcollvo.setIbusitype(vos[i].getIbusitype());
				tempcollvo.setNpricemny(vos[i].getNpricemny());
				tempcollvo.setVsourcebillrowid(vos[i].getVsourcebillrowid());
				tempcollvo.setVreserve9(vos[i].getVreserve9());
				tempcollvo.setCrowno(vos[i].getCrowno());
				tempcollvo.setVdef19(vos[i].getVdef19());
				tempcollvo.setNtaxmny(vos[i].getNtaxmny());
				tempcollvo.setNnum(vos[i].getNnum());
				tempcollvo.setVdef13(vos[i].getVdef13());
				tempcollvo.setVdef3(vos[i].getVdef3());
				tempcollvo.setVreserve6(vos[i].getVreserve6());
				tempcollvo.setNinvmny(vos[i].getNinvmny());
				tempcollvo.setVreserve10(vos[i].getVreserve10());
				tempcollvo.setVlastbillid(vos[i].getVlastbillid());
				tempcollvo.setVreserve3(vos[i].getVreserve3());
				tempcollvo.setNdiffermny(vos[i].getNdiffermny());
				tempcollvo.setInvnote(vos[i].getInvnote());
				tempcollvo.setVreserve4(vos[i].getVreserve4());
				tempcollvo.setNtaxrate(vos[i].getNtaxrate());
				tempcollvo.setVdef10(vos[i].getVdef10());
				tempcollvo.setVdef5(vos[i].getVdef5());
				tempcollvo.setVreserve8(vos[i].getVreserve8());
				tempcollvo.setVdef11(vos[i].getVdef11());
				tempcollvo.setVinvspec(vos[i].getVinvspec());
				tempcollvo.setVdef7(vos[i].getVdef7());
				tempcollvo.setVdef20(vos[i].getVdef20());
				tempcollvo.setVreserve2(vos[i].getVreserve2());
				tempcollvo.setVdef2(vos[i].getVdef2());
				tempcollvo.setNinvtaxmny(vos[i].getNinvtaxmny());
				tempcollvo.setVmemo(vos[i].getVmemo());
				tempcollvo.setVdef1(vos[i].getVdef1());
				tempcollvo.setNorigpricetaxmny(vos[i].getNorigpricetaxmny());
				tempcollvo.setVdef14(vos[i].getVdef14());
				tempcollvo.setVreserve1(vos[i].getVreserve1());
				tempcollvo.setVlastbilltype(vos[i].getVlastbilltype());
				tempcollvo.setVreserve7(vos[i].getVreserve7());
				tempcollvo.setVdef9(vos[i].getVdef9());
				tempcollvo.setVdef8(vos[i].getVdef8());
				tempcollvo.setNpricetaxmny(vos[i].getNpricetaxmny());
				tempcollvo.setVinvunit(vos[i].getVinvunit());
				tempcollvo.setVdef6(vos[i].getVdef6());
				tempcollvo.setVdef15(vos[i].getVdef15());
				tempcollvo.setVsourcebillid(vos[i].getVsourcebillid());
				tempcollvo.setItaxfavourable(vos[i].getItaxfavourable());
				tempcollvo.setNoriginvtaxmny(vos[i].getNoriginvtaxmny());
				collbVO[i] = tempcollvo;
			}
			aggVo.setTableVO(ReceiveBVO.TABCODE, collbVO);
		}
		aggVo.setTableVO(ReceiveDetailVO.TABCODE, new ReceiveDetailVO[] { detailVO });
	}
	
	private CmContractVO getContractVO(String pk_contract){
		IUifService service = JZINVProxy.getIUifService();
		try {
			CmContractVO contraVO = (CmContractVO) service.queryByPrimaryKey(CmContractVO.class, pk_contract);
			return contraVO;
		} catch (UifException e) {
			Logger.error(e.getMessage());
		}
		return null;
	}
	
	@Override
	protected void onBoLineAdd() throws Exception {
		super.onBoLineAdd();
		if (ReceiveDetailVO.TABCODE.equals(this.getBillCardPanelWrapper().getBillCardPanel().getCurrentBodyTableCode())) {
			int row = this.getClientUI().getBillCardPanel().getBillModel(ReceiveDetailVO.TABCODE).getRowCount();
			
			if(this.getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.NTAXRATE) != null){
				
				UFDouble ntaxRate = new UFDouble((String) this.getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTAXRATE).getValueObject());
				
				this.getClientUI().getBillCardPanel().setBodyValueAt(ntaxRate, row - 1, ReceiveVO.NTAXRATE);
				
			}
			
			String pk_project = (String) this.getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_PROJECT)
					.getValueObject();
			String pk_projectbase = (String) this.getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_PROJECTBASE).getValueObject();

			this.getClientUI().getBillCardPanel()
					.setBodyValueAt(pk_project, row - 1, ReceiveVO.PK_PROJECT, ReceiveDetailVO.TABCODE);
			this.getClientUI().getBillCardPanel()
					.setBodyValueAt(pk_projectbase, row - 1, ReceiveVO.PK_PROJECTBASE, ReceiveDetailVO.TABCODE);
			if(this.getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_CONTRACT) != null){
				
				String pk_contract = (String) this.getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.PK_CONTRACT).getValueObject();
				
				this.getClientUI().getBillCardPanel().setBodyValueAt(pk_contract, row - 1, ReceiveVO.PK_CONTRACT);
				
				CmContractVO contraVO = getContractVO(pk_contract);
				if(contraVO != null){
					
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_project(), row-1, ReceiveDetailVO.PK_PROJECT, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_projectbase(), row-1, ReceiveDetailVO.PK_PROJECTBASE, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_second(), row-1, ReceiveDetailVO.PK_SUPPLIER, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_secondbase(), row-1, ReceiveDetailVO.PK_SUPPLIERBASE, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_origintype(), row-1, ReceiveDetailVO.PK_ORIGINTYPE, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getVbillno(), row-1, ReceiveDetailVO.VCONTRACTCODE, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getVname(), row-1, ReceiveDetailVO.VCONTRACTNAME, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getDbilldate(), row-1, ReceiveDetailVO.DCONTRACTDATE, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNcurrcontoriginmny(), row-1, ReceiveDetailVO.NORIGCONTRAMNY, ReceiveDetailVO.TABCODE);//��ͬ���
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNcurrcontorigintaxmny(), row-1, ReceiveDetailVO.NORIGCONTRATAXMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNcurrcontoriginmny(), row-1, ReceiveDetailVO.NCONTRAMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNcurrcontorigintaxmny(), row-1, ReceiveDetailVO.NCONTRATAXMNY, ReceiveDetailVO.TABCODE);
					
					//Լ��Ԥ����(��˰)  ��ֵʱ  �����ж�ֵ����
					UFDouble npreoriginmny = contraVO.getNpreoriginmny();
					//if(npreoriginmny == null){
					if(contraVO.getNpreorigintaxmny() != null){
						UFDouble bilv = SafeCompute.add(new UFDouble(1), SafeCompute.div(contraVO.getNtaxrate(), new UFDouble(100)));
						
						npreoriginmny = SafeCompute.div(contraVO.getNpreorigintaxmny(), bilv);
					}
					//}
					
					this.getClientUI().getBillCardPanel().setBodyValueAt(npreoriginmny, row-1, ReceiveDetailVO.NORIGPREPAYMNY, ReceiveDetailVO.TABCODE);//Լ��Ԥ������
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNpreorigintaxmny(), row-1, ReceiveDetailVO.NORIGPREPAYTAXMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(npreoriginmny, row-1, ReceiveDetailVO.NPREPAYMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNpreorigintaxmny(), row-1, ReceiveDetailVO.NPREPAYTAXMNY, ReceiveDetailVO.TABCODE);
					
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNinvoriginmny(), row-1, ReceiveDetailVO.NORIGSUMRECEMNY, ReceiveDetailVO.TABCODE);//�ۼ���Ʊ���
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNinvorigintaxmny(), row-1, ReceiveDetailVO.NORIGSUMRECETAXMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNinvbasemny(), row-1, ReceiveDetailVO.NSUMRECEMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNinvbasetaxmny(), row-1, ReceiveDetailVO.NSUMRECETAXMNY, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getNtaxrate(), row-1, ReceiveDetailVO.NTAXRATE, ReceiveDetailVO.TABCODE);
					
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_contract(), row-1, ReceiveDetailVO.VLASTBILLID, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getPk_billtype(), row-1, ReceiveDetailVO.VLASTBILLTYPE, ReceiveDetailVO.TABCODE);
					this.getClientUI().getBillCardPanel().setBodyValueAt(contraVO.getTs(), row-1, "ts", ReceiveDetailVO.TABCODE);
					
				}
			}
			//��Ʊ��������ʱ  �Զ������ͷ  ��Ӧ��
			if(this.getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_SUPPLIER) != null){
				
				String pk_supplier = (String) this.getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.PK_SUPPLIER).getValueObject();
				
				this.getClientUI().getBillCardPanel().setBodyValueAt(pk_supplier, row - 1, ReceiveVO.PK_SUPPLIER);
				
			}
			
			this.getClientUI().getBillCardPanel().getBillModel(ReceiveDetailVO.TABCODE).execLoadFormula();
		}

	}

	@Override
	protected void onBoLineDel() throws Exception {
		super.onBoLineDel();
		this.getClientUI().sumReceDetailMny(getBillCardPanel());

	}

	@Override
	protected void onBoLinePaste() throws Exception {
		super.onBoLinePaste();
		this.getClientUI().sumReceDetailMny(getBillCardPanel());
	}

	@Override
	protected void onBoLinePasteToTail() throws Exception {
		super.onBoLinePasteToTail();

	}

	@Override
	protected void onBoSave() throws Exception {
		if(checkBeforeSave()){
			if(getClientUI().isInfoSupply()){
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
					getClientUI().setInfoSupply(false);
			}else{
				new ReceSaveAction((BillManageUI) getBillManageUI()).doAction();
				super.onBoSave();
			}
		}
	}
	/**
	 * ���ܱ�ͷ
	 * @param cardPanel
	 * @throws BusinessException
	 */
	private void sumReceDetailMny(BillCardPanel cardPanel) throws BusinessException{
		String pk_corp = cardPanel.getCorp();
		int first_mode = InvParamTool.getTaxFirstMode(pk_corp);
		String[] invmnys = new String[]{ReceiveVO.NINVTAXMNY,ReceiveVO.NINVMNY};
		String[] nthrecemnys = new String[]{ReceiveDetailVO.NTHRECETAXMNY,ReceiveDetailVO.NTHRECEMNY};
		
		int rowcount = cardPanel.getBillModel(ReceiveDetailVO.TABCODE).getRowCount();
		UFDouble ninvmny_h = new UFDouble((String)cardPanel.getHeadItem(invmnys[first_mode]).getValueObject());
		//��Ʊ���ݵı�����Ʊ���֮�ͱ�����ڱ�ͷ�ķ�Ʊ���
		UFDouble nthrecemny_cllect = UFDouble.ZERO_DBL;
		for(int i = 0; i < rowcount; i++){
			ReceiveDetailVO detailVO = (ReceiveDetailVO) cardPanel.getBillModel(ReceiveDetailVO.TABCODE).
					getBodyValueRowVO(i, ReceiveDetailVO.class.getName());
			UFDouble nthrecemny = (UFDouble)detailVO.getAttributeValue(nthrecemnys[first_mode]);
			nthrecemny_cllect = SafeCompute.add(nthrecemny_cllect, nthrecemny);
		}

		if(nthrecemny_cllect.compareTo(ninvmny_h) != 0){
			throw new BusinessException("��Ʊ���ݵı�����Ʊ���֮�ͱ�����ڱ�ͷ��Ʊ���, ����������!");
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
		//linan ע�͵���䣬��Ϊ����Ѿ�����У����
		/*SuperVO[] vos = JZINVProxy.getJZUifService().queryByConditionAtJZ(ReceiveVO.class, 
				" dr=0 and vinvcode='"+vinvcode+"' and vinvno='"+vinvno+"' and pk_receive <>'"+pk_receicve+"'");
		if(vos.length>0){
			MessageDialog.showHintDlg(getClientUI(), "��ʾ", "��Ʊ����+��Ʊ���Ѵ��ڣ�");
			return false;
		}*/
		return true;
	}
	
	private Object getHeadItemValue(String key){
		return getClientUI().getBillCardPanel().getHeadItem(key).getValueObject();
	}

	
	@Override
	protected void onBoCancel() throws Exception {
		super.onBoCancel();
		new ReceCancelAction((BillManageUI) getBillManageUI()).doAction();
	}

	@Override
	protected void onBoEdit() throws Exception {
		String vlastbillid = (String) getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VLASTBILLID)
				.getValueObject();
		if (!StringUtils.isEmpty(vlastbillid)) {
			setHeadItemEditable(false);
		} else {
			setHeadItemEditable(true);
		}
		super.onBoEdit();
		new ReceEditAction((BillManageUI) getBillManageUI()).doAction();
	}

	@Override
	public void onBoAudit() throws Exception {
		super.onBoAudit();
		onBoRefresh();
	}

	@Override
	protected void onBoCancelAudit() throws Exception {
		super.onBoCancelAudit();
		onBoRefresh();
	}
	
	
	public IReceiveService getReceiveService(){
		if(receiveService == null){
			receiveService = NCLocator.getInstance().lookup(IReceiveService.class);
		}
		return receiveService;
	}
	
	public String getPayer(String vtaxpayerno) throws BusinessException{
		//ȡ����PKֵ
		String payer_cubasdoc = getReceiveService().getCubasDoc(vtaxpayerno);
		if(StringUtils.isNotBlank(payer_cubasdoc)){
			String payer_cumandoc =	getReceiveService().getCumandoc(getClientUI().getPk_corp(), payer_cubasdoc);	
			return payer_cumandoc;
		}
		return null;
	}
	
	
	public String[] getSupplier(String vtaxsupplieno) throws BusinessException{
		//ȡ�۷�PKֵ
		String supplier_cubasdoc = getReceiveService().getCubasDoc(vtaxsupplieno);
		if(StringUtils.isNotBlank(supplier_cubasdoc)){
			String supplier_cumandoc =	getReceiveService().getCumandoc(
					getClientUI().getPk_corp(),
						supplier_cubasdoc);
			
			return new String[]{supplier_cumandoc,supplier_cubasdoc};
		}
		return null;
	}
}