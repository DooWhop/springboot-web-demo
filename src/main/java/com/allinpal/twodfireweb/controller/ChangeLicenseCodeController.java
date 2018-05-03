package com.allinpal.twodfireweb.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.allinpal.twodfireweb.request.User;
/**
 * @author admin
 * 更换营业执照号
 */
@Controller
public class ChangeLicenseCodeController extends BaseController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${changeLicenseCode.url}")
	private String changeLicenseCodeUrl;
	
	@SuppressWarnings("unchecked")
	@RequestMapping("/initChangeLicenseCode")
	public String initChangeLicenseCode(Model model, HttpServletRequest request) {
		logger.info("initChangeLicenseCode is start!");
		String userId = getOpenId(request);
		
		String userQueryUrl = changeLicenseCodeUrl.concat("/queryUser");
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("userId", userId);
		String jsonStr = restTemplate.postForObject(userQueryUrl, paramMap, String.class);
		logger.info("request from {} result is {}",userQueryUrl,jsonStr);
		if(jsonStr == null || "".equals(jsonStr)){
			return "common/error";
		}
		Map<String,Object> result = (Map<String,Object>)JSONObject.parseObject(jsonStr,Map.class);
		String code = (String)result.get("code");
		if(!"000000".equals(code)){
			return "common/error";
		}
		User user = (User)JSONObject.parseObject(JSON.toJSONString(result.get("user")),User.class);
		model.addAttribute("user", user);
		return "changeLicense/changeLicenseCode";
	}
	
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/changeLicenseCode", method = RequestMethod.POST)
	public String changeLicenseCode(Model model, HttpServletRequest request, User user) {
//		Map<String,Object> retMap = new HashMap<String,Object>();
		String userId = getOpenId(request);
		String oldLicenseCode = user.getOldLicenseCode();
		String licenseCode = user.getLicenseCode();
		model.addAttribute("merchantNo", user.getMerchantNo());		
		//判断用户输入的新营业执照号是否为空
		if(licenseCode == null || "".equals(licenseCode.trim())){
			model.addAttribute("message", "请输入营业执照号!");
			return "changeLicense/changefail";
		}
		
		if(licenseCode.equals(oldLicenseCode)){
			model.addAttribute("message", "输入的营业执照号与旧营业执照号一致!");
			return "changeLicense/changefail";
		}
		
		String changeUrl = changeLicenseCodeUrl.concat("/changeLicenseCode");
		
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("userId", userId);
		paramMap.add("licenseCode", licenseCode);
		
		String jsonStr = restTemplate.postForObject(changeUrl, paramMap, String.class);
		logger.info("request from {} result is {}",changeUrl,jsonStr);
		
		Map<String,Object> map = (Map<String,Object>)JSONObject.parseObject(jsonStr,Map.class);
		String code = (String)map.get("code");
		if(!"000000".equals(code)){
			model.addAttribute("message", map.get("message"));
			return "changeLicense/changefail";
		}
		//成功跳转二维火首页
		return "changeLicense/changesucc";
	}
}
