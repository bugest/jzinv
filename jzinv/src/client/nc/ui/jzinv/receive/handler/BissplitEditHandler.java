package nc.ui.jzinv.receive.handler;

import java.util.List;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.jzinv.receive.IReceiveService;
import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;

/**
 * @ClassName: BissplitEditHandler
 * @Description: 是否拆分
 * @author linan linanb@yonyou.com
 * @date 2017-10-24 上午11:34:45
 * 
 */
public class BissplitEditHandler extends InvCardEditHandler {
	public BissplitEditHandler(BillManageUI clientUI) {
		super(clientUI);
	}

	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		if (ReceiveVO.BISSPLIT.equals(e.getKey())) {
			UFBoolean bissplit = new UFBoolean((String) e.getValue());
			ReceiveEditTool.bIsSplitAfterEditSetData(bissplit,
					getClientUI());
			//如果是选中拆分，则计算累计和未拆分金额
			//设置各种税金
			setTax(bissplit);	
		}
	}

	/**
	 * @Title: setTax
	 * @Description: 设置税金
	 * @param
	 * @return void
	 * @throws
	 */
	private void setTax(UFBoolean bissplit) {
		// 选中时计算累计金额
		if (bissplit.booleanValue()) {
			// 当前的表id
			String pk_receive = (String) getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.PK_RECEIVE).getValueObject();
			String vinvcode = (String) getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.VINVCODE).getValueObject();
			String vinvno = (String) getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.VINVNO).getValueObject();
			// 计算累计值查询条件为 = vinvcode = vinvno <> pk_receive
			try {
				List<ReceiveVO> receiveVOList = NCLocator.getInstance()
						.lookup(IReceiveService.class)
						.querySplitHeadVOsByCond(vinvcode, vinvno, pk_receive);
				UFBoolean isFirst = null;
				UFDouble sumTax = UFDouble.ZERO_DBL;
				if (receiveVOList == null || receiveVOList.isEmpty()) {
					getClientUI().getBillCardPanel()
							.getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX)
							.setValue(sumTax);
					isFirst = UFBoolean.TRUE;
					//是第一次拆分
				} else {
					//不是第一次拆分
					isFirst = UFBoolean.FALSE;
					for (ReceiveVO receiveVO : receiveVOList) {
						sumTax = sumTax.add(receiveVO.getNtaxmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtaxmny());
						// 不是第一次就用之前其他的给填上
						getClientUI().getBillCardPanel()
								.getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY)
								.setValue(receiveVO.getNtotalinvoiceamountmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtotalinvoiceamountmny());
						getClientUI()
								.getBillCardPanel()
								.getHeadItem(
										ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY)
								.setValue(receiveVO.getNtotalinvoiceamounttaxmny() == null ? UFDouble.ZERO_DBL : receiveVO.getNtotalinvoiceamounttaxmny());
						getClientUI().getBillCardPanel()
								.getHeadItem(ReceiveVO.NTOTALINVOICETAX)
								.setValue(receiveVO.getNtotalinvoicetax() == null ? UFDouble.ZERO_DBL : receiveVO.getNtotalinvoicetax());
					}
					getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX)
					.setValue(sumTax);	
				}
				//第一次就可以改变，第二次就不可改变
				getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTMNY)
						.setEdit(isFirst.booleanValue());
				getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTOTALINVOICEAMOUNTTAXMNY)
						.setEdit(isFirst.booleanValue());
				getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTOTALINVOICETAX)
						.setEdit(isFirst.booleanValue());
				//设置剩余拆分金额
				//总税金 - 已经拆分 - 本次拆分
				UFDouble ntotalinvoicetax = getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTOTALINVOICETAX).getValueObject() == null ? UFDouble.ZERO_DBL : 
							new UFDouble(getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTOTALINVOICETAX).getValueObject().toString()); 
				UFDouble ntaxmny = getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTAXMNY).getValueObject() == null ? UFDouble.ZERO_DBL : 
							new UFDouble(getClientUI().getBillCardPanel().getHeadItem(ReceiveVO.NTAXMNY).getValueObject().toString());
				getClientUI().getBillCardPanel()
				.getHeadItem(ReceiveVO.NSURPLUSSPLITTAX)
				.setValue(ntotalinvoicetax.sub(ntaxmny).sub(sumTax));
			} catch (BusinessException e) {
				Logger.error("查询发票拆分情况报错！", e);
				getClientUI().showErrorMessage("查询发票拆分情况报错!");
			}
		}
	}

}
