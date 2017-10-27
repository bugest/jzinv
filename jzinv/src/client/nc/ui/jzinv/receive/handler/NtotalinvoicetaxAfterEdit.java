package nc.ui.jzinv.receive.handler;

import nc.ui.jzinv.pub.handler.InvCardEditHandler;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.trade.manage.BillManageUI;
import nc.vo.jzinv.receive.ReceiveVO;

/**
 * @ClassName: NtotalinvoicetaxAfterEdit
 * @Description: 票面总税金改变
 * @author linan linanb@yonyou.com
 * @date 2017-10-25 下午7:31:19
 * 
 */
public class NtotalinvoicetaxAfterEdit extends InvCardEditHandler {
	public NtotalinvoicetaxAfterEdit(BillManageUI clientUI) {
		super(clientUI);
	}

	@Override
	public void cardHeadAfterEdit(BillEditEvent e) {
		if (ReceiveVO.NTOTALINVOICETAX.equals(e.getKey())) {
			ReceiveEditTool.setNsurplussplittax(getClientUI());
			/*UFBoolean bissplit = new UFBoolean(getClientUI().getBillCardPanel()
					.getHeadItem(ReceiveVO.BISSPLIT).getValueObject()
					.toString());
			// 如果没选中的话，就不处理逻辑
			if (UFBoolean.TRUE.equals(bissplit)) {
				// 总税金
				UFDouble ntotalinvoicetax = e.getValue() == null ? UFDouble.ZERO_DBL
						: new UFDouble(e.getValue().toString());
				// 本次拆分税金
				UFDouble ntaxmny = getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NTAXMNY).getValueObject() == null ? UFDouble.ZERO_DBL
						: new UFDouble(getClientUI().getBillCardPanel()
								.getHeadItem(ReceiveVO.NTAXMNY)
								.getValueObject().toString());
				// 当前的表id
				String pk_receive = (String) getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.PK_RECEIVE).getValueObject();
				String vinvcode = (String) getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.VINVCODE).getValueObject();
				String vinvno = (String) getClientUI().getBillCardPanel()
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
					getClientUI().showErrorMessage("查询发票拆分情况报错!");
				}
				// 设置累计已拆分金额
				getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NCUMULATIVESPLITTAX)
						.setValue(sumTax);
				// 设置剩余拆分税金
				getClientUI().getBillCardPanel()
						.getHeadItem(ReceiveVO.NSURPLUSSPLITTAX)
						.setValue(ntotalinvoicetax.sub(ntaxmny).sub(sumTax));
			}*/
		}
	}

}
