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
	}

}
