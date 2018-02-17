package xyz.sysoul.client;

import net.dongliu.requests.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import xyz.sysoul.callback.MessageCallback;
import xyz.sysoul.common.TMSApiURL;
import xyz.sysoul.entity.SubmitForm;

import java.text.DateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoTMSClient {
    private static final Logger LOGGER = Logger.getLogger(AutoTMSClient.class);
    //消息发送失败重发次数
    private static final long RETRY_TIMES = 5;
    private Client client;
    private Session session;
    //线程开关
    private volatile boolean pollStarted;

    public AutoTMSClient(MessageCallback callback) {
        this.client = Client.pooled().maxPerRoute(5).maxTotal(10).build();
        this.session = client.session();
        login("", "");
    }

    public void login(String userId, String userPassword) {
        Response<String> response = session.post(TMSApiURL.LOGIN_URL.getUrl())
                .addHeader("User-Agent", TMSApiURL.USER_AGENT)
                .addHeader("Referer", TMSApiURL.LOGIN_URL.getReferer())
                .addParam("userId", userId)
                .addParam("userPassword", userPassword)
                .text();
        String userName = "";
        if (response.getStatusCode() == HttpStatus.SC_OK) {
            //登陆成功后获取用户名
            Response<String> responseForUserName = get(TMSApiURL.GET_USERNAME);
            Document document = Jsoup.parse(responseForUserName.getBody());
            userName = document.select("table").select("tr").select("td").get(2).select("span").first().text();
            LOGGER.info(userName + "，登录成功!");
        } else {
            LOGGER.info("登录失败!");
        }
        Response<String> taskQueryResponse = getTaskQuery();
        Document documentTaskQuery = Jsoup.parse(taskQueryResponse.getBody());
        //随机获取一条任务
        Elements trs = documentTaskQuery.selectFirst("table").select("tr");
        if (trs.size() < 2) {//没有任务
            LOGGER.info("没有可填写的任务");
        } else {
            int index = new Random().nextInt(trs.size() - 1);
            Element element = trs.get(index + 1).select("td").last().selectFirst("input");
            Pattern pattern = Pattern.compile("(?<=\\()[^\\)]+");
            Matcher matcher = pattern.matcher(element.attr("onclick"));
            String str = "";
            while (matcher.find()) {
                str = matcher.group().replaceAll("'", "");
            }
            String[] args = str.split(",");//args有四个参数分别是：shareId,taskId,jobKindId,ifMaster

            Response<String> checkResponse = doCheckHadDaily(args[0], new Date().toString());
            //返回为空说明验证通过，不为空，则直接alert返回字符串
            String checkResult = checkResponse.getBody();
            if (StringUtils.isNotEmpty(checkResult)) {
                LOGGER.info("此任务未通过系统校验，返回信息为：" + checkResult);
            }
            //默认校验当前日期的工作
            String beginDate = DateFormat.getDateInstance().format(new Date());
            Response<String> checkDelayedIfModifyResp = doCheckDelayedIfModify(beginDate, "0", args[0]);
            if (StringUtils.isNotEmpty(checkDelayedIfModifyResp.getBody())) {
                LOGGER.info("任务填写出错，错误信息为：" + checkDelayedIfModifyResp.getBody());
            }
        }

    }

    /**
     * 获取任务列表（只有一个，多个任务获取方式请自行尝试）
     *
     * @return
     */
    public Response<String> getTaskQuery() {
        Response<String> response = session.post(TMSApiURL.GET_TASK_QUERY.getUrl())
                .addHeader("User-Agent", TMSApiURL.USER_AGENT)
                .addHeader("Referer", TMSApiURL.GET_TASK_QUERY.getReferer())
                .addParam("acceptPerson", "")
                .addParam("cmpCustomer", "-1")
                .addParam("cmpModule", "-1")
                .addParam("cusDealPersons", "")
                .addParam("ifPageSize", "true")
                .addParam("importantLevel", "-1")
                .addParam("jobId", "")
                .addParam("order", "asc")
                .addParam("province", "-1")
                .addParam("queryEndDate", "")
                .addParam("sort", "-1")
                .addParam("tabFlag", "0")
                .addParam("taskKind", "-1")
                .addParam("taskProcess", "-1")
                .addParam("taskSource", "-1")
                .text();
        return response;
    }

    /**
     * 该任务今天的日报是否已经被填写
     *
     * @param shareId
     * @param dateStr
     * @return
     */
    public Response<String> doCheckHadDaily(String shareId, String dateStr) {
        Response<String> response = session.post(TMSApiURL.CHECK_HAD_DAILY.buildUrl(shareId, dateStr))
                .addHeader("User-Agent", TMSApiURL.USER_AGENT)
                .addHeader("Referer", TMSApiURL.CHECK_HAD_DAILY.getReferer())
                .addParam("shareId", shareId)
                .addParam("time", dateStr)
                .text();
        return response;
    }

    /**
     * 提交前检测是否补写日报，若是补写,检测是否允许补写
     *
     * @param beginDate 日报日期
     * @param ifModify  默认为0
     * @param shareId
     * @return
     */
    public Response<String> doCheckDelayedIfModify(String beginDate, String ifModify, String shareId) {
        Response<String> response = session.post(TMSApiURL.CHECK_HAD_DAILY_MODIFY.getUrl())
                .addHeader("User-Agent", TMSApiURL.USER_AGENT)
                .addHeader("Referer", TMSApiURL.CHECK_HAD_DAILY_MODIFY.getReferer())
                .addParam("beginDate", beginDate)
                .addParam("ifModify", ifModify)
                .addParam("shareId", shareId)
                .text();
        return response;
    }

    /**
     * 提交前检测是否将工时填写在加班工时中 正常工时大于0.1时，做这个验证
     *
     * @param overTime  加班工时
     * @param beginDate 日报日期
     * @return
     */
    public Response<String> doCheckOverTime(String overTime, String beginDate) {
        Response<String> response = session.post(TMSApiURL.CHECK_OVER_TIME.getUrl())
                .addHeader("User-Agent", TMSApiURL.USER_AGENT)
                .addHeader("Referer", TMSApiURL.CHECK_OVER_TIME.getReferer())
                .addParam("overTime", overTime)
                .addParam("beginDate", beginDate)
                .text();
        return response;
    }

    public Response<String> doSubmit(SubmitForm submitForm) {
        Response<String> response = session.post(TMSApiURL.DO_SUBMIT.getUrl())
                .addHeader("User-Agent", TMSApiURL.USER_AGENT)
                .addHeader("Referer", TMSApiURL.DO_SUBMIT.buildUrl())
                .addHeader("Content-Type", "multipart/form-data; boundary=-----------------------------7e2802d20882")
                .addParam("ifMaster", submitForm.getIfMaster())
                .addParam("jobKindId", submitForm.getJobKindId())
                .addParam("ifInherit", submitForm.getIfInherit())
                .addParam("ifAudit", submitForm.getIfAudit())
                .addParam("progressIdHid", submitForm.getProgressIdHid())
                .addParam("taskAssayHidden", submitForm.getTaskAssayHidden())
                .addParam("ifCompleteOld", submitForm.getIfCompleteOld())
                .addParam("shareId", submitForm.getShareId())
                .addParam("jobId", submitForm.getJobId())
                .addParam("taskNumber", submitForm.getTaskNumber())
                .addParam("customerId", submitForm.getCustomerId())
                .addParam("projectNumber", submitForm.getProjectNumber())
                .addParam("taskName", submitForm.getTaskName())
                .addParam("taskId", submitForm.getTaskId())
                .addParam("taskKindId", submitForm.getTaskKindId())
                .addParam("beginDate", submitForm.getBeginDate())
                .addParam("endDate", submitForm.getEndDate())
                .addParam("beginDateHour", submitForm.getBeginDateHour())
                .addParam("beginDateMinute", submitForm.getBeginDateMinute())
                .addParam("endDateHour", submitForm.getEndDateHour())
                .addParam("endDateMinute", submitForm.getEndDateMinute())
                .addParam("manHour", submitForm.getManHour())
                .addParam("overTime", submitForm.getOverTime())
                .addParam("totalHourWithOutSelf", submitForm.getTotalHourWithOutSelf())
                .addParam("ifComplete", submitForm.getIfComplete())
                .addParam("isInWbsManager", submitForm.getIsInWbsManager())
                .addParam("cmpletePer", submitForm.getCmpletePer())
                .addParam("predictRemainHours", submitForm.getPredictRemainHours())
                .addParam("taskSource", submitForm.getTaskSource())
                .addParam("taskSourceId", submitForm.getTaskSourceId())
                .addParam("workProduct", submitForm.getWorkProduct())
                .addParam("codeAmount", submitForm.getCodeAmount())
                .addParam("taskDepiction", submitForm.getTaskDepiction())
                .addParam("taskAssay", submitForm.getTaskAssay())
                .addParam("taskDeal", submitForm.getTaskDeal())
                .addParam("isLast", submitForm.getIsLast())
                .text();
        return response;
    }

    //发送get请求
    private Response<String> get(TMSApiURL url, Object... params) {
        HeadOnlyRequestBuilder request = session.get(url.buildUrl(params))
                .addHeader("User-Agent", TMSApiURL.USER_AGENT);
        if (url.getReferer() != null) {
            request.addHeader("Referer", url.getReferer());
        }
        return request.text();
    }

    public static void main(String[] args) {
        MixinHeadOnlyRequestBuilder requestBuilder = Requests.get("https://fangjia.911cha.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0")
                .addHeader("Referer", "http://www.911cha.com/");
        Response<String> response = requestBuilder.text();
        Document dailyDocument = Jsoup.parse(response.getBody());
        Elements elements = dailyDocument.getElementsByAttributeValue("valign", "top");
        for (Element element : elements) {
//            System.out.println(element.);
        }
        System.out.println(DateFormat.getDateInstance().format(new Date()));


    }

}
