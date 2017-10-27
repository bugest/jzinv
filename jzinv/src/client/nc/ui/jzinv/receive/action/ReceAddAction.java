package nc.ui.jzinv.receive.action;

import nc.bs.framework.common.NCLocator;
import nc.itf.jzinv.invpub.IJzinvQuery;
import nc.ui.jzinv.pub.action.InvoiceAction;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.jzinv.vat0505.VatTaxorgsetVO;
/**
 * 收票新增按钮
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
		
		//有些客户发票认证公司录入当前登录公司或置空，需求林云确定新增时该字段置空，不录入默认值
//		setAuthenOrg();
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
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setNull(false);
		
//		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.IREDAPLYREASON).setNull(false);
		
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setEdit(false);
		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VREDINFONO).setNull(false);
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
