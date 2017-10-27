package nc.ui.jzinv.receive.handler;

import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.jzinv.pub.tool.InvMnyTool;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveBVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
/**
 * 收票“税额”编辑事件
 * @author mayyc
 *
 */
public class NtaxmnyEditHandler extends InvCardEditHandler{

	public NtaxmnyEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}
	
	/**
	 * 收票表头“税额”编辑后事件
	 */
    @Override
	public void cardHeadAfterEdit(BillEditEvent e) {
    	if(ReceiveVO.NTAXMNY.equals(e.getKey())){
    		if(!checkBisValid(e)){
    			return;
    		}
    		BillCardPanel cardPanel = getClientUI().getBillCardPanel();
			String[] taxmnyFields = new String[]{ReceiveVO.NINVTAXMNY};
			String[] mnyFields = new String[]{ReceiveVO.NINVMNY};
			InvMnyTool.computeMnyByTaxmny(ReceiveVO.NTAXMNY, taxmnyFields, mnyFields, cardPanel, e);
			// linan add 设置剩余拆分税金
			ReceiveEditTool.setNsurplussplittax(getClientUI());
    	}
	}

    /**
     * 检查税额是否合法
     * @param e
     * @return
     */
    private boolean checkBisValid(BillEditEvent e){
    	UFBoolean bisRed = new UFBoolean((String)getCardPanel().getHeadItem(ReceiveVO.BISRED).getValueObject());
    	UFDouble ntaxmny =  new UFDouble((String)e.getValue());
    	if(bisRed.booleanValue()){   		
    		if(ntaxmny.compareTo(UFDouble.ZERO_DBL) > 0){
    			setOldTaxmnyValue(e);
    			getClientUI().showErrorMessage("该收票为红票, 税额必须为负数, 请重新输入!");
		    	return false;
    		}
    	}
    	else if(ntaxmny.compareTo(UFDouble.ZERO_DBL) < 0){
    		setOldTaxmnyValue(e);
    		getClientUI().showErrorMessage("该收票为蓝票, 税额必须为正数, 请重新输入!");
	    	return false;
    	}
    	return true;
    }
	/**
	 * 收票表体“税额”编辑后事件
	 */
	@Override
	public void cardBodyAfterEdit(BillEditEvent e) {
		if(ReceiveBVO.NTAXMNY.equals(e.getKey())){
			if(!checkBisValid(e)){    			return;
    		}
			BillCardPanel cardPanel = getClientUI().getBillCardPanel();
			String curTabcode = cardPanel.getCurrentBodyTableCode();//当前页签
			if(curTabcode.equals(ReceiveBVO.TABCODE)){
				String[] taxmnyFields = new String[]{ReceiveVO.NINVTAXMNY};
				String[] mnyFields = new String[]{ReceiveVO.NINVMNY};
				InvMnyTool.computeMnyByTaxmny(ReceiveVO.NTAXMNY, taxmnyFields, mnyFields, cardPanel, e);
			}
			else if(curTabcode.equals(ReceiveDetailVO.TABCODE)){
				String[] taxmnyFields = new String[]{ReceiveDetailVO.NTHRECETAXMNY};
				String[] mnyFields = new String[]{ReceiveDetailVO.NTHRECEMNY};
				InvMnyTool.computeMnyByTaxmny(ReceiveVO.NTAXMNY, taxmnyFields, mnyFields, cardPanel, e);
			}
		}
	}
	private void setOldTaxmnyValue(BillEditEvent e){
    	if(e.getPos() == 1){
			getCardPanel().setBodyValueAt(e.getOldValue(), e.getRow(), ReceiveBVO.NTAXMNY);
		}
		else{
			getCardPanel().setHeadItem(ReceiveVO.NTAXMNY, e.getOldValue());
		}
    }
}