package nc.ui.jzinv.receive.handler;

import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFBoolean;

/** 
* @ClassName: VinvnoEditHandler 
* @Description: 发票号
* @author linan linanb@yonyou.com 
* @date 2017-10-24 下午1:55:24 
*  
*/
public class VinvnoEditHandler extends InvCardEditHandler{
	public VinvnoEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}
	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		if(ReceiveVO.VINVNO.equals(e.getKey())){
			//根据VINVNO和VINVCODE设置是否拆分的状态
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
			//如果编号不足就给补全 linan 20171027 此逻辑在保存中已经存在，因为涉及验证的问题，不保存时就应该显示正确的结果
			if (vinvno != null && !vinvno.trim().equals("")) {
				vinvno = String.format("%08d",Long.valueOf(vinvno));
				getClientUI().getBillCardPanel().setHeadItem(ReceiveVO.VINVNO, vinvno);
			}
		}
	}
	
}
