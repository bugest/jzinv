package nc.ui.jzinv.receive.action;

import java.util.List;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.jzinv.receive.IReceiveService;
import nc.ui.jzinv.pub.action.InvoiceAction;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;

/**
 * 收票修改按钮
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
		//linan add
		setTaxSplitFields();
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
    
    /** 
     * @Title: setTaxSplitFields 
     * @Description: TODO 
     * @param     
     * @return void    
     * @throws 
     */
     private void setTaxSplitFields() {
 		String vinvcode = (String)getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VINVCODE).getValueObject();
 		String vinvno = (String)getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.VINVNO).getValueObject();
 		//如果发票号和发票code有一个为空就不能编辑，这种情况应该不会发生，但是还是判断下，以防万一
 		if(vinvcode == null || vinvcode.trim().equals("") || vinvno == null || vinvno.trim().equals("")) {
 			//设置非必填
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(false);
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(false);
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(false);
 			//设置字段的编辑性 都不可编辑
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setEdit(false);
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setEdit(false);
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setEdit(false);	
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setEdit(false);
 			return;
 		}
 		UFBoolean bissplit = new UFBoolean(getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).getValueObject().toString());
 		String pk_receive = (String)getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE).getValueObject();
 		//只要发票部位空，这个字段就可以填写
 		getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setEdit(true);
 		//如果发票号no code都有，就分情况设置可编辑性
 		//如果选中时，都是可以编辑的，并且是非空的
 		if(bissplit.equals(UFBoolean.TRUE)) {	
 			try {
 				List<ReceiveVO> receiveVOList = NCLocator.getInstance()
 						.lookup(IReceiveService.class)
 						.querySplitHeadVOsByCond(vinvcode, vinvno, pk_receive);
 				//判断是不是对于本发票只有一个单据
 				if(receiveVOList == null || receiveVOList.isEmpty()) {
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setEdit(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setEdit(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setEdit(bissplit.booleanValue());	
 					//getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.BISSPLIT).setEdit(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(bissplit.booleanValue());
 				} else {
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setEdit(false);
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setEdit(false);
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setEdit(false);	
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(bissplit.booleanValue());
 					getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(bissplit.booleanValue());					
 				}
 			} catch (BusinessException e) {
 				Logger.error("查询发票拆分情况报错！", e);
 				getClientUI().showErrorMessage("查询发票拆分情况报错!");
 			}

 		} else {
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setEdit(bissplit.booleanValue());
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setEdit(bissplit.booleanValue());
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setEdit(bissplit.booleanValue());	
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(bissplit.booleanValue());
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(bissplit.booleanValue());
 			getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(bissplit.booleanValue());			
 		}
 		

 	}
}