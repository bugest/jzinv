package nc.ui.jzinv.receive.action;

import nc.bs.framework.common.NCLocator;
import nc.itf.jzinv.invpub.IJzinvQuery;
import nc.ui.jzinv.pub.action.InvoiceAction;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.pub.IJzinvBillType;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
import nc.vo.pub.lang.UFBoolean;
/**
 * 收票红票按钮
 * @author mayyc
 *
 */
public class ReceRedAction extends InvoiceAction{

	public ReceRedAction(BillManageUI clientUI) {
		super(clientUI);
	}

	@Override
	public void doAction() throws Exception {
		//getClientUI().getBillCardPanel().getBodyTabbedPane().getIndexofTableCode(BillTabVO);
		//这样写不太安全，理论上应该用上面的方法更安全
		getClientUI().getBillCardPanel().getHeadTabbedPane().setSelectedIndex(1);		
		String pk_corp = getClientUI()._getCorp().getPk_corp();
		boolean bisupload = nc.vo.jzinv.param.InvParamTool.isToGLByBilltype(pk_corp, IJzinvBillType.JZINV_RECEIVE_MT);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISUPLOAD).setValue(bisupload);
		setAuthenOrg();
		setRedRelateField();
		//新增时颜色控制 linan
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(false);
		//新增时不能编辑
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setEdit(false);
		//新增时设置值为空
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setValue(false);
	}
	
	private void setRedRelateField(){
		getClientUI().getBillCardPanel().setHeadItem(ReceiveVO.BISRED, UFBoolean.TRUE);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setEdit(true);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setNull(true);
		
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setEdit(true);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setNull(true);
		
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setEdit(true);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setNull(true);
	}
	/*
	 * 设置认证财务组织
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
