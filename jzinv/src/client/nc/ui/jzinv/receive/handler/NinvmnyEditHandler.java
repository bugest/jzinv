package nc.ui.jzinv.receive.handler;

import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveBVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;

/**
 * 收票“发票金额”编辑事件
 * @author mayyc
 *
 */
public class NinvmnyEditHandler extends InvCardEditHandler{

	public NinvmnyEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}
	/**
	 * 收票表头“发票金额”编辑后事件
	 */
	public void cardHeadAfterEdit(BillEditEvent e) {
		if(ReceiveVO.NINVMNY.equals(e.getKey()) || ReceiveVO.NINVTAXMNY.equals(e.getKey())){
			UFBoolean bisred = new UFBoolean((String)getClientUI().getBillCardPanel().
					getHeadItem(ReceiveVO.BISRED).getValueObject());
			UFDouble nbaseinvmny = new UFDouble((String)e.getValue());
			if(bisred.booleanValue() && nbaseinvmny.compareTo(UFDouble.ZERO_DBL) > 0){
				getClientUI().getBillCardPanel().setHeadItem( ReceiveBVO.NINVMNY, null);
				getClientUI().getBillCardPanel().setHeadItem( ReceiveBVO.NINVTAXMNY, null);
				getClientUI().showErrorMessage("该发票为红票, 金额必须为负, 请重新输入!");
				return;
			}
			else{
				if(nbaseinvmny.compareTo(UFDouble.ZERO_DBL) < 0 && !bisred.booleanValue()){
					getClientUI().getBillCardPanel().setHeadItem( ReceiveBVO.NINVMNY, null);
					getClientUI().getBillCardPanel().setHeadItem( ReceiveBVO.NINVTAXMNY, null);
					getClientUI().showErrorMessage("该发票为蓝票, 金额必须为正, 请重新输入!");
					return;
				}
			}
			super.cardHeadAfterEdit(e);
			int rows = getClientUI().getBillCardPanel().getBillModel(ReceiveDetailVO.TABCODE).getRowCount();
			if(rows == 0){
				setNewLineMny(e);			
			}
			//setBodyMnyValue(e);
			// linan add 设置剩余拆分税金
			ReceiveEditTool.setNsurplussplittax(getClientUI());
		}
	}

	/**
	 * 收票表体“发票金额”编辑后事件
	 */
	@Override
    public void cardBodyAfterEdit(BillEditEvent e){
    	if(ReceiveBVO.NINVMNY.equals(e.getKey()) || ReceiveBVO.NINVTAXMNY.equals(e.getKey())|| 
    			ReceiveDetailVO.NDIFFERMNY.equals(e.getKey())){
			UFBoolean bisred = new UFBoolean((String)getClientUI().getBillCardPanel().
					getHeadItem(ReceiveVO.BISRED).getValueObject());
			UFDouble nbaseinvmny = new UFDouble((String)e.getValue());
			if(nbaseinvmny.compareTo(UFDouble.ZERO_DBL) > 0 && bisred.booleanValue()){
				super.setOldValue(e);
				getClientUI().showErrorMessage("该发票为红票, 金额必须为负, 请重新输入!");
				return;
			}else if(nbaseinvmny.compareTo(UFDouble.ZERO_DBL) < 0 && !bisred.booleanValue()){
				super.setOldValue(e);
				getClientUI().showErrorMessage("该发票为蓝票, 金额必须为正, 请重新输入!");
				return;
			}else{
				super.cardBodyAfterEdit(e);
				//汇总表体第一个页签的发票金额到表头
				//ReceiveUtil.collectMnyToHead(getClientUI());
			}
		}
	}
    
	/**
	 * 设置新增行的值
	 * @param e
	 */
	private void setNewLineMny(BillEditEvent e){
		//表体增行, 并将发票金额赋值给本次收票金额
		getClientUI().getBillCardPanel().getBodyPanel(ReceiveDetailVO.TABCODE).addLine();
	}
	
	private void setBodyMnyValue(BillEditEvent e) {
		String pk_project = getHeadString(ReceiveVO.PK_PROJECT);
		String pk_projectbase = getHeadString(ReceiveVO.PK_PROJECTBASE);
		//判断表体是否有项目，没有项目在赋值
		String bodypkProject = getCardPanel().getBillModel(ReceiveDetailVO.TABCODE).getValueAt(0, ReceiveDetailVO.PK_PROJECT)
				==null?"":getCardPanel().getBillModel(ReceiveDetailVO.TABCODE).getValueAt(0, ReceiveDetailVO.PK_PROJECT).toString();
		if("".equals(bodypkProject)){
			getCardPanel().setBodyValueAt(pk_project, 0, 
					ReceiveDetailVO.PK_PROJECT, ReceiveDetailVO.TABCODE);
			getCardPanel().setBodyValueAt(pk_projectbase, 0, 
					ReceiveDetailVO.PK_PROJECTBASE, ReceiveDetailVO.TABCODE);
		}
		
		UFDouble nbaseinvmny = new UFDouble((String)getCardPanel().
				getHeadItem(ReceiveVO.NINVMNY).getValueObject());

		getCardPanel().setBodyValueAt(nbaseinvmny, 0, 
				ReceiveDetailVO.NTHRECEMNY, ReceiveDetailVO.TABCODE);
		UFDouble nbaseinvtaxmny = new UFDouble((String)getCardPanel().
				getHeadItem(ReceiveVO.NINVTAXMNY).getValueObject());
		getCardPanel().setBodyValueAt(nbaseinvtaxmny, 0, 
				ReceiveDetailVO.NTHRECETAXMNY, ReceiveDetailVO.TABCODE);
		//多税率改造 pengjf
//		UFDouble ntaxrate = new UFDouble((String)getCardPanel().getHeadItem(ReceiveVO.NTAXRATE).getValueObject());
//		getCardPanel().setBodyValueAt(ntaxrate, 0, 
//				ReceiveDetailVO.NTAXRATE, ReceiveDetailVO.TABCODE);
        UFDouble ntaxmny = new UFDouble((String)getCardPanel().getHeadItem(ReceiveVO.NTAXMNY).getValueObject());
        getCardPanel().setBodyValueAt(ntaxmny, 0, 
				ReceiveDetailVO.NTAXMNY, ReceiveDetailVO.TABCODE);
        
        // 取得表头【差额缴税扣除金额】字段
		UFDouble ndiffermny = getCardPanel().getHeadItem(ReceiveVO.NDIFFERMNY) == null ? null :
			new UFDouble((String)getCardPanel().getHeadItem(ReceiveVO.NDIFFERMNY).getValueObject());
		// 新增表体【差额缴税扣除金额】字段
		getCardPanel().setBodyValueAt(ndiffermny, 0,ReceiveDetailVO.NDIFFERMNY, ReceiveDetailVO.TABCODE);
        
        getCardPanel().getBillModel(ReceiveDetailVO.TABCODE).execLoadFormula();
	}

}