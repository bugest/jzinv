package nc.ui.jzinv.receive.handler;

import nc.itf.jzinv.pub.taxrate.util.JZTaxRateUtil;
import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.jzinv.pub.tool.InvMnyTool;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillModel;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.param.InvParamTool;
import nc.vo.jzinv.pub.utils.SafeCompute;
import nc.vo.jzinv.receive.ReceiveBVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFDouble;

/**
 * 收票“税率”编辑事件
 * @author mayyc
 *
 */
public class NtaxrateEditHandler extends InvCardEditHandler{

	public NtaxrateEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}

	/**
	 * 收票表头“税率”编辑后事件
	 */
	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		if(ReceiveVO.NTAXRATE.equals(e.getKey())){
			doBodyWhenEditHeadRate(e);
		}

	}
	private void doBodyWhenEditHeadRate(BillEditEvent e){
		BillCardPanel cardPanel = getClientUI().getBillCardPanel();
		String[] taxmnyFields = new String[]{ReceiveVO.NINVTAXMNY};
		String[] mnyFields = new String[]{ReceiveVO.NINVMNY};
		InvMnyTool.computeMnyByTaxrate(ReceiveVO.NTAXRATE, taxmnyFields, mnyFields, cardPanel, e);
		UFDouble ntaxrate = new UFDouble((String)this.getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTAXRATE).getValueObject());
		// 取得表头【差额缴税扣除金额】字段
		UFDouble ndiffermny = cardPanel.getHeadItem(ReceiveVO.NDIFFERMNY) == null ? null :
			new UFDouble((String)cardPanel.getHeadItem(ReceiveVO.NDIFFERMNY).getValueObject());
		// 取得表头【发票金额】
		UFDouble ninvtaxmnytemp = cardPanel.getHeadItem(ReceiveVO.NINVTAXMNY) == null ? null :
			new UFDouble((String)cardPanel.getHeadItem(ReceiveVO.NINVTAXMNY).getValueObject());
		// 税额计算公式修改：税额=（收票金额—差额缴税扣除金额）\（1+税率）*税率
    	UFDouble ntaxmnytemp = SafeCompute.multiply(SafeCompute.div(
				SafeCompute.sub(ninvtaxmnytemp, ndiffermny),
				SafeCompute.add(UFDouble.ONE_DBL, SafeCompute.div(ntaxrate, new UFDouble(100))))
				,SafeCompute.div(ntaxrate, new UFDouble(100)));
    	// 表头【税额】赋值
    	cardPanel.setHeadItem(ReceiveVO.NTAXMNY, ntaxmnytemp);
    	// 表头【含税算无税】赋值
    	cardPanel.setHeadItem(ReceiveVO.NINVMNY, SafeCompute.sub(ninvtaxmnytemp,ntaxmnytemp));
    	
		int rowCount = this.getClientUI().getBillCardPanel().getBillModel("jzinv_receive_detail").getRowCount();			
		if(rowCount > 0){
			UFDouble ninvmny = getHeadUFDouble(ReceiveVO.NINVMNY);
			UFDouble ninvtaxmny = getHeadUFDouble(ReceiveVO.NINVTAXMNY);
			this.getClientUI().getBillCardPanel().setBodyValueAt(ninvmny, 0, ReceiveDetailVO.NTHRECEMNY);
			this.getClientUI().getBillCardPanel().setBodyValueAt(ninvtaxmny, 0, ReceiveDetailVO.NTHRECETAXMNY);
		}
		BillModel detailModel = getCardPanel().getBillModel(ReceiveDetailVO.TABCODE);
		String pk_corp = cardPanel.getCorp();
		int inv_tax_first_mode = InvParamTool.getTaxFirstMode(pk_corp);
		for (int row = 0; row < rowCount; row++) {
			UFDouble nthrecemny = (UFDouble) detailModel.getValueAt(row, ReceiveDetailVO.NTHRECEMNY);
			UFDouble nthrecetaxmny = (UFDouble) detailModel.getValueAt(row, ReceiveDetailVO.NTHRECETAXMNY);
			if(inv_tax_first_mode == 0){
				//含税优先,通过含税计算无税
				nthrecemny = JZTaxRateUtil.calcMny(SafeCompute.div(ntaxrate, new UFDouble(100)), nthrecetaxmny);
			}
			else{
				//无税优先,通过无税计算含税
				nthrecetaxmny = JZTaxRateUtil.calcTaxMny(SafeCompute.div(ntaxrate, new UFDouble(100)), nthrecemny);
			}
			// 税额计算公式修改：税额=（收票金额—差额缴税扣除金额）\（1+税率）*税率
        	UFDouble ntaxmny = SafeCompute.multiply(SafeCompute.div(
					SafeCompute.sub(nthrecetaxmny, ndiffermny),
					SafeCompute.add(UFDouble.ONE_DBL, SafeCompute.div(ntaxrate, new UFDouble(100))))
					,SafeCompute.div(ntaxrate, new UFDouble(100)));
        	
			getCardPanel().setBodyValueAt(ntaxmny, row, ReceiveDetailVO.NTAXMNY);
			// 【差额缴税扣除金额】表体赋值
			getCardPanel().setBodyValueAt(ndiffermny, row, ReceiveDetailVO.NDIFFERMNY);
			getCardPanel().setBodyValueAt(SafeCompute.sub(nthrecetaxmny,ntaxmny), row, ReceiveDetailVO.NTHRECEMNY);
			getCardPanel().setBodyValueAt(nthrecetaxmny, row, ReceiveDetailVO.NTHRECETAXMNY);
			getCardPanel().setBodyValueAt(ntaxrate, row, ReceiveVO.NTAXRATE);
		}
	}
	/**
	 * 收票表体“税率”编辑后事件
	 */
	@Override
	public void cardBodyAfterEdit(BillEditEvent e) {
		if(ReceiveBVO.NTAXRATE.equals(e.getKey())){
			doBodyWhenEditBodyRate(e);
		}
	}
	private void doBodyWhenEditBodyRate(BillEditEvent e){
		BillCardPanel cardPanel = getClientUI().getBillCardPanel();
		String curTabcode = cardPanel.getCurrentBodyTableCode();//当前页签
		if(curTabcode.equals(ReceiveBVO.TABCODE)){
			String[] taxmnyFields = new String[]{ReceiveVO.NINVTAXMNY};
			String[] mnyFields = new String[]{ReceiveVO.NINVMNY};
			InvMnyTool.computeMnyByTaxrate(ReceiveVO.NTAXRATE, taxmnyFields, mnyFields, cardPanel, e);
		}
		else if(curTabcode.equals(ReceiveDetailVO.TABCODE)){
			String[] taxmnyFields = new String[]{ReceiveDetailVO.NTHRECETAXMNY};
			String[] mnyFields = new String[]{ReceiveDetailVO.NTHRECEMNY};
			InvMnyTool.computeMnyByTaxrate(ReceiveVO.NTAXRATE, taxmnyFields, mnyFields, cardPanel, e);
		}
	}
}