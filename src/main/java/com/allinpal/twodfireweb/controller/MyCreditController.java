package com.allinpal.twodfireweb.controller;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.allinpal.twodfireweb.request.CreditReq;
import com.allinpal.twodfireweb.util.GenerateIdsUtil;

@Controller
public class MyCreditController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${creditService.url}")
	private String creditServiceUrl;

	@Value("${orgCode}")
	private String orgCode;

	@Value("${sdk.mode}")
	private String mode;
	
	private static String ORGCODE_2DFIRE = "21100005";
	private static String PRODCODE_2DFIRE = "9002000003";
	private static String RETCODE_000000="000000";
	//0全部 1申请未完成 2申请失效 3审批中 4审批失败 5审批通过 6额度生效 66额度到期 7额度失效 8审批作废 9额度暂停13贷前拒绝 14贷前通过
	private static String EFFECTIVE_STATUS="3,5,6,9,14";
	
    @RequestMapping(value ="/queryCreditDetail")  
    public String queryCreditDetail(CreditReq credit,Model model){ 
    	try{
    	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	    logger.info("queryCreditDetail credit "+JSON.toJSONString(credit));

	    	model.addAttribute("merchantNo", credit.getMerchantNo());
	    	model.addAttribute("prodCode", credit.getProdCode());
	    	model.addAttribute("mobile", credit.getMobile());
	    	
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			if(null != credit.getRecordNo() && !"".equals(credit.getRecordNo())){
		    	map.add("recordNo", credit.getRecordNo());
			}
	    	map.add("merchantNo", credit.getMerchantNo());
			map.add("prodCode", credit.getProdCode());
			map.add("describle", "queryCreditDetail");
	        String result = restTemplate.postForObject(creditServiceUrl.concat("/query"), map, String.class);
			logger.info("queryCreditDetail result {}", result);
	        JSONObject initJson = JSONObject.parseObject(result);
			if(RETCODE_000000.equals(initJson.getString("code"))){
	    		List<CreditReq> creditList = JSON.parseArray(initJson.getString("creditList"), CreditReq.class); 
	    		if(null != creditList && creditList.size() > 0){
	    			if(!EFFECTIVE_STATUS.contains(creditList.get(0).getStatus())){
		    	    	model.addAttribute("effective", "0"); 
	    			}
    				//待提取报告
	    			if("3".equals(creditList.get(0).getStatus()) && "1".equals(creditList.get(0).getStatus2dfire())){
	    				Map<String,String> temp = new HashMap<String,String>();
	    				temp.put("orgCode", orgCode);
	    				temp.put("mode", mode);
	    				temp.put("loginId", creditList.get(0).getCertNo());
	    				temp.put("userName", creditList.get(0).getCustName());
	    				temp.put("idenno", creditList.get(0).getCertNo());
	    				model.addAttribute("message", JSONObject.toJSON(temp));
	    		    	model.addAttribute("merchantNo", creditList.get(0).getMerchantNo());
	    				return"pickReport";  
	    			}
	    			//待额度审批
	    			if("3".equals(creditList.get(0).getStatus()) && "2".equals(creditList.get(0).getStatus2dfire())){
	    				return"myCredit/creditToCal"; 
	    			}
	    			//审批失败 或贷前拒绝
	    			if("4".equals(creditList.get(0).getStatus()) || "13".equals(creditList.get(0).getStatus())){
	    		    	model.addAttribute("message", "对不起，您的贷款未通过审批");
	    				return"loanApply/applyFail"; 
	    			}
	    			//待贷前审核
	    			if("5".equals(creditList.get(0).getStatus())){
	    				BigDecimal twelveMonthAmt = creditList.get(0).getTwelveMonthAmt();
	    				BigDecimal sixMonthAmt = creditList.get(0).getSixMonthAmt();
	    				if(null == twelveMonthAmt || new BigDecimal("0").compareTo(twelveMonthAmt) == 0){
		    				model.addAttribute("month", "6");
		    				model.addAttribute("sixMonthAmt", sixMonthAmt.setScale(0));
	    				}else{
		    				model.addAttribute("month", "12");
		    				model.addAttribute("twelveMonthAmt", twelveMonthAmt.setScale(0));
	    				}
		    			model.addAttribute("creditReq", creditList.get(0));
	    				return"myCredit/creditToCheck"; 
	    			}
	    			
	    			if("3".equals(creditList.get(0).getStatus2dfire())){
	    				model.addAttribute("signFlag", "0");
	    			}
	    			Date createTime=new Date(creditList.get(0).getCreateTime());
	    			model.addAttribute("createTime", sdf.format(createTime));
	    			Date currDate = new Date();
        	    	Calendar calendar = Calendar.getInstance();
        	    	calendar.setTime(currDate);
        	    	calendar.add(Calendar.MONTH, 6);
        	    	String sixEndDate = sdf.format(calendar.getTime());
        	    	calendar.add(Calendar.MONTH, 6);
        	    	String twelveEndDate = sdf.format(calendar.getTime());
        	    	logger.info("sixEndDate "+sixEndDate+"twelveEndDate "+twelveEndDate);
        	    	if(null != creditList.get(0).getUseAmt() && !"".equals(creditList.get(0).getUseAmt())){
        	    		String sDate = creditList.get(0).getAppStartDate();
        	    		String eDate = creditList.get(0).getAppEndDate();
    	    			model.addAttribute("lmtStartDate", sDate.substring(0, 4)+"-"+sDate.substring(4, 6)+"-"+sDate.substring(6, 8));
    	    			model.addAttribute("lmtEndDate", eDate.substring(0, 4)+"-"+eDate.substring(4, 6)+"-"+eDate.substring(6, 8));
    	    			if("0".equals(creditList.get(0).getStatusUse()) || "9".equals(creditList.get(0).getStatusUse()) || "13".equals(creditList.get(0).getStatusUse())){
    	    				creditList.get(0).setStatus2dfire("5"); //5-签订失败
    	    				model.addAttribute("signFlag", "0");
    	    			}else{
    	    				model.addAttribute("signFlag", "1");
    	    			}
        	    	}
	    			model.addAttribute("currDate", sdf.format(currDate));
	    			model.addAttribute("sixEndDate", sixEndDate);
	    			model.addAttribute("twelveEndDate", twelveEndDate);
	    			model.addAttribute("creditReq", creditList.get(0));
	        		model.addAttribute("creditReqStr", JSON.toJSONString(creditList.get(0)));
	        		
	        		//协议参数
	    			SimpleDateFormat df = new SimpleDateFormat( "yyyyMMdd");
	    			model.addAttribute("signDate", df.format(currDate));//协议签订日期
	    			model.addAttribute("protocolNoWTKK", GenerateIdsUtil.protocolId("0"));//委托扣款协议编号
	    			model.addAttribute("protocolNoKHFW", GenerateIdsUtil.protocolId("1"));//客户服务协议编号

	    		}else{
	    	    	model.addAttribute("totalCount", "0"); 
	    		}
			}
	    }catch(Exception e){
    		logger.info("queryCreditDetail exception : ",e);
	    	model.addAttribute("code", "000002");
	    	model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
    		return"myCredit/fail";  
    	}
		return"myCredit/creditDetail";  
    }

    @RequestMapping(value ="/showProtocol", method = RequestMethod.POST)  
    public String showProtocol(Model model,HttpServletRequest request){ 
    	try{
        	String creditReqStr = request.getParameter("creditReqStr");
        	logger.info("creditReqStr is : "+creditReqStr);
        	CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
	    	model.addAttribute("custName", tempInfo.getCustName());
	    	model.addAttribute("certNo", tempInfo.getCertNo());
	    	model.addAttribute("signDate", request.getParameter("signDate"));
	    	
    		String protocolType = request.getParameter("protocolType");
    		if("1".equals(protocolType)){
    	    	model.addAttribute("protocolNoKHFW", request.getParameter("protocolNoKHFW"));
    	 		return"myCredit/2dfireCustomerServiceProtocol";  
    		}else if("0".equals(protocolType)){
    	    	model.addAttribute("bankAcctNo", tempInfo.getBankAcctNo());
    	    	model.addAttribute("protocolNoWTKK", request.getParameter("protocolNoWTKK"));
    	 		return"myCredit/2dfireWTKKProtocol";  
    		}
    		
    	 }catch(Exception e){
     		logger.info("showProtocol exception : ",e);
 	    	model.addAttribute("code", "000002");
 	    	model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
     	}
 		return"myCredit/fail";  
     }


    @RequestMapping(value ="/queryCreditList")  
    public String queryCreditList(CreditReq credit,Model model){ 
    	try{
    	    logger.info("queryCreditList credit param "+JSON.toJSONString(credit));
	    	model.addAttribute("merchantNo", credit.getMerchantNo());
	    	model.addAttribute("prodCode", credit.getProdCode());
	    	model.addAttribute("mobile", credit.getMobile());	    	

    	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
	    	map.add("merchantNo", credit.getMerchantNo());
			map.add("prodCode", credit.getProdCode());
	        String result = restTemplate.postForObject(creditServiceUrl.concat("/query"), map, String.class);
			logger.info("queryCreditList result {}", result);
	        JSONObject initJson = JSONObject.parseObject(result);
			if(RETCODE_000000.equals(initJson.getString("code"))){
	    		List<CreditReq> creditList = JSON.parseArray(initJson.getString("creditList"), CreditReq.class); 
	    		if(null != creditList && creditList.size() > 0){
	    			for(int i=0;i<creditList.size();i++){
		    			Date createTime=new Date(creditList.get(i).getCreateTime());
		    			creditList.get(i).setDescrible((sdf.format(createTime)));
		    			if("6".equals(creditList.get(i).getStatus()) || "7".equals(creditList.get(i).getStatus())){

		    				MultiValueMap<String, Object> parammap = new LinkedMultiValueMap<String, Object>();
		    				parammap.add("recordNo", creditList.get(i).getRecordNo());
		    				parammap.add("describle", "queryCreditDetail");
		    		        String ret = restTemplate.postForObject(creditServiceUrl.concat("/query"), parammap, String.class);
		    				logger.info("queryCreditDetail result {}", ret);
		    		        JSONObject retJson = JSONObject.parseObject(ret);
		    				if(RETCODE_000000.equals(retJson.getString("code"))){
		    		    		List<CreditReq> signList = JSON.parseArray(retJson.getString("creditList"), CreditReq.class); 
		    		    		creditList.get(i).setUseAmt(signList.get(0).getUseAmt());
		    		    		creditList.get(i).setStatusUse(signList.get(0).getStatusUse());
		    	    			model.addAttribute("useAmt", signList.get(0).getUseAmt());
		    	    			model.addAttribute("statusUse", signList.get(0).getStatusUse());
			    				logger.info("queryCreditList creditDetail useAmt {}", signList.get(0).getUseAmt());
		    				}
		    			}
		    			
	    			}
	    			
	    			model.addAttribute("creditList", creditList);
	    			model.addAttribute("creditListStr", JSONObject.toJSON(creditList));
	    			
	    		}else{
	    	    	model.addAttribute("totalCount", "0"); 
	    		}
			}
    		
    	}catch(Exception e){
    		logger.info("queryCreditList exception : ",e);
	    	model.addAttribute("code", "000002");
	    	model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
    		return"myCredit/fail";  
    	}
		return"myCredit/creditList";  
    }
	
}
