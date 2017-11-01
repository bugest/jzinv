package nc.ui.jzinv.vatproj.pub.handler;

import nc.ui.jzinv.vatproj.pub.handler.VatProjPubEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.vat1050.VatProjanalyVO;

import org.apache.commons.lang.StringUtils;
/**
 * ��Ŀ�༭�¼�
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
	 * ��Ŀ˰������ͷ����Ŀ���༭���¼�
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
	    		getClientUI().showErrorMessage("����Ŀû�м�˰��ʽ, ������ѡ��!");
	    		return;
	    	}
	    	if(null == getOrgsetVO()){
	    		getClientUI().getBillCardPanel().setHeadItem(VatProjanalyVO.PK_PROJECT, null);
	    		getClientUI().getBillCardPanel().setHeadItem(VatProjanalyVO.PK_PROJECTBASE, null);
	    		getClientUI().showErrorMessage("����Ŀû����˰��֯, ������ѡ��!");
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