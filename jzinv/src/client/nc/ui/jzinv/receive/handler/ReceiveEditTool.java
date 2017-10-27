package nc.ui.jzinv.receive.handler;

import java.util.List;

import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.jzinv.receive.IReceiveService;
import nc.ui.trade.manage.BillManageUI;

/** 
* @ClassName: ReceiveEditTool 
* @Description: 修改后处理的一些公共方法
* @author linan linanb@yonyou.com 
* @date 2017-10-26 上午10:27:23 
*  
*/
public class ReceiveEditTool {
	/** 
	* @Title: setNsurplussplittax 
	* @Description: 设置剩余拆分税金金额 
	* @param     
	* @return void    
	* @throws 
	*/
	public static void setNsurplussplittax(BillManageUI billManageUI) {
		//是否选中了拆分金额
		UFBoolean bissplit = new UFBoolean(billManageUI.getBillCardPanel()
				.getHeadItem(ReceiveVO.BISSPLIT).getValueObject()
				.toString());
		//只有选中拆分才会计算剩余拆分税额
		if (UFBoolean.TRUE.equals(bissplit)) {
			// 总税金
			UFDouble ntotalinvoicetax = billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NTOTALINVOICETAX).getValueObject() == null ? UFDouble.ZERO_DBL
					: new UFDouble(billManageUI.getBillCardPanel()
							.getHeadItem(ReceiveVO.NTOTALINVOICETAX)
							.getValueObject().toString());
			// 本次拆分税金
			UFDouble ntaxmny = billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NTAXMNY).getValueObject() == null ? UFDouble.ZERO_DBL
					: new UFDouble(billManageUI.getBillCardPanel()
							.getHeadItem(ReceiveVO.NTAXMNY)
							.getValueObject().toString());
			// 当前的表id
			String pk_receive = (String) billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_RECEIVE).getValueObject();
			String vinvcode = (String) billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.VINVCODE).getValueObject();
			String vinvno = (String) billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.VINVNO).getValueObject();
			// 设置累计已拆分金额
			UFDouble sumTax = UFDouble.ZERO_DBL; // todo
			try {
				List<ReceiveVO> receiveVOList = NCLocator
						.getInstance()
						.lookup(IReceiveService.class)
						.querySplitHeadVOsByCond(vinvcode, vinvno,
								pk_receive);
				if (receiveVOList != null && !receiveVOList.isEmpty()) {
					for (ReceiveVO receiveVO : receiveVOList) {
						sumTax = sumTax.add(receiveVO.getNtaxmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtaxmny());
					}
				}
			} catch (BusinessException be) {
				Logger.error("查询发票拆分情况报错！", be);
				billManageUI.showErrorMessage("查询发票拆分情况报错!");
			}
			// 设置累计已拆分金额
			billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX)
					.setValue(sumTax);
			// 设置剩余拆分税金
			billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NSURPLUSSPLITTAX)
					.setValue(ntotalinvoicetax.sub(ntaxmny).sub(sumTax));			
		}
	}
	
	
	/** 
	* @Title: setNsurplussplittax 
	* @Description: 设置剩余拆分税金金额  本次税金金额用 含税-不含税
	* @param     
	* @return void    
	* @throws 
	*/
	public static void setNsurplussplittaxBySubTax(BillManageUI billManageUI) {
		//是否选中了拆分金额
		UFBoolean bissplit = new UFBoolean(billManageUI.getBillCardPanel()
				.getHeadItem(ReceiveVO.BISSPLIT).getValueObject()
				.toString());
		//只有选中拆分才会计算剩余拆分税额
		if (UFBoolean.TRUE.equals(bissplit)) {
			// 总税金
			UFDouble ntotalinvoicetax = billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NTOTALINVOICETAX).getValueObject() == null ? UFDouble.ZERO_DBL
					: new UFDouble(billManageUI.getBillCardPanel()
							.getHeadItem(ReceiveVO.NTOTALINVOICETAX)
							.getValueObject().toString());
			// 本次拆分税金
			UFDouble ntaxmny = billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NINVTAXMNY).getValueObject() == null ? UFDouble.ZERO_DBL
					: new UFDouble(billManageUI.getBillCardPanel()
							.getHeadItem(ReceiveVO.NINVTAXMNY)
							.getValueObject().toString()).sub(
									billManageUI.getBillCardPanel()
									.getHeadItem(ReceiveVO.NINVMNY).getValueObject() == null ? UFDouble.ZERO_DBL
									: new UFDouble(billManageUI.getBillCardPanel()
											.getHeadItem(ReceiveVO.NINVMNY)
											.getValueObject().toString()) );
			// 当前的表id
			String pk_receive = (String) billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_RECEIVE).getValueObject();
			String vinvcode = (String) billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.VINVCODE).getValueObject();
			String vinvno = (String) billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.VINVNO).getValueObject();
			// 设置累计已拆分金额
			UFDouble sumTax = UFDouble.ZERO_DBL; // todo
			try {
				List<ReceiveVO> receiveVOList = NCLocator
						.getInstance()
						.lookup(IReceiveService.class)
						.querySplitHeadVOsByCond(vinvcode, vinvno,
								pk_receive);
				if (receiveVOList != null && !receiveVOList.isEmpty()) {
					for (ReceiveVO receiveVO : receiveVOList) {
						sumTax = sumTax.add(receiveVO.getNtaxmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtaxmny());
					}
				}
			} catch (BusinessException be) {
				Logger.error("查询发票拆分情况报错！", be);
				billManageUI.showErrorMessage("查询发票拆分情况报错!");
			}
			// 设置累计已拆分金额
			billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX)
					.setValue(sumTax);
			// 设置剩余拆分税金
			billManageUI.getBillCardPanel()
					.getHeadItem(ReceiveVO.NSURPLUSSPLITTAX)
					.setValue(ntotalinvoicetax.sub(ntaxmny).sub(sumTax));			
		}
	}
	
	/** 
	* @Title: bIsSplitAfterEdit 
	* @Description: 是否拆分修改事件 
	* @param @param bissplit    
	* @return void    
	* @throws 
	*/
	public static void bIsSplitAfterEditSetData(UFBoolean bissplit, BillManageUI billManagerUI) {
/*		if(!bissplit.booleanValue()){
			//清空相关字段 
			billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setValue(null);
			billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setValue(null);
			billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setValue(null);
			billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX).setValue(null);
			billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NSURPLUSSPLITTAX).setValue(null);
		}*/
		//无论选中和不选中先清空，后边有逻辑增加逻辑
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setValue(null);
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setValue(null);
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setValue(null);
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX).setValue(null);
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NSURPLUSSPLITTAX).setValue(null);
		//设置是否必填
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setNull(bissplit.booleanValue());
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setNull(bissplit.booleanValue());
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setNull(bissplit.booleanValue());
		//设置字段的编辑性
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY).setEdit(bissplit.booleanValue());
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY).setEdit(bissplit.booleanValue());
		billManagerUI.getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).setEdit(bissplit.booleanValue());	
	}	
}
