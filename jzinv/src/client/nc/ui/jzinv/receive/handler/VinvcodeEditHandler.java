package nc.ui.jzinv.receive.handler;

import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFBoolean;

/** 
* @ClassName: VinvcodeEditHandler 
* @Description: 发票代号
* @author linan linanb@yonyou.com 
* @date 2017-10-24 下午1:55:10 
*  
*/
public class VinvcodeEditHandler extends InvCardEditHandler{
	public VinvcodeEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}
	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		if(ReceiveVO.VINVCODE.equals(e.getKey())){
			//只要改变，就先设置不拆分
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setValue(false);
			ReceiveEditTool.bIsSplitAfterEditSetData(UFBoolean.FALSE, getClientUI());
			String vinvcode = (String) getCardPanel().getHeadItem(ReceiveVO.VINVCODE).getValueObject();
			String vinvno = (String) getCardPanel().getHeadItem(ReceiveVO.VINVNO).getValueObject();
			//如果这code和no有一个为空就，就不能选择拆分，不能编辑
			if(vinvcode == null || vinvcode.trim().equals("") || vinvno == null || vinvno.trim().equals("")) {
				getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setEdit(false);
			} else {
				getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setEdit(true);
			}
		}
	}
}
