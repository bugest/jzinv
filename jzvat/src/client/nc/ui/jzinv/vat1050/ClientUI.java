package nc.ui.jzinv.vat1050;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.jzinv.vat1050.IVatProjanalyService;
import nc.ui.bd.ref.busi.JobmngfilDefaultRefModel;
import nc.ui.jzinv.pub.tool.DeptPsnRela;
import nc.ui.jzinv.pub.tool.RefModelHelper;
import nc.ui.jzinv.pub.ui.BillManageUI;
import nc.ui.jzinv.pub.uifii.treemode.SafeCompute;
import nc.ui.jzinv.vat1050.handler.NlresttaxmnyEditHandler;
import nc.ui.jzinv.vatproj.pub.handler.PeriodEditHandler;
import nc.ui.jzinv.vatproj.pub.handler.ProjectEditHandler;
import nc.ui.jzinv.vatpub.buttonstate.BatchAddBtnVO;
import nc.ui.jzinv.vatpub.buttonstate.BeginBtnVO;
import nc.ui.pub.bill.BillCardBeforeEditListener;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillItemEvent;
import nc.ui.trade.bill.AbstractManageController;
import nc.ui.trade.manage.ManageEventHandler;
import nc.vo.jzinv.vat1050.VatProjanalyVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;

public class ClientUI extends BillManageUI implements
		BillCardBeforeEditListener {
	private static final long serialVersionUID = 1L;

	private DeptPsnRela deptPsnRela = null;
	/**
	 * ��չ��ť״̬--����
	 */
	public final static int ENABLE = 1;

	/**
	 * ��չ��ť״̬--������
	 */
	public final static int DISABLE = 2;
	public ClientUI() {
		super();
		this.getBillCardPanel().setBillBeforeEditListenerHeadTail(this);
		initModel();
	}

	public DeptPsnRela getDeptPsnRela() {
		if (deptPsnRela == null) {
			BillItem deptItem = getBillCardPanel().getHeadItem(
					VatProjanalyVO.PK_DEPTDOC);
			BillItem psnItem = getBillCardPanel().getHeadItem(
					VatProjanalyVO.PK_PSNDOC);
			deptPsnRela = new DeptPsnRela(deptItem, psnItem);
		}
		return deptPsnRela;
	}

	protected AbstractManageController createController() {
		return new ClientCtrl();
	}

	protected ManageEventHandler createEventHandler() {
		return new ClientEventHandler(this, getUIControl());
	}
	private void initModel(){
		JobmngfilDefaultRefModel projRefModel = new RefModelHelper<JobmngfilDefaultRefModel>().
				getRefModel(getBillCardPanel(), VatProjanalyVO.PK_PROJECT);
		projRefModel.addWherePart(" and bd_jobbasfil.pk_jobbasfil in (SELECT pk_projectbase FROM "
				+ " jzinv_vat_projtaxset where isnull(dr, 0) = 0 and  bissealed = 'N' and itaxtype = 0 "
				+ " and pk_corp ='" + getCorpPrimaryKey()+ "' ) ");
	}
	public void setDefaultData() throws Exception {
		super.setDefaultData();
		getDeptPsnRela().setOnAdd();
		getBillCardPanel().setHeadItem(getBaseCurrFldName(),
				getCy().getLocalCurrPK());
		String curDate = getEnvironment().getDate().toString();
		getBillCardPanel().setHeadItem(VatProjanalyVO.VPERIOD, curDate.substring(0, 7));
	}

	@Override
	protected void initPrivateButton() {
		super.initPrivateButton();
		addBeginBtn();
		addBatchAddBtn();
	}
	
	private void addBeginBtn(){
		BeginBtnVO beginBtnVO = new BeginBtnVO();
		beginBtnVO.getButtonVO().setExtendStatus(new int[]{ENABLE});
		addPrivateButton(beginBtnVO.getButtonVO());
	}
	
	private void addBatchAddBtn(){
		BatchAddBtnVO batchBtnVO = new BatchAddBtnVO();
		batchBtnVO.getButtonVO().setExtendStatus(new int[]{ENABLE});
		addPrivateButton(batchBtnVO.getButtonVO());
	}
	
	public void afterEdit(BillEditEvent e) {
		
		super.afterEdit(e);		
		if(e.getPos() == 1){
			afterEditBody(e);
		}
		else{
			afterEditHead(e);
		}
	}

	private void afterEditBody(BillEditEvent e) {
		
	}

	private void afterEditHead(BillEditEvent e) {
		if (VatProjanalyVO.PK_DEPTDOC.equals(e.getKey())) {
			getDeptPsnRela().setPsnByDept();
		}
		else if (VatProjanalyVO.PK_PSNDOC.equals(e.getKey())) {
			getDeptPsnRela().setDeptByPsn();
		}
		else if(VatProjanalyVO.PK_PROJECT.equals(e.getKey())){
			new ProjectEditHandler(this, null).cardHeadAfterEdit(e);
			try {
				// ������Ŀ+��˾+�ڼ�ȡ�á�Ԥ��˰�
				getNprepaytaxmny();
			} catch (BusinessException e1) {
				Logger.error(e1);
			}
		}
		else if(VatProjanalyVO.VPERIOD.equals(e.getKey())){
			new PeriodEditHandler(this, null).cardHeadAfterEdit(e);
			try {
				// ������Ŀ+��˾+�ڼ�ȡ�á�Ԥ��˰�
				getNprepaytaxmny();
			} catch (BusinessException e1) {
				Logger.error(e1);
			}
		}
		else if(VatProjanalyVO.NLRESTTAXMNY.equals(e.getKey()) || 
				VatProjanalyVO.NTHHADAUTHTAXMNY.equals(e.getKey()) || 
				VatProjanalyVO.NINTOOUTMNY.equals(e.getKey()) || 
				VatProjanalyVO.NTHAUTHNOCUTMNY.equals(e.getKey()) ||
				VatProjanalyVO.NLAUTHTHCUTMNY.equals(e.getKey()) ||
				VatProjanalyVO.NSALETAXMNY.equals(e.getKey()) || 
				VatProjanalyVO.NTAXABLESALEMNY.equals(e.getKey())){
			new NlresttaxmnyEditHandler(this).cardHeadAfterEdit(e);
		}
	}

	public boolean beforeEdit(BillItemEvent e) {
		return true;
	}
	
	/**
	 * ������Ŀ+��˾+�ڼ�ȡ�á�Ԥ��˰�,���ý��桾����˰�
	 * 
	 * @throws BusinessException
	 */
	public void getNprepaytaxmny() throws BusinessException {
		IVatProjanalyService vatProjanalyService = NCLocator.getInstance().lookup(IVatProjanalyService.class);
		BillCardPanel billCardPanel = getBillCardPanelWrapper().getBillCardPanel();
		// ��Ŀ
		String pk_project = (String) billCardPanel.getHeadItem(VatProjanalyVO.PK_PROJECT).getValueObject();
		// �ڼ�
		String vperiod = (String) billCardPanel.getHeadItem(VatProjanalyVO.VPERIOD).getValueObject();
		// �ڳ�
		UFBoolean bisbegin = new UFBoolean(billCardPanel.getHeadItem(VatProjanalyVO.BISBEGIN).getValueObject().toString());
		// ��˾
		String pk_corp = getPk_corp();
		
		// ȡ�ý��桾����˰�
		UFDouble nsaletaxmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NSALETAXMNY).
				getValueObject().toString());
		// ȡ�ý��桾��������˰�
		UFDouble nlresttaxmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NLRESTTAXMNY).
				getValueObject().toString());
		// ȡ�ý��桾��������֤˰�
		UFDouble nthhadauthtaxmny= new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NTHHADAUTHTAXMNY).
				getValueObject().toString());
		// ȡ�ý��桾����ת����
		UFDouble nintooutmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NINTOOUTMNY).
				getValueObject().toString());
		// ȡ�ý��桾������֤���ɵֿۡ�
		UFDouble nthauthnocutmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NTHAUTHNOCUTMNY).
				getValueObject().toString());
		// ȡ�ý��桾ǰ����֤���ڵֿۡ�
		UFDouble nlauththcutmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NLAUTHTHCUTMNY).
				getValueObject().toString());
		//Ӧ˰���۶�
		UFDouble ntaxablesalemny = billCardPanel.getHeadItem(VatProjanalyVO.NTAXABLESALEMNY) == null ? 
				UFDouble.ZERO_DBL : new UFDouble((String)billCardPanel.
				getHeadItem(VatProjanalyVO.NTAXABLESALEMNY).getValueObject());
		// ����������ʱ����
		// ��������˰��+��������֤˰��-������֤ת��-������֤���ɵֿ�+ǰ����֤���ڵֿ�
		UFDouble tempmnya = nlresttaxmny.add(nthhadauthtaxmny).sub(nintooutmny)
				.sub(nthauthnocutmny).add(nlauththcutmny);
		// ����˰��-����������˰��+��������֤˰��-������֤ת��-������֤���ɵֿ�+ǰ����֤���ڵֿۣ�
		UFDouble tempmnyb = nsaletaxmny.sub(tempmnya);
		
		// ********************* ��Ӧ��˰���ֵ���� *******************
		// Ӧ��˰�� = ����˰��-����������˰��+��������֤˰��-������֤ת��-������֤���ɵֿ�+ǰ����֤���ڵֿۣ�
		UFDouble ntaxablemny = new UFDouble(0);
		// ��������ΪӦ��˰����С������Ϊ���㡱
		if(SafeCompute.compare(tempmnyb,new UFDouble(0)) >= 0){
			ntaxablemny = tempmnyb;
		}
		// ���桾Ӧ��˰���ֵ
		billCardPanel.getHeadItem(VatProjanalyVO.NTAXABLEMNY).setValue(ntaxablemny);
		
		// ********************* ��Ԥ��˰��͡�Ӧ�Ͻ�˰���ֵ���� *******************
		// ������Ŀ+��˾+�ڼ�ȡ�á�Ԥ��˰�
		UFDouble nprepaytaxmny = vatProjanalyService.getNprepaytaxmny(pk_project,vperiod,pk_corp,bisbegin);
		// Ӧ�Ͻ�˰�����
		UFDouble npaidtaxmny = ntaxablemny.sub(nprepaytaxmny);
		// ���桾Ԥ��˰���ֵ
		billCardPanel.getHeadItem(VatProjanalyVO.NPREPAYTAXMNY).setValue(nprepaytaxmny);
		// ���桾Ӧ�Ͻ�˰���ֵ
		// ����˰�� = ������˰��-����������˰��+��������֤˰��-������֤ת��-������֤���ɵֿ�+ǰ����֤���ڵֿۣ���-Ԥ��˰��
		UFDouble tempnresttaxmny = tempmnyb.sub(nprepaytaxmny);
		// ������桾����˰�
		UFDouble nresttaxmny = new UFDouble(0);
		nresttaxmny = new UFDouble(Math.abs(tempnresttaxmny.doubleValue()));
		// ��Ӧ�Ͻ�˰�
		if(npaidtaxmny.compareTo(UFDouble.ZERO_DBL) > 0){
			billCardPanel.getHeadItem(VatProjanalyVO.NPAIDTAXMNY).setValue(npaidtaxmny);
			//����˰��
			billCardPanel.setHeadItem(VatProjanalyVO.NRESTTAXMNY, UFDouble.ZERO_DBL);
		}else{
			billCardPanel.getHeadItem(VatProjanalyVO.NPAIDTAXMNY).setValue(UFDouble.ZERO_DBL);
			// ���桾����˰���ֵ
			billCardPanel.getHeadItem(VatProjanalyVO.NRESTTAXMNY).setValue(nresttaxmny);
		}
		
		// ********************* ��Ӧ��˰������ֵ���� *******************
		// Ӧ��˰����������˰��-����������˰��+��������֤˰��-������֤ת��-������֤���ɵֿ�+ǰ����֤���ڵֿۣ���/Ӧ˰���۶�����ɸ�
		UFDouble ntaxes = new UFDouble(0);
		ntaxes = SafeCompute.multiply(tempmnyb.div(ntaxablesalemny), new UFDouble(100));
		// ���桾Ӧ��˰������ֵ
		billCardPanel.getHeadItem(VatProjanalyVO.NTAXES).setValue(ntaxes);
	}
	
	@Override 
	public String[] getHeadBaseItems() { 
	return new String[] {VatProjanalyVO.NLRESTTAXMNY, VatProjanalyVO.NTAXABLESALEMNY, 
			             VatProjanalyVO.NSALETAXMNY, VatProjanalyVO.NTHHADAUTHTAXMNY, 
			             VatProjanalyVO.NTAXABLEINMNY, VatProjanalyVO.NINTAXMNY, 
			             VatProjanalyVO.NINTOOUTMNY, 
			             VatProjanalyVO.NTAXABLEMNY, VatProjanalyVO.NRESTTAXMNY
			             ,VatProjanalyVO.NTHAUTHNOCUTMNY,VatProjanalyVO.NLAUTHTHCUTMNY
			             ,VatProjanalyVO.NPREPAYTAXMNY,VatProjanalyVO.NPAIDTAXMNY}; 
	} 
}
