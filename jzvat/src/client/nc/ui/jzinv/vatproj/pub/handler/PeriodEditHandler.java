package nc.ui.jzinv.vatproj.pub.handler;

import nc.bs.logging.Logger;
import nc.ui.jzinv.vatproj.pub.handler.VatProjPubEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.vat1050.VatProjanalyVO;
import nc.vo.pub.lang.UFBoolean;

import org.apache.commons.lang.StringUtils;
/**
 * 期间编辑事件
 * @author mayyc
 *
 */
public class PeriodEditHandler extends VatProjPubEditHandler{
	/**
	 * 页签
	 */
	public String tabcode;
	public PeriodEditHandler(BillManageUI clientUI, String tabcode) {
		super(clientUI);
		this.tabcode = tabcode;
	}
	/**
	 * 项目税金计算表头“期间”编辑后事件
	 */
	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		super.cardHeadAfterEdit(e);
		try {
			super.checkPeriodValid(e);
		} catch (Exception e1) {
			Logger.error(e1, e1.getCause());
			return;
		}
		if(VatProjanalyVO.VPERIOD.equals(e.getKey())){
			doEdit(e);
		}
	}
	
	public void doEdit(BillEditEvent e){
		UFBoolean bisbegin = new UFBoolean((String)getCardPanel().getHeadItem(VatProjanalyVO.BISBEGIN).getValueObject());
		if(bisbegin.booleanValue()){
			return;
		}
		String pk_project = (String) getCardPanel().getHeadItem(VatProjanalyVO.PK_PROJECT).getValueObject();
		String vperiod = (String) getCardPanel().getHeadItem(VatProjanalyVO.VPERIOD).getValueObject();
		if(StringUtils.isEmpty(pk_project)){
			return;
		}
		try {
			setHeadItem(getProjsetVO(), getOrgsetVO());
			//linan addd
			setNtaxablesalemnyByProjectOrPeriodChange();
			checkData(pk_project, vperiod, e);			
		} catch (Exception e1) {
			Logger.error(e1, e1.getCause());
		}
	}
}