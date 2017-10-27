package nc.ui.jzinv.receive.action;

import nc.bs.framework.common.NCLocator;
import nc.itf.jzinv.invpub.IJzinvQuery;
import nc.ui.jzinv.pub.action.InvoiceAction;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.pub.IJzinvBillType;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
/**
 * ��Ʊ������ť
 * @author mayyc
 *
 */
public class ReceAddAction extends InvoiceAction{

	public ReceAddAction(BillManageUI clientUI) {
		super(clientUI);
	}

	@Override
	public void doAction() throws Exception {
		getClientUI().getBillCardPanel().getHeadTabbedPane().setSelectedIndex(0);
		String pk_corp = getClientUI()._getCorp().getPk_corp(); 
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_PAYCORP).setValue(pk_corp);
		//IJzinvBillType.JZINV_RECEIVE_MT
		boolean bisupload = nc.vo.jzinv.param.InvParamTool.isToGLByBilltype(pk_corp, getClientUI().getUIControl().getBillType());
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISUPLOAD).setValue(bisupload);
		
		//��Щ�ͻ���Ʊ��֤��˾¼�뵱ǰ��¼��˾���ÿգ���������ȷ������ʱ���ֶ��ÿգ���¼��Ĭ��ֵ
//		setAuthenOrg();
		setRedRelateField();
	}
	private void setRedRelateField(){
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setNull(false);
		
//		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setNull(false);
		
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setNull(false);
	}
	/*
	 * ������֤������֯
	 */
	private void setAuthenOrg() throws Exception{
		String pk_corp = getClientUI()._getCorp().getPk_corp();
		IJzinvQuery query = NCLocator.getInstance().lookup(IJzinvQuery.class);
		VatTaxorgsetVO orgSetVO = query.getTaxOrg(pk_corp);
		if(null != orgSetVO){
			getClientUI().getBillCardPanel().setHeadItem(ReceiveVO.PK_FINANCE, orgSetVO.getPk_taxorg());
			getClientUI().getBillCardPanel().setHeadItem(ReceiveVO.VTAXPAYERNUMBER, orgSetVO.getVtaxpayernumber());
		    
		}
	}
}