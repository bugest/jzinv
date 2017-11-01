package nc.ui.jzinv.vatproj.pub.handler;

import nc.ui.jzinv.vatproj.pub.handler.VatProjPubEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.vat1050.VatProjanalyVO;

import org.apache.commons.lang.StringUtils;
/**
 * 项目编辑事件
 * @author mayyc
 *
 */
public class ProjectEditHandler extends VatProjPubEditHandler{
	public String tabcode;
	public ProjectEditHandler(BillManageUI clientUI, String tabcode) {
		super(clientUI);
		this.tabcode = tabcode;
	}	

	/**
	 * 项目税金计算表头“项目”编辑后事件
	 */
	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		super.cardHeadAfterEdit(e);
		if(VatProjanalyVO.PK_PROJECT.equals(e.getKey())){
			doEdit(e);
		}
	}
	
	private void doEdit(BillEditEvent e){
		String vperiod = (String) getCardPanel().getHeadItem(VatProjanalyVO.VPERIOD).getValueObject();
		String pk_project = (String) getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECT).getValueObject();
	
	    if(!StringUtils.isEmpty(pk_project)){	    		    	
	    	if(null == getProjsetVO()){
	    		getClientUI().getBillCardPanel().setHeadItem(VatProjanalyVO.PK_PROJECT, null);
	    		getClientUI().getBillCardPanel().setHeadItem(VatProjanalyVO.PK_PROJECTBASE, null);
	    		getClientUI().showErrorMessage("该项目没有计税方式, 请重新选择!");
	    		return;
	    	}
	    	if(null == getOrgsetVO()){
	    		getClientUI().getBillCardPanel().setHeadItem(VatProjanalyVO.PK_PROJECT, null);
	    		getClientUI().getBillCardPanel().setHeadItem(VatProjanalyVO.PK_PROJECTBASE, null);
	    		getClientUI().showErrorMessage("该项目没有纳税组织, 请重新选择!");
	    		return;
	    	}
			try {
				setHeadItem(getProjsetVO(), getOrgsetVO());
				checkData(pk_project, vperiod, e);
				

			} catch (Exception e1) {
				e1.printStackTrace();
			}
	    }	
	}
	
}