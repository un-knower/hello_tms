package xyz.sysoul.common;

public enum TMSApiURL {
    LOGIN_URL(
            "http://59.46.175.74:5898/oa/login.do?fun=login",
            "http://59.46.175.74:5898/oa/login_out/login.jsp"
    ),
    GET_USERNAME(
            "http://59.46.175.74:5898/oa/login_out/topFrame.jsp",
            "http://59.46.175.74:5898/oa/login.do?fun=login"
    ),
    GET_TASK_QUERY(
            "http://59.46.175.74:5898/oa/taskQueryAction.do?method=doQuery",
            "http://59.46.175.74:5898/oa/taskQueryAction.do?method=doInit"
    ),
    CHECK_HAD_DAILY(
            "http://59.46.175.74:5898/oa/add_daily_report.do?method=doCheckHadDaily",
            "http://59.46.175.74:5898/oa/task_manage/mainPageQuery/taskQueryInfoContent.jsp"
    ),
    CHECK_HAD_DAILY_MODIFY(
            "http://59.46.175.74:5898/oa/add_daily_report.do?method=doCheckDelayed",
            "http://59.46.175.74:5898/oa/daily_report_manage/add_wbs_daily_report.jsp"
    ),
    CHECK_OVER_TIME(
            "http://59.46.175.74:5898/oa/add_daily_report.do?method=doCheckOverTime",
            "http://59.46.175.74:5898/oa/daily_report_manage/add_wbs_daily_report.jsp"
    ),
    DO_SUBMIT(
            "http://59.46.175.74:5898/oa/add_daily_report.do?method=doSubmit",
            "http://59.46.175.74:5898/oa/add_daily_report.do?method=doInit&taskId={1}&shareId={2}&jobKindId={3}&ifMaster={4}&flag={5}"
    );

    public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E)";
    private String url;

    private String referer;

    TMSApiURL(String url, String referer) {
        this.url = url;
        this.referer = referer;
    }

    public String getUrl() {
        return url;
    }


    public String getReferer() {
        return referer;
    }

    public String buildUrl(Object... params) {
        int i = 1;
        String url = this.url;
        for (Object param : params) {
            url = url.replace("{" + i++ + "}", param.toString());
        }
        return url;
    }

    public String getOrigin() {
        return this.url.substring(0, url.lastIndexOf("/"));
    }

}
