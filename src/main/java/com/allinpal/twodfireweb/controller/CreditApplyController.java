package com.allinpal.twodfireweb.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.allinpal.twodfireweb.request.CreditReq;
import com.allinpal.twodfireweb.util.BeanUtil;

@Controller
public class CreditApplyController {
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
	
    @RequestMapping(value ="/preapply")  
    public String apply(CreditReq credit,Model model){ 
    	try{
			MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<String, Object>();
			paramMap.add("merchantNo", credit.getMerchantNo());
			paramMap.add("mobile", credit.getMobile());
			paramMap.add("prodCode", credit.getProdCode());
			paramMap.add("userId", credit.getUserId());
	        String result = restTemplate.postForObject(creditServiceUrl.concat("/preapply"), paramMap, String.class);
			logger.info("preapply param {}", result);
	        JSONObject initJson = JSONObject.parseObject(result);
			CreditReq creditReq = JSONObject.parseObject(initJson.toString(), CreditReq.class);
			if(RETCODE_000000.equals(initJson.getString("code"))){
		    	String creditReqStr = JSON.toJSONString(creditReq);
		    	model.addAttribute("creditReqStr", creditReqStr);
		    	model.addAttribute("creditReq", creditReq);
		    	model.addAttribute("mobilestr", getMobilestr(creditReq.getMobile()));
			}else{
				model.addAttribute("merchantNo", creditReq.getMerchantNo());
				model.addAttribute("mobile", creditReq.getMobile());
				model.addAttribute("prodCode", creditReq.getProdCode());
		    	model.addAttribute("code", initJson.getString("code"));
		    	model.addAttribute("message", initJson.getString("message"));
		    	return "loanApply/applyFail";
			}
	    	
	    }catch(Exception e){
    		logger.info("applyFirst exception : ",e);
	    	model.addAttribute("code", "000002");
	    	model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
	    	return "loanApply/applyFail";
    	}
		return"loanApply/applyFirst";  
    }
   
    @RequestMapping(value ="/applySecond", method = RequestMethod.POST)  
    public String applySecond(CreditReq creditReq,Model model, HttpServletRequest request){ 
    	try{  
        	String creditReqStr = request.getParameter("creditReqStr");
        	logger.info("creditReqStr is : "+creditReqStr);
        	CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
			new BeanUtil().copyProperties(creditReq, tempInfo, true);
			
    		model.addAttribute("creditReqStr", JSON.toJSONString(creditReq));
	    	model.addAttribute("creditReq", creditReq);
    	}catch(Exception e){
    		logger.info("applySecond exception : ",e);
    	}
		return"loanApply/applySecond";  
    }
    
    @RequestMapping(value ="/applyThird", method = RequestMethod.POST)  
    public String applyThird(CreditReq creditReq,Model model, HttpServletRequest request){ 
    	try{  
        	String creditReqStr = request.getParameter("creditReqStr");
        	logger.info("creditReqStr is : "+creditReqStr);
        	CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
			new BeanUtil().copyProperties(creditReq, tempInfo, true);
			
			creditReq.setCertType("10100");
    		creditReq.setMchtCity(creditReq.getShowCityPicker());
    		creditReq.setLiveCity(creditReq.getShowCityPicker2());
    		if("20".equals(creditReq.getMarriag())){
    			creditReq.setSpouseIdtp("10100");
    		}
    		model.addAttribute("creditReqStr", JSON.toJSONString(creditReq));
	    	model.addAttribute("creditReq", creditReq);
	    	String acctNo =  creditReq.getBankAcctNo();
	    	if(StringUtils.isEmpty(acctNo)){
	    		model.addAttribute("acctNoStr", "");
	    	}else{
	    		model.addAttribute("acctNoStr", acctNo.substring(acctNo.length()-4, acctNo.length()));
	    	}
    	}catch(Exception e){
    		logger.info("applyThird exception : ",e);
    	}
		return"loanApply/applyThird";  
    }
    
    /**
     * 校验客户经理手机号
     */
    @RequestMapping(value = "/checkEmpPhone", method = RequestMethod.POST)
	@ResponseBody
    public String checkEmpPhone(String empPhone, Model model, HttpServletRequest request){
    	String res;
		try {
			MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
			paramMap.add("phone", empPhone);
			res = restTemplate.postForObject(creditServiceUrl.concat("/checkEmpPhone"), paramMap, String.class);
			logger.info("checkEmpPhone result {}", res);
	    } catch (Exception e) {
			logger.info("",e);
			Map<String, String> temp = new HashMap<String, String>();
			temp.put("code", "error");
			temp.put("message", "校验客户经理失败");
			res = JSON.toJSONString(temp);
		}
		return res;
    }
    
    @RequestMapping(value ="/submitApply", method = RequestMethod.POST)  
    public String submitApply(CreditReq creditReq,Model model, HttpServletRequest request){ 
    	try{  
    		BigDecimal appAmt = creditReq.getApplAmt();
    		String empPhone = creditReq.getEmpPhone();
    		
    		logger.info("appAmt :"+appAmt);
    		logger.info("empPhone:"+empPhone);
    		
        	String creditReqStr = request.getParameter("creditReqStr");
        	logger.info("creditReqStr is : "+creditReqStr);
        	CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
			new BeanUtil().copyProperties(creditReq, tempInfo, true);	
    		//重新设值
    		creditReq.setApplAmt(appAmt);
    		creditReq.setEmpPhone(empPhone);
    		
    		logger.info("creditReq.getApplAmt() :"+creditReq.getApplAmt());
    		logger.info("creditReq.getEmpPhone() :"+creditReq.getEmpPhone());
			
			//1.创建账户
			String acctId ="";
			Map<String,Object> prod = convertObjToMap(creditReq);
			MultiValueMap<String, Object> prodMap = convertMap2MultiValueMap(prod);
			String prodResult = restTemplate.postForObject(creditServiceUrl.concat("/createProdAcct"), prodMap, String.class);
			//记得设置acctId
			JSONObject prodJson = JSONObject.parseObject(prodResult);
			logger.info("apply param {}", prodJson.toString());
			if(!RETCODE_000000.equals(prodJson.getString("code"))){
				model.addAttribute("code", prodJson.getString("code"));
		    	model.addAttribute("message", prodJson.getString("message"));
		    	model.addAttribute("merchantNo", creditReq.getMerchantNo());
		    	model.addAttribute("prodCode", creditReq.getProdCode());
		    	model.addAttribute("mobile", creditReq.getMobile());
		    	return "loanApply/applyFail";
			}else{
				acctId = prodJson.getString("acctId");
			}
			
			//2.创建授信申请
			creditReq.setAcctId(acctId);
			creditReq.setOrgChl("APP");
			creditReq.setOrgCode(ORGCODE_2DFIRE);
			creditReq.setProdCode(PRODCODE_2DFIRE);
			creditReq.setPayRate(new BigDecimal("12"));
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMddHHmmss" );
			creditReq.setGrantNo("THZX"+ sdf.format(date));			//征信查询授权书编号
			creditReq.setGrantTime(date.getTime());			//征信查询授权时间
			Map<String,Object> requestMap = convertObjToMap(creditReq);
			MultiValueMap<String, Object> paramMap = convertMap2MultiValueMap(requestMap);
        	logger.info("submitApply paramMap is : "+paramMap);
	        String result = restTemplate.postForObject(creditServiceUrl.concat("/apply"), paramMap, String.class);
	        JSONObject initJson = JSONObject.parseObject(result);
			logger.info("apply param {}", initJson.toString());
			if(!RETCODE_000000.equals(initJson.getString("code"))){
		    	model.addAttribute("code", initJson.getString("code"));
		    	model.addAttribute("message", initJson.getString("message"));
		    	model.addAttribute("merchantNo", creditReq.getMerchantNo());
		    	model.addAttribute("prodCode", creditReq.getProdCode());
		    	model.addAttribute("mobile", creditReq.getMobile());
		    	return "loanApply/applyFail";
			}else{
				CreditReq creditRet = JSON.parseObject(initJson.getString("creditInfo"), CreditReq.class);
				Map<String,String> temp = new HashMap<String,String>();
				temp.put("orgCode", orgCode);
				temp.put("mode", mode);
				temp.put("loginId", creditRet.getCertNo());
				temp.put("userName", creditRet.getCustName());
				temp.put("idenno", creditRet.getCertNo());
				model.addAttribute("message", JSON.toJSONString(temp));
		    	model.addAttribute("merchantNo", creditReq.getMerchantNo());
			}
    	}catch(Exception e){
    		logger.info("submitApply exception : ",e);
	    	model.addAttribute("code", "000002");
	    	model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
    		return"loanApply/applyFail";  
    	}
		return"pickReport";  
    }

	private String getMobilestr(String mobile) {
		return mobile.substring(0,3)+"****"+mobile.substring(7, 11);
	}  
    
	private Map<String, Object> convertObjToMap(CreditReq obj) {
		 Map<String,Object> reMap = new HashMap<String,Object>();
		 if (obj == null){
			 return null;
		 }
		 Field[] fields = obj.getClass().getDeclaredFields();
		 try{
			 for(int i=0;i<fields.length;i++){
				 try{
					 Field f = obj.getClass().getDeclaredField(fields[i].getName());
					 f.setAccessible(true);
					 Object o = f.get(obj);
					 reMap.put(fields[i].getName(), o);
				 }catch (NoSuchFieldException e) {
					 e.printStackTrace();
				 }catch (IllegalArgumentException e) {
					 e.printStackTrace();
				 }catch (IllegalAccessException e) {
					 e.printStackTrace();
				 }
			 }
		 }catch (SecurityException e) {
			 e.printStackTrace();
		 } 
		 return reMap;
	}
	
	public static MultiValueMap<String, Object> convertMap2MultiValueMap(Map<String, Object> map){
		
		MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
		for(Map.Entry<String, Object> entry :map.entrySet()){
			multiValueMap.add(entry.getKey(), entry.getValue());
		}
		return multiValueMap;		
	}
	
	 @RequestMapping(value ="/selectCard", method = RequestMethod.POST)  
	    public String selectCard(CreditReq creditReq,Model model, HttpServletRequest request){ 
	    	try{  
	    		BigDecimal appAmt = creditReq.getApplAmt();
	    		String empPhone = creditReq.getEmpPhone();
	    		
	        	String creditReqStr = request.getParameter("creditReqStr");
	        	logger.info("creditReqStr is : "+creditReqStr);
	        	CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
				new BeanUtil().copyProperties(creditReq, tempInfo, true);
				
				//重新设值
	    		creditReq.setApplAmt(appAmt);
	    		creditReq.setEmpPhone(empPhone);
				
	    		model.addAttribute("creditReqStr", JSON.toJSONString(creditReq));
		    	model.addAttribute("creditReq", creditReq);
		    	
		    	model.addAttribute("merchantNo", creditReq.getMerchantNo());
		    	model.addAttribute("userId", creditReq.getUserId());
		    	model.addAttribute("mobile", creditReq.getMobile());
		    	
		    	model.addAttribute("bankName", creditReq.getBankCode());
		    	model.addAttribute("bankCode", creditReq.getBankCode());
		    	model.addAttribute("bankAcctNo", creditReq.getBankAcctNo());
		    	
	    	}catch(Exception e){
	    		logger.info("applyThird exception : ",e);
	    	}
			return"loanApply/selectBankCard";  
	    }
	 
	
	 @RequestMapping(value="/changeCard", method = RequestMethod.POST)
	 public String changeCard(CreditReq creditReq,Model model, HttpServletRequest request) {
		 try{  
			String acctNo = creditReq.getBankAcctNo();
			String acctName = creditReq.getBankName();
			String banckCode = creditReq.getBankCode();
			
			logger.info("changeCard/banckCode is :"+banckCode);
			String creditReqStr = request.getParameter("creditReqStr");
	     	logger.info("creditReqStr is : "+creditReqStr);
	     	CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
		    new BeanUtil().copyProperties(creditReq, tempInfo, true);
		    
		    logger.info("changeCard is/creditReq.getBankAcctNo() :"+creditReq.getBankAcctNo());
		    logger.info("changeCard is/creditReq.getBankName() :"+creditReq.getBankName());
		
		    //重新设置
		    creditReq.setBankAcctNo(acctNo);
		    creditReq.setBankName(acctName);
		    creditReq.setBankCode(banckCode);
		    
	    	model.addAttribute("creditReqStr", JSON.toJSONString(creditReq));
	    	model.addAttribute("creditReq", creditReq);
	   
	    	model.addAttribute("bankName", creditReq.getBankCode());
	    	model.addAttribute("bankCode", creditReq.getBankCode());
	    	model.addAttribute("bankAcctNo", creditReq.getBankAcctNo());
	    	model.addAttribute("acctNoStr", acctNo.substring(acctNo.length()-4, acctNo.length()));    	
		 }catch(Exception e){
	    	logger.info("changeCard exception : ",e);
		 }
		 return"loanApply/applyThird";
	}
}
