package nc.ui.jzinv.receive.action;

import nc.ui.jzinv.pub.action.InvoiceAction;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFBoolean;

/**
 * 收票取消按钮
 * @author mayyc
 *
 */
public class ReceCancelAction extends InvoiceAction{

	public ReceCancelAction(BillManageUI clientUI) {
		super(clientUI);
	}

	@Override
	public void doAction() throws Exception {
		UFBoolean bisred = new UFBoolean((String)getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISRED).getValueObject());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setNull(bisred.booleanValue());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setNull(bisred.booleanValue());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setNull(bisred.booleanValue());
		// linan 20171025 增加取消时，根据是否拆分上颜色
		UFBoolean bissplit = new UFBoolean((String)getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).getValueObject());
		if(UFBoolean.TRUE.equals(bissplit)) {
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(true);
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(true);
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(true);
		} else {
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(false);
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(false);
			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(false);			
		}
	}

}
