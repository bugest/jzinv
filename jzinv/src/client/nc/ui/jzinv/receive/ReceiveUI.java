package nc.ui.jzinv.receive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.jzinv.invpub.IJzinvQuery;
import nc.itf.uap.IUAPQueryBS;
import nc.jdbc.framework.processor.MapListProcessor;
import nc.ui.jzinv.inv0510.utils.OpenUtil;
import nc.ui.jzinv.othcont.pub.SupplementInfo;
import nc.ui.jzinv.pub.button.BillLinkBtnVO;
import nc.ui.jzinv.pub.button.LinkQueryPfBtnVO;
import nc.ui.jzinv.pub.buttonstate.RedInvBtnVO;
import nc.ui.jzinv.pub.buttonstate.RefAddCollBtnVO;
import nc.ui.jzinv.pub.tool.DeptPsnRela;
import nc.ui.jzinv.pub.ui.MultiChildBillManageUI;
import nc.ui.jzinv.receive.handler.BisredEditHandler;
import nc.ui.jzinv.receive.handler.BlueInvoiceEditHandler;
import nc.ui.jzinv.receive.handler.ContractEditHandler;
import nc.ui.jzinv.receive.handler.NinvmnyEditHandler;
import nc.ui.jzinv.receive.handler.NnumEditHandler;
import nc.ui.jzinv.receive.handler.NpriceEditHandler;
import nc.ui.jzinv.receive.handler.NtaxmnyEditHandler;
import nc.ui.jzinv.receive.handler.NtaxrateEditHandler;
import nc.ui.jzinv.receive.handler.NthrecemnyEditHandler;
import nc.ui.jzinv.receive.handler.OpenDateEditHandler;
import nc.ui.jzinv.receive.handler.ProjectEditHandler;
import nc.ui.jzinv.spider.button.ImageShowButton;
import nc.ui.pub.ButtonObject;
import nc.ui.pub.bill.BillCardBeforeEditListener;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillItemEvent;
import nc.ui.trade.base.IBillOperate;
import nc.ui.trade.bill.AbstractManageController;
import nc.ui.trade.bsdelegate.BusinessDelegator;
import nc.ui.trade.button.IBillButton;
import nc.ui.trade.buttonstate.RefBillBtnVO;
import nc.ui.trade.manage.ManageEventHandler;
import nc.vo.bd.b08.CustBasVO;
import nc.vo.jzinv.inv0503.ReceiveCollVO;
import nc.vo.jzinv.inv0510.OpenHVO;
import nc.vo.jzinv.param.InvParamTool;
import nc.vo.jzinv.pub.IJzinvBillType;
import nc.vo.jzinv.pub.IJzinvButton;
import nc.vo.jzinv.pub.ParamReader;
import nc.vo.jzinv.pub.utils.SafeCompute;
import nc.vo.jzinv.receive.ReceiveBVO;
import nc.vo.jzinv.receive.ReceiveDetailVO;
import nc.vo.jzinv.receive.ReceiveVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.SuperVO;
import nc.vo.pub.bill.BillTabVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
import nc.vo.trade.button.ButtonVO;

/**
 * 收票UI
 * @author mayyc
 *
 */
@SuppressWarnings("rawtypes")
public class ReceiveUI extends MultiChildBillManageUI implements ChangeListener, BillCardBeforeEditListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 扩展按钮状态--可用
	 */
	public final static int ENABLE = 1;
	private boolean isInfoSupply=false;
	
	public void setInfoSupply(boolean isInfoSupply) {
		this.isInfoSupply = isInfoSupply;
	}
	
	public boolean isInfoSupply() {
		return isInfoSupply;
	}

	public ReceiveUI() {
		super();
		// 控制按钮状态
		updateCheckBtnenble();
	}
	
	/**
	 * 更新按钮状态
	 */
	public void updateCheckBtnenble() {
		// 【传会计平台】按钮
		ButtonObject accountBtn = getButtonManager().getButton(IJzinvButton.BTN_ACCOUNT);
		// 【取消传凭证】按钮
		ButtonObject unaccountBtn = getButtonManager().getButton(IJzinvButton.BTN_UNACCOUNT);
		// 取得界面【认证已传凭证】数据
		String bisaccountflg = getHeadItemString(ReceiveVO.BISACCOUNTFLG);
		// 取得界面【认证状态】数据
		Integer iauthstatus = (Integer) getBillCardPanel().getHeadItem(ReceiveVO.IAUTHSTATUS).getValueObject();
		String o = getHeadItemString(ReceiveVO.IAUTHSTATUS);
		if (o != null && o.trim().length() > 0){
			iauthstatus = Integer.parseInt(o);
		}else{
			iauthstatus = -1;
		}
		if(bisaccountflg == null){
			bisaccountflg = "N";
		}
		// 按钮状态更新
		if(iauthstatus.intValue() == 2){
			if(bisaccountflg.equals("Y")||bisaccountflg.equals("true") ){
				accountBtn.setEnabled(false);
				unaccountBtn.setEnabled(true);
			}else if(bisaccountflg.equals("N")||bisaccountflg.equals("false") ){
				accountBtn.setEnabled(true);
				unaccountBtn.setEnabled(false);
			}else{
				accountBtn.setEnabled(false);
				unaccountBtn.setEnabled(false);
			}
		}else{
			accountBtn.setEnabled(false);
			unaccountBtn.setEnabled(false);
		}
		// 跟新按钮状态
		updateButton(accountBtn);
		updateButton(unaccountBtn);
	}

	@Override
	protected AbstractManageController createController() {
		return new ReceiveCtrl();
	}

	protected BusinessDelegator createBusinessDelegator() {
		return new ReceiveDelegator();
	}

	@Override
	protected ManageEventHandler createEventHandler() {
		return new ReceiveEH(this, getUIControl());
	}

	@Override
	protected ButtonVO[] initAssQryBtnVOs() {
		//添加单据联查按钮
		BillLinkBtnVO billLinkBtnVo = new BillLinkBtnVO();

		return new ButtonVO[] { new LinkQueryPfBtnVO().getButtonVO(), billLinkBtnVo.getButtonVO() };
	}

	@Override
	public void afterEdit(BillEditEvent e) {
		super.afterEdit(e);
		if (e.getPos() == 1) {
			afterBodyEdit(e);
		} else {
			afterHeadEdit(e);
		}
	}

	@Override
	protected void initPrivateButton() {
		super.initPrivateButton();
		addRefBtn();
		addRedBtn();
		addPrivateButton(new ImageShowButton().getButtonVO());//影像调用按钮
		addPrivateButton(new SupplementInfo().getButtonVO());//信息补充
	}

	public void setBuyerOrSellerInfo(String infoPK, String flag) {
		// 购方纳税人识别号  OpenHVO.VTAXCSTMNUMBER
		//购方地址及电话OpenHVO.VCSTMPHONE


		try {
			if ("buyer".equals(flag)) {
				Map custBasVOAndBankInfo = getJzinvQuery().getCustBasVOAndBankInfo(null, infoPK);
				if (custBasVOAndBankInfo != null) {

					CustBasVO custBasVO = (CustBasVO) custBasVOAndBankInfo.get("custBasVO");
					getBillCardPanel().setHeadItem(OpenHVO.VTAXPAYERNUMBER, custBasVO.getTaxpayerid());
					//购方开户银行及银行帐号  OpenHVO.VCSTMBANK
					getBillCardPanel().setHeadItem(OpenHVO.VBANKACCOUNT, custBasVOAndBankInfo.get("bankInfo"));
					getBillCardPanel().setHeadItem("pk_cubasdoc", custBasVO.getPk_cubasdoc());
					if (custBasVO != null)
						getBillCardPanel().setHeadItem(
								"vtaxpayerphone",
								OpenUtil.null2EmptyStrWithDouHao(new Object[] { custBasVO.getConaddr(),
										custBasVO.getPhone1() }));

				}
			} else if ("seller".equals(flag)) {

				Map custManVOAndBankInfo = getJzinvQuery().getCustManVOAndBankInfo(infoPK, _getCorp().getPk_corp());
				if (custManVOAndBankInfo != null) {

					CustBasVO cumandocVO = (CustBasVO) custManVOAndBankInfo.get("cumandocVO");
					if (cumandocVO != null) {
						getBillCardPanel().setHeadItem("vtaxsuppliernumber", cumandocVO.getTaxpayerid());
						/*getBillCardPanel().setHeadItem(
								"vtaxpayerphone",
								OpenUtil.null2EmptyStrWithDouHao(new Object[] { cumandocVO.getConaddr(),
										cumandocVO.getPhone1() }));*/
						getBillCardPanel().setHeadItem("vsupbankaccount", custManVOAndBankInfo.get("bankInfo"));
//						getBillCardPanel().setHeadItem(OpenHVO.NTAXRATE, cumandocVO.getNrate());

					}
				}
			} else {
				Map custManInfoByCrop = getJzinvQuery().getCustManInfoByCrop(_getCorp());
				if (custManInfoByCrop != null) {

					CustBasVO cumandocVO = (CustBasVO) custManInfoByCrop.get("cumandocVO");
					if (cumandocVO != null) {
						getBillCardPanel().setHeadItem("vtaxpayername", custManInfoByCrop.get("pk_custmandoc"));
						getBillCardPanel().setHeadItem(OpenHVO.VTAXPAYERNUMBER, cumandocVO.getTaxpayerid());
						getBillCardPanel().setHeadItem(
								"vtaxpayerphone",
								OpenUtil.null2EmptyStrWithDouHao(new Object[] { cumandocVO.getConaddr(),
										cumandocVO.getPhone1() }));
						getBillCardPanel().setHeadItem(OpenHVO.VBANKACCOUNT, custManInfoByCrop.get("bankInfo"));
						//getBillCardPanel().setHeadItem(OpenHVO.NTAXRATE, cumandocVO.getNrate());
						getBillCardPanel().setHeadItem("pk_cubasdoc", cumandocVO.getPk_cubasdoc());

					}
				}
			}

		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
		}
	}

	/**
	 * 参照按钮
	 */
	private void addRefBtn() {
		RefAddCollBtnVO refAddBtn = new RefAddCollBtnVO();
		refAddBtn.getButtonVO().setExtendStatus(new int[] { ENABLE });
		addPrivateButton(refAddBtn.getButtonVO());
		ButtonVO refBtnVO = new RefBillBtnVO().getButtonVO();
		refBtnVO.setChildAry(new int[] { IJzinvButton.REFADDCOLLBTN });
		addPrivateButton(refBtnVO);
	}

	/**
	 * 红票按钮
	 */
	private void addRedBtn() {
		RedInvBtnVO redBtn = new RedInvBtnVO();
		redBtn.getButtonVO().setExtendStatus(new int[] { ENABLE });
		addPrivateButton(redBtn.getButtonVO());
	}

	/**
	 * 表头编辑后事件
	 * @param e
	 */

	public void afterHeadEdit(BillEditEvent e) {
		try { 
		if (e.getKey().equals("pk_deptdoc")) {// 经办部门
			getDeptPsnRela().setPsnByDept();
			//			getBillCardPanel().execHeadEditFormulas();
		} else if (e.getKey().equals("pk_psndoc")) {// 经办人
			getDeptPsnRela().setDeptByPsn();
			//			getBillCardPanel().execHeadEditFormulas();
		} else if (ReceiveVO.BISRED.equals(e.getKey())) {
			//getBillCardPanel().execHeadEditFormulas();
		} else if (ReceiveVO.BISRED.equals(e.getKey())) {
			new BisredEditHandler(this).cardHeadAfterEdit(e);
		} else if (ReceiveVO.PK_RECEIVE_REF.equals(e.getKey())) {
			new BlueInvoiceEditHandler(this).cardHeadAfterEdit(e);
		} else if (ReceiveVO.PK_PROJECT.equals(e.getKey())) {
			new ProjectEditHandler(this).cardHeadAfterEdit(e);
		} else if (ReceiveVO.PK_SUPPLIER.equals(e.getKey())) {
//			new SupplierEditHandler(this).cardHeadAfterEdit(e);
//			String pk_supplierbase = (String) getBillCardPanel().getHeadItem(ReceiveVO.PK_SUPPLIERBASE)
//					.getValueObject();
//			this.setBuyerOrSellerInfo(pk_supplierbase, "seller");
			//--------------------start----------------------
		    String pk_supplier = (String)getBillCardPanel().getHeadItem(ReceiveVO.PK_SUPPLIER).getValueObject();
		    String pk_supplier_base = (String)getBillCardPanel().getHeadItem(ReceiveVO.PK_SUPPLIERBASE).getValueObject();
		    if(pk_supplier_base==null){
		    	pk_supplier_base=getCumandocAction(pk_supplier);
		    }
		     //供应商名称
		     Object vtaxsuppliename= getBillCardPanel().getHeadItem("vsuppliername").getValueObject();
		     //销售方纳税人识别号
		     Object vtaxsuppliernumber= getBillCardPanel().getHeadItem(ReceiveVO.VTAXSUPPLIERNUMBER).getValueObject();
		     //销售方地址电话vsupplierphone	销售方地址及电话
		     Object vtaxsupplieaddr= getBillCardPanel().getHeadItem(ReceiveVO.VSUPPLIERPHONE).getValueObject();
		     //销售方开户银行及银行账号
		     Object vsupbankaccount= getBillCardPanel().getHeadItem(ReceiveVO.VSUPBANKACCOUNT).getValueObject();
		    hashmap=getCubasdocAction(pk_supplier_base);
		    //供应商名称
		     if(vtaxsuppliename==null||vtaxsuppliename.equals("")){
		    	if(hashmap.get(pk_supplier_base)!=null){
		    		HashMap value=hashmap.get(pk_supplier_base);
		    		getBillCardPanel().setHeadItem("vtaxsuppliename",value.get("custname")==null?"":value.get("custname"));
		    	}
		     }//销售方纳税人识别号
		     if(vtaxsuppliernumber==null||vtaxsuppliernumber.equals("")){
			    	if(hashmap.get(pk_supplier_base)!=null){
			    		HashMap value=hashmap.get(pk_supplier_base);
			    		getBillCardPanel().setHeadItem(ReceiveVO.VTAXSUPPLIERNUMBER,value.get("taxpayerid")==null?"":value.get("taxpayerid"));
			    	}
		     }//销售方地址电话
		     if(vtaxsupplieaddr==null||vtaxsupplieaddr.equals("")||vtaxsupplieaddr.equals("conaddr,phone1")){
			    	if(hashmap.get(pk_supplier_base)!=null){
			    		HashMap value=hashmap.get(pk_supplier_base);
			    		Object conaddr=value.get("conaddr");
			    		Object phone1=value.get("phone1");
			    		String message=getVtaxsupplieaddr(conaddr,phone1);
			    		getBillCardPanel().setHeadItem(ReceiveVO.VSUPPLIERADDRESS,message==null?"":message);
			    	}
		     }//销售方开户银行及银行账号项目主键	
		     if(vsupbankaccount==null||vsupbankaccount.equals("")){
					String	value = getJzinvQuery().getAccountAndAccountNum(pk_supplier_base);
					getBillCardPanel().setHeadItem(ReceiveCollVO.VTAXSUPPLIEACCOUNT,value); 
		     }

		} else if (ReceiveVO.NINVMNY.equals(e.getKey()) || ReceiveVO.NINVTAXMNY.equals(e.getKey())) {
			new NinvmnyEditHandler(this).cardHeadAfterEdit(e);
		}else if (ReceiveVO.NDIFFERMNY.equals(e.getKey())) {
			try {
				// 表头【差额缴税扣除金额】编辑后事件
				afterEditDifferMny();
			} catch (BusinessException e1) {
				Logger.error(e1.getMessage());
			}
		} else if (ReceiveVO.NTAXRATE.equals(e.getKey())) {
			new NtaxrateEditHandler(this).cardHeadAfterEdit(e);
		} else if (ReceiveVO.NTAXMNY.equals(e.getKey())) {
			new NtaxmnyEditHandler(this).cardHeadAfterEdit(e);
		} else if (ReceiveVO.PK_CONTRACT.equals(e.getKey())) {
			new ContractEditHandler(this).cardHeadAfterEdit(e);
		} else if (ReceiveVO.DOPENDATE.equals(e.getKey())) {
			new OpenDateEditHandler(this).cardHeadAfterEdit(e);
		} else if ("vtaxpayername".equals(e.getKey())) {
			//购方名称
			String pk_supplierbase = (String) getBillCardPanel().getHeadItem(ReceiveVO.VTAXPAYERNAME).getValueObject();
			String pk_corp = (String) getBillCardPanel().getHeadItem(ReceiveVO.PK_CORP).getValueObject();
			String pk_basecorp=pk_supplierbase+pk_corp;
             corpmap=getBdcorpAction(pk_supplierbase,pk_corp);
//			this.setBuyerOrSellerInfo(pk_supplierbase, "buyer");
			//“购方纳税人识别号”、“购方地址及电话”、“购方开户银行及银行账号”
				//购方地址及电话
			String vtaxpayerphone = (String) getBillCardPanel().getHeadItem(ReceiveVO.VTAXPAYERPHONE).getValueObject();
			if(vtaxpayerphone==null||vtaxpayerphone.equals("")||vtaxpayerphone.equals("conaddr,phone1")){
		    	if(corpmap.get(pk_basecorp)!=null){
		    		HashMap value=corpmap.get(pk_basecorp);
		    		Object conaddr=value.get("postaddr");
		    		Object phone1=value.get("phone1");
		    		String message=getVtaxsupplieaddr(conaddr,phone1);
		    		getBillCardPanel().setHeadItem(ReceiveVO.VTAXPAYERPHONE,message==null?"":message);
		    	}
			}
			//购方开户银行及银行账号
			String vbankaccount = (String) getBillCardPanel().getHeadItem(ReceiveVO.VBANKACCOUNT).getValueObject();
			if(vbankaccount==null||vbankaccount.equals("")){
				Map custBasVOAndBankInfo = getJzinvQuery().getCustBasVOAndBankInfo(null, pk_supplierbase);
				if (custBasVOAndBankInfo != null) {
					CustBasVO custBasVO = (CustBasVO) custBasVOAndBankInfo.get("custBasVO");
					//购方开户银行及银行帐号  OpenHVO.VCSTMBANK
					getBillCardPanel().setHeadItem(OpenHVO.VBANKACCOUNT, custBasVOAndBankInfo.get("bankInfo"));
					getBillCardPanel().setHeadItem("pk_cubasdoc", custBasVO.getPk_cubasdoc());
 			   }
			}
			//购方纳税人识别号
			String vtaxpayernumber = (String) getBillCardPanel().getHeadItem(ReceiveVO.VTAXPAYERNUMBER).getValueObject();
			if(vtaxpayernumber==null||vtaxpayernumber.equals("")){
				if(corpmap.get(pk_basecorp)!=null){
		    		HashMap value=corpmap.get(pk_basecorp);
		    		Object taxcode=value.get("taxpayerid");
		    		getBillCardPanel().setHeadItem(ReceiveVO.VTAXPAYERNUMBER,taxcode==null?"":taxcode);
		    	}
			}

	}// 发票类型编辑后事件
		else if (ReceiveVO.IINVTYPE.equals(e.getKey())) {
			// 取得表头的【发票类型】
			Integer iinvtpe = (Integer) getBillCardPanel().getHeadItem(ReceiveVO.IINVTYPE).getValueObject();
			if(iinvtpe != 0 && iinvtpe != 1){
				getBillCardPanel().getHeadItem(ReceiveVO.VTAXPERIOD).setEdit(true);
			}else{
				getBillCardPanel().getHeadItem(ReceiveVO.VTAXPERIOD).setEdit(false);
			}
			getBillCardPanel().getHeadItem(ReceiveVO.VTAXPERIOD).setValue(null);
			getBillCardPanel().execHeadLoadFormulas();
		}
		} catch (Exception ex) {
			Logger.error("编辑控件后触发事件过程中发生异常", ex);
			showErrorMessage("编辑控件后触发事件过程中发生异常");
		}
	}
	Map<String, HashMap>  corpmap=new HashMap<String,HashMap>();
	private Map<String, HashMap> getBdcorpAction(String pk_supplierbase, String pk_corp) throws BusinessException {
		String pk_basecorp=pk_supplierbase+pk_corp;
		  if(corpmap!=null&&corpmap.get(pk_basecorp)!=null){
			 return corpmap;
		   }else{
				StringBuffer  strbuffer=new StringBuffer();
				strbuffer.append(" select distinct bd_cubasdoc.custcode,");
				strbuffer.append(" bd_cubasdoc.custname,  bd_cumandoc.cmnecode,");
				strbuffer.append(" bd_cubasdoc.custshortname, ");
				strbuffer.append(" bd_cubasdoc.taxpayerid, ");
				strbuffer.append(" bd_cubasdoc.conaddr, bd_cubasdoc.phone1, ");
				strbuffer.append(" bd_cumandoc.pk_cumandoc,");
				strbuffer.append(" bd_cubasdoc.pk_cubasdoc, ");
				strbuffer.append(" bd_cubasdoc.pk_areacl ");
				strbuffer.append("  from bd_cumandoc  inner join bd_cubasdoc ");
				strbuffer.append("  on bd_cumandoc.pk_cubasdoc = bd_cubasdoc.pk_cubasdoc ");
				strbuffer.append("  where (bd_cumandoc.pk_corp = '"+pk_corp+"' AND ");
				strbuffer.append("  (bd_cumandoc.custflag = '0' OR bd_cumandoc.custflag = '2')) ");
				strbuffer.append("  and (bd_cumandoc.sealflag is null AND ");
				strbuffer.append(" (frozenflag = 'N' or frozenflag is null)) ");
				strbuffer.append("  and  bd_cumandoc.pk_cumandoc='"+pk_supplierbase+"' ");
				strbuffer.append("  order by bd_cubasdoc.custcode ");
				ArrayList arraylist = (ArrayList) getQueryBS().executeQuery(String.valueOf(strbuffer),
	                    new MapListProcessor());
				if(arraylist!=null&&arraylist.size()>0){
					HashMap docmap=(HashMap) arraylist.get(0);
					corpmap.put(pk_basecorp, docmap);
				}
			}
			return corpmap;
	}

	private String getCumandocAction(String pk_supplier) throws BusinessException {
		StringBuffer strbuffer=new StringBuffer();
		strbuffer.append("select pk_cubasdoc  from  bd_cumandoc pk_cumandoc='"+pk_supplier+"' and isnull(dr,0)=0 ");
		ArrayList arraylist = (ArrayList) getQueryBS().executeQuery(String.valueOf(strbuffer),
                new MapListProcessor());
		String pk_supplier_base=null;
		if(arraylist!=null&&arraylist.size()>0){
			HashMap docmap=(HashMap) arraylist.get(0);
			pk_supplier_base=String.valueOf(docmap.get("pk_cubasdoc"));
		
		}
		return pk_supplier_base;
	}
	private String getVtaxsupplieaddr(Object conaddr, Object phone1) {
	   if(conaddr!=null&&phone1==null){
			return conaddr.toString();
		}else if(conaddr==null&&phone1!=null){
			return phone1.toString();
		}else if(conaddr!=null&&phone1!=null){
			return conaddr.toString()+";"+phone1.toString();
		}else
		return null;
	}
	Map<String, HashMap>  hashmap=new HashMap<String,HashMap>();
	private Map<String, HashMap> getCubasdocAction(String pk_supplier_base) throws BusinessException {	
		if(hashmap!=null&&hashmap.get(pk_supplier_base)!=null){
         return hashmap;
		}else{
			StringBuffer  strbuffer=new StringBuffer();
			strbuffer.append(" select taxpayerid,conaddr,phone1,custname  from  bd_cubasdoc ");
			strbuffer.append(" where  pk_cubasdoc='"+pk_supplier_base+"' and isnull(dr,0)=0");
			ArrayList arraylist = (ArrayList) getQueryBS().executeQuery(String.valueOf(strbuffer),
                    new MapListProcessor());
			if(arraylist!=null&&arraylist.size()>0){
				HashMap docmap=(HashMap) arraylist.get(0);
				hashmap.put(pk_supplier_base, docmap);
			}
			
		}
		return hashmap;
	}
	private IJzinvQuery ijzinvquery = null;
	public IJzinvQuery getJzinvQuery() {
		if (ijzinvquery == null) {
			ijzinvquery = NCLocator.getInstance().lookup(IJzinvQuery.class);
		}
		return ijzinvquery;
	}
	private IUAPQueryBS iuapQueryBS = null;
	public IUAPQueryBS getQueryBS() {
		if (iuapQueryBS == null) {
			iuapQueryBS = NCLocator.getInstance().lookup(IUAPQueryBS.class);
		}
		return iuapQueryBS;
	}
	/**
	 * 表头【差额缴税扣除金额】编辑后事件
	 * 
	 * @throws BusinessException
	 */
	public void afterEditDifferMny() throws BusinessException{
		BillCardPanel billCardPanel = getBillCardPanelWrapper().getBillCardPanel();
		// 取得表头【差额缴税扣除金额】
		UFDouble ndiffermny = billCardPanel.getHeadItem(ReceiveVO.NDIFFERMNY) == null ? null :
			new UFDouble((String)billCardPanel.getHeadItem(ReceiveVO.NDIFFERMNY).getValueObject());
		// 取得表头【发票金额】
		UFDouble ninvtaxmny = billCardPanel.getHeadItem(ReceiveVO.NINVTAXMNY) == null ? null :
			new UFDouble((String)billCardPanel.getHeadItem(ReceiveVO.NINVTAXMNY).getValueObject());
		// 取得表头【税率】
		UFDouble ntaxrate = billCardPanel.getHeadItem(ReceiveVO.NTAXRATE) == null ? null :
			new UFDouble((String)billCardPanel.getHeadItem(ReceiveVO.NTAXRATE).getValueObject());
		
		UFDouble tempntaxmny = SafeCompute.sub(ninvtaxmny,ndiffermny);// （收票金额—差额缴税扣除金额）
		UFDouble tempninvtaxmny = SafeCompute.add(new UFDouble(1), SafeCompute.div(ntaxrate, new UFDouble(100)));// （1+税率）
		// 税额计算公式修改：税额=（收票金额—差额缴税扣除金额）\（1+税率）*税率
		UFDouble ntaxmny = SafeCompute.multiply(SafeCompute.div(tempntaxmny,tempninvtaxmny),
				SafeCompute.div(ntaxrate, new UFDouble(100)));
		//给税额赋值
		billCardPanel.setHeadItem(ReceiveVO.NTAXMNY, ntaxmny);
		// 表头金额无税的赋值
		billCardPanel.setHeadItem(ReceiveVO.NINVMNY, SafeCompute.sub(ninvtaxmny,ntaxmny));
	}

	/**
	 * 表体编辑后事件
	 * @param e
	 */
	public void afterBodyEdit(BillEditEvent e) {
		String currentTabCode = getBillCardPanel().getCurrentBodyTableCode();
		if (currentTabCode.equals(ReceiveBVO.TABCODE)) {
			if (ReceiveBVO.NINVMNY.equals(e.getKey()) || ReceiveBVO.NINVTAXMNY.equals(e.getKey())) {
				new NinvmnyEditHandler(this).cardBodyAfterEdit(e);
			}else if (ReceiveBVO.NDIFFERMNY.equals(e.getKey())) {
				try {
					// 表体（纸质发票内容）【差额缴税扣除金额】编辑后事件
					afterEditRecBDifferMny();
				} catch (BusinessException e1) {
					Logger.error(e1.getMessage());
				}
			} else if (ReceiveBVO.NTAXRATE.equals(e.getKey())) {
				new NtaxrateEditHandler(this).cardBodyAfterEdit(e);
			} else if (ReceiveBVO.NTAXMNY.equals(e.getKey())) {
				new NtaxmnyEditHandler(this).cardBodyAfterEdit(e);
			} else if (ReceiveBVO.NPRICEMNY.equals(e.getKey()) || ReceiveBVO.NPRICETAXMNY.equals(e.getKey())) {
				new NpriceEditHandler(this).cardBodyAfterEdit(e);
			} else if (ReceiveBVO.NNUM.equals(e.getKey())) {
				new NnumEditHandler(this).cardBodyAfterEdit(e);
			}
		} else if (currentTabCode.equals(ReceiveDetailVO.TABCODE)) {
			if (ReceiveDetailVO.NTHRECEMNY.equals(e.getKey()) || ReceiveDetailVO.NTHRECETAXMNY.equals(e.getKey())) {
				new NthrecemnyEditHandler(this).cardBodyAfterEdit(e);
			}else if (ReceiveDetailVO.NDIFFERMNY.equals(e.getKey())) {
				try {
					// 表体（发票依据）【差额缴税扣除金额】编辑后事件
					afterEditRecDetaDifferMny();
				} catch (BusinessException e1) {
					Logger.error(e1.getMessage());
				}
			} else if (ReceiveDetailVO.VCONTRACTCODE.equals(e.getKey())) {
				new ContractEditHandler(this).cardBodyAfterEdit(e);
			} else if (ReceiveBVO.NTAXRATE.equals(e.getKey())) {
				new NtaxrateEditHandler(this).cardBodyAfterEdit(e);
			} else if (ReceiveDetailVO.NTAXMNY.equals(e.getKey())) {
				new NtaxmnyEditHandler(this).cardBodyAfterEdit(e);
			}
		}
		
		try {
			sumReceDetailMny(getBillCardPanel());
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage());
		}
	}
	
	/**
	 * 表体（发票依据）【差额缴税扣除金额】编辑后事件
	 * 
	 * @throws BusinessException
	 */
	public void afterEditRecDetaDifferMny() throws BusinessException{
		BillCardPanel billCardPanel = getBillCardPanelWrapper().getBillCardPanel();
		// 取得【纸质发票内容】表体数据
		SuperVO[] receiveBVO = (SuperVO[]) billCardPanel.getBillModel(ReceiveDetailVO.TABCODE).getBodyValueVOs(
				ReceiveDetailVO.class.getName());
		if(receiveBVO != null || receiveBVO.length != 0){
			// 取得当前选中行数 
			int row = billCardPanel.getBillTable().getSelectedRow();
			// 表体（发票依据）【差额缴税扣除金额】
			UFDouble ndiffermny = new UFDouble(receiveBVO[row].getAttributeValue(ReceiveDetailVO.NDIFFERMNY).toString());
			// 表体（发票依据）【本次收票金额】
			UFDouble ninvtaxmny = new UFDouble(receiveBVO[row].getAttributeValue(ReceiveDetailVO.NTHRECETAXMNY).toString());
			// 表体（发票依据）【税率】
			UFDouble ntaxrate = new UFDouble(receiveBVO[row].getAttributeValue(ReceiveDetailVO.NTAXRATE).toString());
				
			// 【税额】计算
			UFDouble tempntaxmny = SafeCompute.sub(ninvtaxmny,ndiffermny);// （收票金额—差额缴税扣除金额）
			UFDouble tempninvtaxmny = SafeCompute.add(new UFDouble(1), SafeCompute.div(ntaxrate, new UFDouble(100)));// （1+税率）
			// 税额计算公式修改：税额=（收票金额—差额缴税扣除金额）\（1+税率）*税率
			UFDouble ntaxmny = SafeCompute.multiply(SafeCompute.div(tempntaxmny,tempninvtaxmny),
					SafeCompute.div(ntaxrate, new UFDouble(100)));
			// 给税额赋值
			billCardPanel.getBillModel(ReceiveDetailVO.TABCODE).setValueAt(ntaxmny, row, ReceiveDetailVO.NTAXMNY);
			// 表头金额无税的赋值
			billCardPanel.getBillModel(ReceiveDetailVO.TABCODE).setValueAt(SafeCompute.sub(ninvtaxmny,ntaxmny),
					row, ReceiveDetailVO.NTHRECEMNY);
		}
	}

	/**
	 * 表体（纸质发票内容）【差额缴税扣除金额】编辑后事件
	 * 
	 * @throws BusinessException
	 */
	public void afterEditRecBDifferMny() throws BusinessException{
		BillCardPanel billCardPanel = getBillCardPanelWrapper().getBillCardPanel();
		// 取得【纸质发票内容】表体数据
		SuperVO[] receiveBVO = (SuperVO[]) billCardPanel.getBillModel(ReceiveBVO.TABCODE).getBodyValueVOs(
				ReceiveBVO.class.getName());
		if(receiveBVO != null || receiveBVO.length != 0){
			// 取得当前选中行数 
			int row = billCardPanel.getBillTable().getSelectedRow();
			// 表体（纸质发票内容）【差额缴税扣除金额】
			UFDouble ndiffermny = receiveBVO[row].getAttributeValue(ReceiveBVO.NDIFFERMNY) == null ? null :
				new UFDouble(receiveBVO[row].getAttributeValue(ReceiveBVO.NDIFFERMNY).toString());
			// 表体（纸质发票内容）【发票金额】
			UFDouble ninvtaxmny = receiveBVO[row].getAttributeValue(ReceiveBVO.NINVTAXMNY) == null ? null :
				new UFDouble((String)receiveBVO[row].getAttributeValue(ReceiveBVO.NINVTAXMNY).toString());
			// 表体（纸质发票内容）【税率】
			UFDouble ntaxrate = receiveBVO[row].getAttributeValue(ReceiveBVO.NTAXRATE) == null ? null :
				new UFDouble((String)receiveBVO[row].getAttributeValue(ReceiveBVO.NTAXRATE).toString());
			// 【税额】计算
			UFDouble tempntaxmny = SafeCompute.sub(ninvtaxmny,ndiffermny);// （收票金额—差额缴税扣除金额）
			UFDouble tempninvtaxmny = SafeCompute.add(new UFDouble(1), SafeCompute.div(ntaxrate, new UFDouble(100)));// （1+税率）
			// 税额计算公式修改：税额=（收票金额—差额缴税扣除金额）\（1+税率）*税率
			UFDouble ntaxmny = SafeCompute.multiply(SafeCompute.div(tempntaxmny,tempninvtaxmny),
					SafeCompute.div(ntaxrate, new UFDouble(100)));
			// 税额计算公式修改：税额=（收票金额—差额缴税扣除金额）\（1+税率）*税率
			billCardPanel.getBillModel(ReceiveBVO.TABCODE).setValueAt(ntaxmny, row, ReceiveBVO.NTAXMNY);//给税额赋值
			// 表头金额无税的赋值
			billCardPanel.getBillModel(ReceiveBVO.TABCODE).setValueAt(SafeCompute.sub(ninvtaxmny,ntaxmny),
								row, ReceiveBVO.NINVMNY);
		}
	}

	/**
	 * 汇总表头
	 * @param cardPanel
	 * @throws BusinessException
	 * 
	 */
	public void sumReceDetailMny(BillCardPanel cardPanel) throws BusinessException{
		String pk_corp = cardPanel.getCorp();
		int first_mode = InvParamTool.getTaxFirstMode(pk_corp);
		String[] invmnys = new String[]{ReceiveVO.NINVTAXMNY,ReceiveVO.NINVMNY};
		String[] nthrecemnys = new String[]{ReceiveDetailVO.NTHRECETAXMNY,ReceiveDetailVO.NTHRECEMNY};
		
		int rowcount = cardPanel.getBillModel(ReceiveDetailVO.TABCODE).getRowCount();
	//	UFDouble ninvmny_h = new UFDouble((String)cardPanel.getHeadItem(invmnys[first_mode]).getValueObject());
		UFDouble nthrecemny_cllect = UFDouble.ZERO_DBL;
		UFDouble nthrecemny_cllect1 = UFDouble.ZERO_DBL;
		UFDouble nheadtaxmny = UFDouble.ZERO_DBL;
		for(int i = 0; i <= rowcount; i++){
			ReceiveDetailVO detailVO = (ReceiveDetailVO) cardPanel.getBillModel(ReceiveDetailVO.TABCODE).
					getBodyValueRowVO(i, ReceiveDetailVO.class.getName());
			UFDouble nthrecemny = (UFDouble)detailVO.getAttributeValue(nthrecemnys[first_mode]);
			UFDouble nthrecemny1 = (UFDouble)detailVO.getAttributeValue(nthrecemnys[1-first_mode]);
			UFDouble ntaxmny = (UFDouble)detailVO.getAttributeValue(ReceiveDetailVO.NTAXMNY);
			
			nthrecemny_cllect = SafeCompute.add(nthrecemny_cllect, nthrecemny);
			nthrecemny_cllect1 = SafeCompute.add(nthrecemny_cllect1, nthrecemny1);
			nheadtaxmny = SafeCompute.add(nheadtaxmny, ntaxmny);
			
			getBillCardPanel().setHeadItem(invmnys[first_mode], nthrecemny_cllect);
			getBillCardPanel().setHeadItem(invmnys[1-first_mode], nthrecemny_cllect1);
			getBillCardPanel().setHeadItem(ReceiveVO.NTAXMNY, nheadtaxmny);
		}
	}
	
	@Override
	protected void initEventListener() {
		this.getBillCardPanel().setBillBeforeEditListenerHeadTail(this);
		super.initEventListener();
	}

	@Override
	public void setDefaultData() throws Exception {
		super.setDefaultData();
		getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setEdit(false);
		getBillCardPanel().getHeadItem(ReceiveVO.PK_RECEIVE_REF).setNull(false);
		getBillCardPanel().getBillModel().execLoadFormula();
		getBillListPanel().getHeadBillModel().execLoadFormula();
		getDeptPsnRela().setOnAdd();
		// 原币 
		getBillCardPanel().setHeadItem(getOriginCurrFldName(), getCy().getLocalCurrPK()); //新增时赋值原币币种
		getBillCardPanel().setHeadItem(getBaseCurrFldName(), getCy().getLocalCurrPK()); //新增时赋值原币币种
		getBillCardPanel().setHeadItem(getBaseRateFldName(), new UFDouble(1.0));
	}

	public DeptPsnRela getDeptPsnRela() {
		DeptPsnRela deptPsnRela = null;
		BillCardPanel billCardPanel = getBillCardPanel();
		BillItem deptItem = billCardPanel.getHeadItem(ReceiveVO.PK_DEPTDOC);
		BillItem psnItem = billCardPanel.getHeadItem(ReceiveVO.PK_PSNDOC);
		deptPsnRela = new DeptPsnRela(deptItem, psnItem);
		return deptPsnRela;
	}

	/**
	 * 表头编辑前事件
	 */
	public boolean beforeEdit(BillItemEvent e) {
		String key = e.getItem().getKey();
		if (ReceiveVO.PK_CONTRACT.equals(key)) {
			new ContractEditHandler(this).cardHeadBeforeEdit(e);
		}
		return true;
	}

	/**
	 * 表体编辑前事件
	 */
	public boolean beforeEdit(BillEditEvent e) {
		return super.beforeEdit(e);
	}

	@Override
	public String[] getHeadOriginItems() {
		return new String[] { ReceiveVO.NORIGINVMNY, ReceiveVO.NORIGINVTAXMNY, ReceiveVO.NORIGBACKMNY,
				ReceiveVO.NORIGBACKTAXMNY, ReceiveVO.NORIGHADREDMNY, ReceiveVO.NORIGHADREDTAXMNY,
				ReceiveVO.NORIGHADSETLEVEFYMNY, ReceiveVO.NORIGHADSETLEVEFYTAXMNY, ReceiveVO.NORIGHADPAYVEFYMNY,
				ReceiveVO.NORIGHADPAYVEFYTAXMNY };
	}

	@Override
	public String[] getHeadBaseItems() {
		return new String[] { ReceiveVO.NINVMNY, ReceiveVO.NINVTAXMNY, ReceiveVO.NTAXMNY, ReceiveVO.NBACKMNY,
				ReceiveVO.NBACKTAXMNY, ReceiveVO.NHADREDMNY, ReceiveVO.NHADREDTAXMNY, ReceiveVO.NHADSETLEVEFYMNY,
				ReceiveVO.NHADSETLEVEFYTAXMNY, ReceiveVO.NHADPAYVEFYMNY, ReceiveVO.NHADPAYVEFYTAXMNY,
				ReceiveVO.NNOSETLEVEFYMNY, ReceiveVO.NNOSETLEVEFYTAXMNY, ReceiveVO.NNOPAYVEFYTAXMNY,
				ReceiveVO.NFHADDDUCTMNY, ReceiveVO.NFHADDDUCTTAXMNY };
	}

	@Override
	public String[][] getBodyOriginItems() {
		return new String[][] {
				{ ReceiveBVO.NORIGINVMNY, ReceiveBVO.NORIGINVTAXMNY, ReceiveBVO.NTAXMNY },
				{ ReceiveDetailVO.NORIGCONTRAMNY, ReceiveDetailVO.NORIGCONTRATAXMNY, ReceiveDetailVO.NORIGSUMRECEMNY,
						ReceiveDetailVO.NORIGSUMRECETAXMNY, ReceiveDetailVO.NORIGTHRECEMNY,
						ReceiveDetailVO.NORIGTHRECETAXMNY, ReceiveDetailVO.NORIGAPPLIEDMNY,
						ReceiveDetailVO.NORIGAPPLIEDTAXMNY, ReceiveDetailVO.NORIGPAIDMNY,
						ReceiveDetailVO.NORIGPAIDTAXMNY, ReceiveDetailVO.NORIGPREPAYMNY,
						ReceiveDetailVO.NORIGPREPAYTAXMNY, ReceiveDetailVO.NTAXMNY } };
	}

	@Override
	public String[][] getBodyBaseItems() {
		return new String[][] {
				{ ReceiveBVO.NINVMNY, ReceiveBVO.NINVTAXMNY, ReceiveBVO.NTAXMNY },
				{ ReceiveDetailVO.NCONTRAMNY, ReceiveDetailVO.NCONTRATAXMNY, ReceiveDetailVO.NSUMRECEMNY,
						ReceiveDetailVO.NSUMRECETAXMNY, ReceiveDetailVO.NTHRECEMNY, ReceiveDetailVO.NTHRECETAXMNY,
						ReceiveDetailVO.NAPPLIEDMNY, ReceiveDetailVO.NAPPLIEDTAXMNY, ReceiveDetailVO.NPAIDMNY,
						ReceiveDetailVO.NPAIDTAXMNY, ReceiveDetailVO.NPREPAYMNY, ReceiveDetailVO.NPREPAYTAXMNY,
						ReceiveDetailVO.NTAXMNY } };
	}

	public String[] getTableCodes() {
		return new String[] { ReceiveBVO.TABCODE, ReceiveDetailVO.TABCODE };
	}

	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBodySpecialData(CircularlyAccessibleValueObject[] vos) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setHeadSpecialData(CircularlyAccessibleValueObject vo, int intRow) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setTotalHeadSpecialData(CircularlyAccessibleValueObject[] vos) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initSelfData() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRefBillType() {
		return IJzinvBillType.JZINV_RECEIPT_COLL;
	}

	private HashMap<String, String[][]> bodyDigitMap = null;

	@Override
	/**
	 *表体单价数量特殊精度参数处理
	 */
	public HashMap<String, String[][]> getBodyOthDigitItems() {
		if (bodyDigitMap == null) {
			bodyDigitMap = new HashMap<String, String[][]>();
			bodyDigitMap.put(
					ReceiveBVO.TABCODE,
					new String[][] {
							{ ReceiveBVO.NNUM, ReceiveBVO.NPRICEMNY, ReceiveBVO.NPRICETAXMNY },
							{ ParamReader.getParamter(_getCorp().getPk_corp(), ParamReader.NUMBER_DIGITAL),
									ParamReader.getParamter(_getCorp().getPk_corp(), ParamReader.PRICE_DIGITAL),
									ParamReader.getParamter(_getCorp().getPk_corp(), ParamReader.PRICE_DIGITAL) } });
		}
		return bodyDigitMap;
	}
	public void updateBtnWhenInfoAdd() {
		if (getBillOperate() == IBillOperate.OP_EDIT && isInfoSupply) {
			int[] unenblebtn = getUnEnbleBtnWhenInfoAdd();
			if (unenblebtn != null && unenblebtn.length > 0) {
				for (int btn : unenblebtn) {
					ButtonObject button = getButtonManager().getButton(btn);
					button.setEnabled(false);
				}
				try {
					updateButtonUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * 返回信息补充操作时不可用的按钮
	 * 
	 * @return
	 */
	protected int[] getUnEnbleBtnWhenInfoAdd() {
		return new int[] { IBillButton.Line};
	}
	@Override
	protected int getExtendStatus(AggregatedValueObject vo) {
		int billstatus = -1;
		if (vo == null) {
			return billstatus;
		}

		ReceiveVO head = (ReceiveVO) vo.getParentVO();
		UFBoolean bisAuthen = head.getBisauthen();
		if(!bisAuthen.booleanValue()){
			getButtonManager().getButton(IJzinvButton.SUPPLEMENTINFO).setEnabled(true);
		}else{
			getButtonManager().getButton(IJzinvButton.SUPPLEMENTINFO).setEnabled(false);
		}
		try {
			updateButtonUI();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return billstatus;
	}

}
