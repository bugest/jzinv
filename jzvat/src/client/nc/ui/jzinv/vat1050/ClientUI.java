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
	 * 扩展按钮状态--可用
	 */
	public final static int ENABLE = 1;

	/**
	 * 扩展按钮状态--不可用
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
				// 根据项目+公司+期间取得【预缴税额】
				getNprepaytaxmny();
			} catch (BusinessException e1) {
				Logger.error(e1);
			}
		}
		else if(VatProjanalyVO.VPERIOD.equals(e.getKey())){
			new PeriodEditHandler(this, null).cardHeadAfterEdit(e);
			try {
				// 根据项目+公司+期间取得【预缴税额】
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
	 * 根据项目+公司+期间取得【预缴税额】,设置界面【留底税额】
	 * 
	 * @throws BusinessException
	 */
	public void getNprepaytaxmny() throws BusinessException {
		IVatProjanalyService vatProjanalyService = NCLocator.getInstance().lookup(IVatProjanalyService.class);
		BillCardPanel billCardPanel = getBillCardPanelWrapper().getBillCardPanel();
		// 项目
		String pk_project = (String) billCardPanel.getHeadItem(VatProjanalyVO.PK_PROJECT).getValueObject();
		// 期间
		String vperiod = (String) billCardPanel.getHeadItem(VatProjanalyVO.VPERIOD).getValueObject();
		// 期初
		UFBoolean bisbegin = new UFBoolean(billCardPanel.getHeadItem(VatProjanalyVO.BISBEGIN).getValueObject().toString());
		// 公司
		String pk_corp = getPk_corp();
		
		// 取得界面【销项税额】
		UFDouble nsaletaxmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NSALETAXMNY).
				getValueObject().toString());
		// 取得界面【上期留抵税额】
		UFDouble nlresttaxmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NLRESTTAXMNY).
				getValueObject().toString());
		// 取得界面【本期已认证税额】
		UFDouble nthhadauthtaxmny= new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NTHHADAUTHTAXMNY).
				getValueObject().toString());
		// 取得界面【进项转出】
		UFDouble nintooutmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NINTOOUTMNY).
				getValueObject().toString());
		// 取得界面【本期认证不可抵扣】
		UFDouble nthauthnocutmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NTHAUTHNOCUTMNY).
				getValueObject().toString());
		// 取得界面【前期认证本期抵扣】
		UFDouble nlauththcutmny = new UFDouble(billCardPanel.getHeadItem(VatProjanalyVO.NLAUTHTHCUTMNY).
				getValueObject().toString());
		//应税销售额
		UFDouble ntaxablesalemny = billCardPanel.getHeadItem(VatProjanalyVO.NTAXABLESALEMNY) == null ? 
				UFDouble.ZERO_DBL : new UFDouble((String)billCardPanel.
				getHeadItem(VatProjanalyVO.NTAXABLESALEMNY).getValueObject());
		// 金额的运算临时参数
		// 上期留底税额+本期已认证税额-进项认证转出-本期认证不可抵扣+前期认证本期抵扣
		UFDouble tempmnya = nlresttaxmny.add(nthhadauthtaxmny).sub(nintooutmny)
				.sub(nthauthnocutmny).add(nlauththcutmny);
		// 销项税额-（上期留底税额+本期已认证税额-进项认证转出-本期认证不可抵扣+前期认证本期抵扣）
		UFDouble tempmnyb = nsaletaxmny.sub(tempmnya);
		
		// ********************* 【应缴税额】赋值计算 *******************
		// 应缴税额 = 销项税额-（上期留底税额+本期已认证税额-进项认证转出-本期认证不可抵扣+前期认证本期抵扣）
		UFDouble ntaxablemny = new UFDouble(0);
		// 大于零则为应缴税额；如果小于零则为“零”
		if(SafeCompute.compare(tempmnyb,new UFDouble(0)) >= 0){
			ntaxablemny = tempmnyb;
		}
		// 界面【应缴税额】赋值
		billCardPanel.getHeadItem(VatProjanalyVO.NTAXABLEMNY).setValue(ntaxablemny);
		
		// ********************* 【预缴税额】和【应上缴税额】赋值计算 *******************
		// 根据项目+公司+期间取得【预缴税额】
		UFDouble nprepaytaxmny = vatProjanalyService.getNprepaytaxmny(pk_project,vperiod,pk_corp,bisbegin);
		// 应上缴税额计算
		UFDouble npaidtaxmny = ntaxablemny.sub(nprepaytaxmny);
		// 界面【预缴税额】赋值
		billCardPanel.getHeadItem(VatProjanalyVO.NPREPAYTAXMNY).setValue(nprepaytaxmny);
		// 界面【应上缴税额】赋值
		// 留抵税额 = 【销项税额-（上期留底税额+本期已认证税额-进项认证转出-本期认证不可抵扣+前期认证本期抵扣）】-预缴税额
		UFDouble tempnresttaxmny = tempmnyb.sub(nprepaytaxmny);
		// 定义界面【留抵税额】
		UFDouble nresttaxmny = new UFDouble(0);
		nresttaxmny = new UFDouble(Math.abs(tempnresttaxmny.doubleValue()));
		// 【应上缴税额】
		if(npaidtaxmny.compareTo(UFDouble.ZERO_DBL) > 0){
			billCardPanel.getHeadItem(VatProjanalyVO.NPAIDTAXMNY).setValue(npaidtaxmny);
			//留抵税额
			billCardPanel.setHeadItem(VatProjanalyVO.NRESTTAXMNY, UFDouble.ZERO_DBL);
		}else{
			billCardPanel.getHeadItem(VatProjanalyVO.NPAIDTAXMNY).setValue(UFDouble.ZERO_DBL);
			// 界面【留抵税额】赋值
			billCardPanel.getHeadItem(VatProjanalyVO.NRESTTAXMNY).setValue(nresttaxmny);
		}
		
		// ********************* 【应缴税负】赋值计算 *******************
		// 应缴税负：【销项税额-（上期留底税额+本期已认证税额-进项认证转出-本期认证不可抵扣+前期认证本期抵扣）】/应税销售额；可正可负
		UFDouble ntaxes = new UFDouble(0);
		ntaxes = SafeCompute.multiply(tempmnyb.div(ntaxablesalemny), new UFDouble(100));
		// 界面【应缴税负】赋值
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
