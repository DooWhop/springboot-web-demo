package com.allinpal.twodfireweb.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.allinpal.twodfireweb.request.CreditReq;
import com.allinpal.twodfireweb.request.TLoanProtocol;

@Controller
public class ContractController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private RestTemplate restTemplate;

	@Value("${contract.url}")
	private String contractUrl;

	@Value("${draw.url}")
	private String drawUrl;

	@RequestMapping(value = "/contractSign", method = RequestMethod.POST)
	public String contractSign(Model model, HttpServletRequest request) {
		logger.info("开始签约。");
		String merchantNo = request.getParameter("merchantNo");
		try {

			String appAmt = request.getParameter("appAmt");
			String status = request.getParameter("status");
			String protocolNum = request.getParameter("protocolNum");
			String timeLimit = request.getParameter("applyTnr");
			String usercode = request.getParameter("userCode");
			String recordNo = request.getParameter("recordNo");
			String fileId = request.getParameter("fileId");
			String prodCode = request.getParameter("prodCode");
			String batchNo = request.getParameter("batchNo");
			String useRecordNo = request.getParameter("useRecordNo");

			JSONObject params = new JSONObject();
			if (appAmt == null || "".equals(appAmt) || status == null
					|| "".equals(status) || protocolNum == null
					|| "".equals(protocolNum) || timeLimit == null
					|| "".equals(timeLimit) || usercode == null
					|| "".equals(usercode) || recordNo == null
					|| "".equals(recordNo) || fileId == null
					|| "".equals(fileId) || prodCode == null
					|| "".equals(prodCode) || merchantNo == null
					|| "".equals(merchantNo) || batchNo == null
					|| "".equals(batchNo) || useRecordNo == null
					|| "".equals(useRecordNo)) {
				// logger.error("contractSign 参数不正确，{}");
				// return "common/error";
			}
			params.put("appAmt", appAmt);
			params.put("status", status);
			params.put("protocolNum", protocolNum);
			params.put("timeLimit", timeLimit);
			params.put("usercode", usercode);
			params.put("recordNo", recordNo);
			params.put("fileId", fileId);
			params.put("prodCode", prodCode);
			params.put("merchantNo", merchantNo);
			params.put("batchNo", batchNo);
			params.put("useRecordNo", useRecordNo);
			logger.info("contractSign 参数{}", params);
			// 新增判断是否已支用，已支用则返回提示页面
			String drawApplyQueryUrl = drawUrl + "/queryUseInfo";
			JSONObject queryParam = new JSONObject();
			queryParam.put("creditRecordNo", recordNo);
			JSONObject queryRes = restTemplate.postForObject(drawApplyQueryUrl,
					queryParam, JSONObject.class);
			logger.info("判断是否已成功签约支用queryUseInfo param, {}", queryParam);
			logger.info("判断是否已成功签约支用queryUseInfo res, {}", queryRes);
			if ("000000".equals(queryRes.get("code"))) {
				JSONObject useInfoJO = queryRes.getJSONObject("data");
				logger.info("useInfoJO========>>>{}", useInfoJO);
				//12:放款处理中,4:已放款
				if ("12".equals(useInfoJO.get("status"))||"4".equals(useInfoJO.get("status"))) {
					logger.error("{}已完成支用签约。", useRecordNo);
					model.addAttribute("message", "已完成支用签约。");
					model.addAttribute("merchantNo", merchantNo);
					model.addAttribute("prodCode", prodCode);
					return "draw/drawSucc";
				} else if ("11".equals(useInfoJO.get("status"))) {
					logger.error("{} 放款失败。", useRecordNo);
					model.addAttribute("merchantNo", merchantNo);
					return "draw/error";
				}
				params.put("status", useInfoJO.get("status"));
			} else if ("000007".equals(queryRes.get("code"))) {
				logger.error("调用queryUseInfo接口异常。");
				model.addAttribute("merchantNo", merchantNo);
				return "draw/error";
			} else if ("000006".equals(queryRes.get("code"))) {
				logger.info("新增支用、服务费信息。");
				String drawApplyUrl = drawUrl + "/confirm";
				logger.info("contractSign=====>" + drawApplyUrl);
				String drawResult = restTemplate.postForObject(drawApplyUrl, params,
						String.class);
				logger.debug("request from {} result is {}", drawApplyUrl,
						drawResult);
				JSONObject drawJo = JSON.parseObject(drawResult);
				String code = drawJo.getString("code");
				if (code != null && !"".equals(code) && !"000000".equals(code)) {
					logger.error("调用confirm失败，状态码为{}", code);
					model.addAttribute("merchantNo", merchantNo);
					return "draw/error";
				}
			}
			// 2:征信查询授权书，3：扣款授权书
			Map<String, Object> protocolParams = new HashMap<String, Object>();
			protocolParams.put("creditRecordNo", recordNo);
			List<Map<String, Object>> protocolList = new ArrayList<Map<String, Object>>();
			Map<String, Object> KHXYmap = new HashMap<String, Object>();
			KHXYmap.put("protocolType", "2");
			Map<String, Object> KKmap = new HashMap<String, Object>();
			KKmap.put("protocolType", "3");
			Map<String, Object> DKSQ = new HashMap<String, Object>();
			DKSQ.put("protocolType", "6");
			protocolList.add(KHXYmap);
			protocolList.add(KKmap);
			protocolList.add(DKSQ);
			protocolParams.put("protocols", protocolList);
			protocolParams.put("useRecordNo", useRecordNo);
			protocolParams.put("DKHTNo", protocolNum);
			String signAgreeUrl = contractUrl + "/signAgreement";
			logger.info("signAgree signAgreeUrl=====>{}", signAgreeUrl);
			logger.info("signAgree param=====>{}", protocolParams);
			String signAgreeResult = restTemplate.postForObject(signAgreeUrl,
					protocolParams, String.class);
			logger.info("signAgree response=====>{}", signAgreeResult);

			logger.info("开始调用个人签章等服务进行签约。");
			String contractSignUrl = contractUrl + "/sign";
			logger.info("contractSign=====>" + contractSignUrl);
			String restResult = restTemplate.postForObject(contractSignUrl, params,
					String.class);
			// String restResult = restTemplate.getForObject(contractSignUrl,
			// String.class);
			logger.info("contractSign res: {}", restResult);
			logger.debug("request from {} result is {}", contractSignUrl,
					restResult);
			JSONObject restJO = JSON.parseObject(restResult);
			String code1 = restJO.getString("code");
			if (code1 != null && !"".equals(code1) && !"000000".equals(code1)) {
				// 签约失败
				logger.error("签约失败，状态码为{}", code1);
				model.addAttribute("merchantNo", merchantNo);
				return "draw/signError";
			}

			model.addAttribute("prodCode", prodCode);
			model.addAttribute("merchantNo", merchantNo);
			logger.info("response model, {}", model);

			// 删除当前支用的扣款合同pdf以及jpg文件
			String current = this.getClass().getResource("/").getPath()
					+ "static/pdfjs/";
			logger.info("current=====>" + current);

			String fileID = current + fileId + ".pdf";
			File file = new File(fileID);
			if (file.delete()) {
				logger.info("File is deleted, {}.", file.getName());
			} else {
				logger.info("PDF {} Delete failed.", fileId);
			}
			String imgID = current + fileId + ".jpg";
			File imgFile = new File(imgID);
			if (imgFile.delete()) {
				logger.info("File is deleted, {}.", imgFile.getName());
			} else {
				logger.info("IMG {} Delete failed.", fileId);
			}
			model.addAttribute("message", "用款申请签约成功");
			return "draw/drawSucc";
		} catch (Exception e) {
			logger.error("contractSign exception : ", e);
			model.addAttribute("code", "000002");
			model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
			model.addAttribute("merchantNo", merchantNo);
			return "draw/error";
		}

	}
	
	@RequestMapping(value = "/viewContractList", method = RequestMethod.POST)
    public String viewContractList(Model model,HttpServletRequest request) {
	    String creditReqStr = request.getParameter("creditReqStr");
        logger.info("creditReqStr is : "+creditReqStr);
        CreditReq credit = JSON.parseObject(creditReqStr, CreditReq.class);
	    
        logger.info("viewContractList param is {}",credit.getRecordNo());
        String result = restTemplate.postForObject(contractUrl.concat("/viewContract"), credit.getRecordNo(), String.class);
        logger.info("viewContractList result {}", result);
        JSONObject initJson = JSONObject.parseObject(result);
        if("000000".equals(initJson.getString("code"))){
            List<TLoanProtocol> protocols = JSON.parseArray(initJson.getString("protocols"), TLoanProtocol.class); 
            if(null != protocols && protocols.size() > 0){
                model.addAttribute("protocols", protocols);
                model.addAttribute("creditStr", JSONObject.toJSON(credit));
            }else{
                model.addAttribute("totalCount", "0"); 
            }
        }
        
        return "contract/contractList";
	}
	
	@RequestMapping(value ="/viewProtocol", method = RequestMethod.POST)  
    public String viewProtocol(Model model,HttpServletRequest request){ 
        try{
            String recordNo = request.getParameter("recordNo");
            logger.info("recordNo is {}",recordNo);
            String creditReqStr = request.getParameter("creditStr");
            logger.info("creditReqStr is : "+creditReqStr);
            CreditReq tempInfo = JSON.parseObject(creditReqStr, CreditReq.class);
            
            //queryContractDetail查询合同详情
            String result = restTemplate.postForObject(contractUrl.concat("/queryContractDetail"), recordNo, String.class);
            logger.info("queryContractDetail result {}", result);
            JSONObject initJson = JSONObject.parseObject(result);
            logger.info("queryContractDetail initJson {}", initJson);
            TLoanProtocol protocol = null;
            if(!"000000".equals(initJson.getString("code"))){
                model.addAttribute("code", "000002");
                model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
                model.addAttribute("merchantNo", tempInfo.getMerchantNo());
                return "draw/error";
            }
            if(null == initJson.getString("protocol") ){
                model.addAttribute("code", "000002");
                model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
                model.addAttribute("merchantNo", tempInfo.getMerchantNo());
                return "draw/error";
            }
            
            protocol = JSON.parseObject(initJson.getString("protocol"),TLoanProtocol.class); 
            String protocolType = protocol.getProtocolType();
            
            /*if("1".equals(protocolType)){
                model.addAttribute("custName", tempInfo.getCustName());
                model.addAttribute("certNo", tempInfo.getCertNo());
                model.addAttribute("signDate", protocol.getSignDate());
                model.addAttribute("protocolNoKHFW", protocol.getProtocolNo());
                
                logger.info("showKHFWProtocal model====>{} ", model);
                return"myCredit/2dfireCustomerServiceProtocol";
            }else if("0".equals(protocolType)){
                model.addAttribute("custName", tempInfo.getCustName());
                model.addAttribute("certNo", tempInfo.getCertNo());
                model.addAttribute("signDate", protocol.getSignDate());
                model.addAttribute("bankAcctNo", tempInfo.getBankAcctNo());
                model.addAttribute("protocolNoWTKK", protocol.getProtocolNo());
                
                logger.info("showWTKKProtocal model====>{} ", model);
                return"myCredit/2dfireWTKKProtocol";  
            }else if("2".equals(protocolType)){
                model.addAttribute("credit", tempInfo);
                model.addAttribute("signDate", protocol.getSignDate());
                model.addAttribute("protocolNum", "");
                
                logger.info("showKKSQSProtocal model====>{} ", model);
                return "draw/drawKKSQS";
            }else if("3".equals(protocolType)){
                model.addAttribute("custName", tempInfo.getCustName());
                model.addAttribute("signDate", protocol.getSignDate());
                
                logger.info("showZXCXProtocal model====>{} ", model);
                return "draw/drawZXCXSBSQS";
            }else if("4".equals(protocolType)){*/
            String current = this.getClass().getResource("/").getPath() + "static/pdfjs/";
            logger.info("current=====>" + current);
            File file = new File(current);
            if (!file.exists() && !file.isDirectory()) {
                logger.info("所在目录不存在，创建目录，{}", current);
                file .mkdir();
            }
            String fileID = current + protocol.getOriginalPdffileId() + ".pdf";
            File pdfFile = new File(fileID);
            if (!pdfFile.exists()) {
                byte[] bytes = Base64.decodeBase64(initJson.getString("base64String"));
                FileOutputStream fileOutputStream = new FileOutputStream(new File(fileID));
                fileOutputStream.write(bytes);
                fileOutputStream.close();
            }
            String imgId = current + protocol.getOriginalPdffileId() + ".jpg";
            File imgFile = new File(imgId);
            if (!imgFile.exists()) {
                this.pdf2multiImage(fileID, imgId);
            }
            pdfFile.delete();
            logger.info("=================fileID==========={}",protocol.getOriginalPdffileId());
            model.addAttribute("imgId", "pdfjs/" + protocol.getOriginalPdffileId() + ".jpg");
            
          //合同类型:0,火融e委托扣款授权书;1,火融e客户服务协议;2,扣款授权书;3,征信查询上报授权书;4,贷款合同
            if("0".equals(protocolType)){
                return "contract/WTKK";
            }else if("1".equals(protocolType)){
                return "contract/KHFW";
            }else if("2".equals(protocolType)){
                return "contract/KKSQS";
            }else if("3".equals(protocolType)){
                return "contract/ZXCX";
            }else if("4".equals(protocolType)){
                return "draw/KKHT";
            }
//            }
            
         }catch(Exception e){
            logger.info("showProtocol exception : ",e);
            model.addAttribute("code", "000002");
            model.addAttribute("message", "很抱歉，系统繁忙，请稍后再试");
        }
        return"myCredit/fail";  
     }
	
	/*@RequestMapping("/testInit")
    public String testInit(Model model, HttpServletRequest request) {
        return "/contract/testInit";
    }
    
    @RequestMapping("/testResult")
    public String testResult(Model model, HttpServletRequest request) {
        String merchantNo = request.getParameter("merchantNo");
        String prodCode = request.getParameter("prodCode");
        String lmtSerno = request.getParameter("lmtSerno");
        logger.info("test merchantNo is {},prodCode is {},lmtSerno is {}",merchantNo,prodCode,lmtSerno);
        
        JSONObject queryParam = new JSONObject();
        queryParam.put("merchant_no", merchantNo);
        queryParam.put("prod_code", prodCode);
        queryParam.put("lmt_serno", lmtSerno);
        
        String result = restTemplate.postForObject(contractUrl.concat("/testOpen"), queryParam, String.class);
        model.addAttribute("result", result);
        
        return "/contract/testResult";
    }*/
	
	private static void pdf2multiImage(String pdfFile, String outpath) {  
        try {  
            InputStream is = new FileInputStream(pdfFile);  
            PDDocument pdf = PDDocument.load(is);  
            int actSize  = pdf.getNumberOfPages();  
            List<BufferedImage> piclist = new ArrayList<BufferedImage>();  
            for (int i = 0; i < actSize; i++) {
            BufferedImage  image = new PDFRenderer(pdf).renderImageWithDPI(i,130,ImageType.RGB);  
                piclist.add(image); 
            }  
            yPic(piclist, outpath);  
            is.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    } 
	
	public static void yPic(List<BufferedImage> piclist, String outPath) {// 纵向处理图片  
        if (piclist == null || piclist.size() <= 0) {  
            System.out.println("图片数组为空!");  
            return;  
        }  
        try {  
            int height = 0, // 总高度  
            width = 0, // 总宽度  
            _height = 0, // 临时的高度 , 或保存偏移高度  
            __height = 0, // 临时的高度，主要保存每个高度  
            picNum = piclist.size();// 图片的数量  
            int[] heightArray = new int[picNum]; // 保存每个文件的高度  
            BufferedImage buffer = null; // 保存图片流  
            List<int[]> imgRGB = new ArrayList<int[]>(); // 保存所有的图片的RGB  
            int[] _imgRGB; // 保存一张图片中的RGB数据  
            for (int i = 0; i < picNum; i++) {  
                buffer = piclist.get(i);  
                heightArray[i] = _height = buffer.getHeight();// 图片高度  
                if (i == 0) {  
                    width = buffer.getWidth();// 图片宽度  
                }  
                height += _height; // 获取总高度  
                _imgRGB = new int[width * _height];// 从图片中读取RGB  
                _imgRGB = buffer.getRGB(0, 0, width, _height, _imgRGB, 0, width);  
                imgRGB.add(_imgRGB);  
            }  
            _height = 0; // 设置偏移高度为0  
            // 生成新图片  
            BufferedImage imageResult = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  
            for (int i = 0; i < picNum; i++) {  
                __height = heightArray[i];  
                if (i != 0) _height += __height; // 计算偏移高度  
                imageResult.setRGB(0, _height, width, __height, imgRGB.get(i), 0, width); // 写入流中  
            }  
            File outFile = new File(outPath);  
            ImageIO.write(imageResult, "jpg", outFile);// 写图片  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    } 
}
