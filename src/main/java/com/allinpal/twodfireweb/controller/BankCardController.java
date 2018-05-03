package com.allinpal.twodfireweb.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.allinpal.twodfireweb.request.BankCardDto;
import com.allinpal.twodfireweb.request.CreditReq;
import com.allinpal.twodfireweb.request.User;

@Controller
public class BankCardController {	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${idenVerifyService.url}")
	private String idenVerifyServiceUrl;
	
	private static final String QUERY_CARD_URI = "/bindedCard";
	
	private static final String QUERY_ACCT_URI = "/creditCard";
	
	private static final String CREDIT_CARD_CHECK_URI = "/checkCreditCard";
	
	private static final String DELETE_BANK_CARD_URI = "/deleteBankCard";
		
	
	/**跳转绑卡
	 * @param user
	 * @param model
	 * @return
	 */
	@RequestMapping("/goBindCard")
	public String goBindCard(User user, Map<String, Object> model){
		//跳转绑卡页面
		logger.info("goBindCard param merchantNo:{},userId:{},mobile:{}",
				 user.getMerchantNo(), user.getUserId(),user.getMobile());
		model.put("userId", user.getUserId());
		model.put("merchantNo", user.getMerchantNo());
		model.put("mobile", user.getMobile());
		return "idenVerify/bindCard";
	}
	
	
	/**我的账户银行卡列表
	 * @param userId
	 * @return
	 */
	@RequestMapping(value="/ajaxGetCardList", produces="application/json; charset=utf-8")
	@ResponseBody
	public String ajaxGetCardList(String userId) {
		logger.info("ajaxGetCardList param userId:{}", userId);
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("userId", userId);
        String result = restTemplate.postForObject(idenVerifyServiceUrl+QUERY_CARD_URI, paramMap, String.class);
        logger.info("ajaxGetCardList result:{}",result);
        return result;
	}
	
	/**查询子账户关联的当前贷款卡
	 * @param userId
	 * @return
	 */
	@RequestMapping(value="/ajaxGetCreditCard", produces="application/json; charset=utf-8")
	@ResponseBody
	public String ajaxGetCreditCard(String userId) {
		logger.info("ajaxGetCreditCard param userId:{}", userId);
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("userId", userId);
        String result = restTemplate.postForObject(idenVerifyServiceUrl+QUERY_ACCT_URI, paramMap, String.class);
        logger.info("ajaxGetCreditCard result:{}",result);
        return result;
	}
	
	/**贷款卡删除/更换校验
	 * @param userId
	 * @param acctNo
	 * @return
	 */
	@RequestMapping(value="/ajaxCheckCreditCard", produces="application/json; charset=utf-8")
	@ResponseBody
	public String ajaxCheckCreditCard(CreditReq creditReq) {
		String userId = creditReq.getUserId();
		String acctNo = creditReq.getBankAcctNo();
		logger.info("ajaxCheckCreditCard param userId:{}, acctNo:{} ", userId, acctNo);
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("userId", userId);
		paramMap.add("bankAcctNo", acctNo);
        String result = restTemplate.postForObject(idenVerifyServiceUrl+CREDIT_CARD_CHECK_URI, paramMap, String.class);
        logger.info("ajaxCheckCreditCard result:{}",result);
        return result;
	}
	
	/**删卡
	 * @param userId
	 * @param acctNo
	 * @return
	 */
	@RequestMapping(value="/ajaxDeleteBankCard", produces="application/json; charset=utf-8")
	@ResponseBody
	public String ajaxDeleteBankCard(BankCardDto bankCard, boolean isCreditCard) {
		String userId = bankCard.getUserId();
		String acctNo = bankCard.getAcctNo();
		logger.info("ajaxDeleteBankCard param userId:{}, acctNo:{}, isCreditCard:{} ", userId, acctNo, isCreditCard);
		MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<String, Object>();
		paramMap.add("userId", userId);
		paramMap.add("acctNo", acctNo);
		paramMap.add("isCreditCard", isCreditCard);
        String result = restTemplate.postForObject(idenVerifyServiceUrl+DELETE_BANK_CARD_URI, paramMap, String.class);
        logger.info("ajaxDeleteBankCard result:{}",result);
        return result;
	}

}
