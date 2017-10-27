package nc.ui.jzinv.receive.action;

import nc.ui.jzinv.pub.action.InvoiceAction;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFBoolean;

/**
 * ÊÕÆ±ÐÞ¸Ä°´Å¥
 * @author mayyc
 *
 */
public class ReceEditAction extends InvoiceAction{

	public ReceEditAction(BillManageUI clientUI) {
		super(clientUI);
	}

	@Override
	public void doAction() throws Exception {
		setHeadValue();
	}
    private void setHeadValue(){
    	BillCardPanel cardPanel = getClientUI().getBillCardPanel();
		UFBoolean bisopenred  = new UFBoolean((String)cardPanel.getHeadItem(ReceiveVO.BISOPENRED).getValueObject());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_PROJECT).setEdit(!bisopenred.booleanValue());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_SUPPLIER).setEdit(!bisopenred.booleanValue());
		
		ReceiveVO headvo = (ReceiveVO)getClientUI().getBufferData().getCurrentVO().getParentVO();
		UFBoolean bisred = headvo.getBisred();
		if(bisred.booleanValue()){
			getClientUI().getBillCardPanel().getHeadTabbedPane().setSelectedIndex(1);		
		}
		else{
			getClientUI().getBillCardPanel().getHeadTabbedPane().setSelectedIndex(0);		
		}
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setNull(bisred.booleanValue());
	
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setEdit(bisred.booleanValue());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setNull(bisred.booleanValue());
		
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setEdit(bisred.booleanValue());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setNull(bisred.booleanValue());
		
		String pk_corp = getClientUI()._getCorp().getPk_corp();
		boolean bisupload = nc.vo.jzinv.param.InvParamTool.isToGLByBilltype(pk_corp, getClientUI().getUIControl().getBillType());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISUPLOAD).setValue(bisupload);
    }
}