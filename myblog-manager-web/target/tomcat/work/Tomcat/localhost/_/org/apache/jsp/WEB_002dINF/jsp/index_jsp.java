/*
 * Generated by the Jasper component of Apache Tomcat
 * Version: Apache Tomcat/7.0.47
 * Generated at: 2018-12-19 06:09:28 UTC
 * Note: The last modified time of this file was set to
 *       the last modified time of the source file after
 *       generation to assist with modification tracking.
 */
package org.apache.jsp.WEB_002dINF.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class index_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final javax.servlet.jsp.JspFactory _jspxFactory =
          javax.servlet.jsp.JspFactory.getDefaultFactory();

  private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.tomcat.InstanceManager _jsp_instancemanager;

  public java.util.Map<java.lang.String,java.lang.Long> getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_instancemanager = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig());
  }

  public void _jspDestroy() {
  }

  public void _jspService(final javax.servlet.http.HttpServletRequest request, final javax.servlet.http.HttpServletResponse response)
        throws java.io.IOException, javax.servlet.ServletException {

    final javax.servlet.jsp.PageContext pageContext;
    javax.servlet.http.HttpSession session = null;
    final javax.servlet.ServletContext application;
    final javax.servlet.ServletConfig config;
    javax.servlet.jsp.JspWriter out = null;
    final java.lang.Object page = this;
    javax.servlet.jsp.JspWriter _jspx_out = null;
    javax.servlet.jsp.PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\r\n");
      out.write("<!DOCTYPE html>\r\n");
      out.write("<html>\r\n");
      out.write("\t<head>\r\n");
      out.write("\t\t<meta charset=\"utf-8\" />\r\n");
      out.write("\t\t<title>文章管理</title>\r\n");
      out.write("\t\t<link href=\"/css/Site.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n");
      out.write("\t\t<link href=\"/css/layout.css\" rel=\"stylesheet\" />\r\n");
      out.write("\t\t<link href=\"/css/form.css\" rel=\"stylesheet\" />\r\n");
      out.write("\t\t<link href=\"/css/font-awesome.min.css\" rel=\"stylesheet\" />\r\n");
      out.write("\t\t<style type=\"text/css\">\r\n");
      out.write("\t\t\t.headerlitooleulsubitem_gray {\r\n");
      out.write("\t\t\t\tbackground-Color: White;\r\n");
      out.write("\t\t\t\tposition: absolute;\r\n");
      out.write("\t\t\t\tborder-left: 1px solid #BCD4E5;\r\n");
      out.write("\t\t\t\tborder-right: 1px solid #BCD4E5;\r\n");
      out.write("\t\t\t\tborder-bottom: 1px solid #BCD4E5;\r\n");
      out.write("\t\t\t\tbox-shadow: 0px 0px 5px #BCD4E5;\r\n");
      out.write("\t\t\t\twidth: 230px;\r\n");
      out.write("\t\t\t\tmargin-left: -170px;\r\n");
      out.write("\t\t\t\tcolor: Black;\r\n");
      out.write("\t\t\t\tdisplay: none;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitooleulsubitem_red {\r\n");
      out.write("\t\t\t\tbackground-Color: White;\r\n");
      out.write("\t\t\t\tposition: absolute;\r\n");
      out.write("\t\t\t\tborder-left: 1px solid #E5BCD4;\r\n");
      out.write("\t\t\t\tborder-right: 1px solid #E5BCD4;\r\n");
      out.write("\t\t\t\tborder-bottom: 1px solid #E5BCD4;\r\n");
      out.write("\t\t\t\tbox-shadow: 0px 0px 5px #E5BCD4;\r\n");
      out.write("\t\t\t\twidth: 230px;\r\n");
      out.write("\t\t\t\tmargin-left: -170px;\r\n");
      out.write("\t\t\t\tcolor: Black;\r\n");
      out.write("\t\t\t\tdisplay: none;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitooleulsubitem_gray>li,\r\n");
      out.write("\t\t\t.headerlitooleulsubitem_red>li {\r\n");
      out.write("\t\t\t\theight: 45px;\r\n");
      out.write("\t\t\t\tbackground-color: White;\r\n");
      out.write("\t\t\t\tpadding: 0px 10px;\r\n");
      out.write("\t\t\t\tborder-bottom: 1px solid #E4ECF3;\r\n");
      out.write("\t\t\t\tfont-size: 14px;\r\n");
      out.write("\t\t\t\tline-height: 26px;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitoolelisubitemtitle_gray {\r\n");
      out.write("\t\t\t\theight: 35px !important;\r\n");
      out.write("\t\t\t\tline-height: 35px !important;\r\n");
      out.write("\t\t\t\tbackground-color: #ECF2F7 !important;\r\n");
      out.write("\t\t\t\tmargin: 0px !important;\r\n");
      out.write("\t\t\t\tcolor: #8090A0 !important;\r\n");
      out.write("\t\t\t\tfont-size: 14px;\r\n");
      out.write("\t\t\t\tfont-weight: bold;\r\n");
      out.write("\t\t\t\tborder-bottom: 1px solid #BCD4E5 !important;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitoolelisubitemtitle_red {\r\n");
      out.write("\t\t\t\theight: 35px !important;\r\n");
      out.write("\t\t\t\tline-height: 35px !important;\r\n");
      out.write("\t\t\t\tbackground-color: #F7ECF2 !important;\r\n");
      out.write("\t\t\t\tmargin: 0px !important;\r\n");
      out.write("\t\t\t\tcolor: #B471A0 !important;\r\n");
      out.write("\t\t\t\tfont-size: 14px;\r\n");
      out.write("\t\t\t\tfont-weight: bold;\r\n");
      out.write("\t\t\t\tborder-bottom: 1px solid #E5BCD4 !important;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitooleulsubitem_gray>li:hover {\r\n");
      out.write("\t\t\t\tbackground-Color: #F4F9FC;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitooleulsubitem_red>li:hover {\r\n");
      out.write("\t\t\t\tbackground-color: #FCF4F9;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.ulsubitemitems>li {\r\n");
      out.write("\t\t\t\tfloat: left;\r\n");
      out.write("\t\t\t\theight: 20px;\r\n");
      out.write("\t\t\t\tfont-size: 13px;\r\n");
      out.write("\t\t\t\tfont-weight: normal !important;\r\n");
      out.write("\t\t\t\tcolor: #555 !important;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.ulsubitemitems>li:last-child {\r\n");
      out.write("\t\t\t\tclear: both;\r\n");
      out.write("\t\t\t\twidth: 100%;\r\n");
      out.write("\t\t\t\theight: 10px;\r\n");
      out.write("\t\t\t\tmargin-top: 8px;\r\n");
      out.write("\t\t\t\tbackground-color: #DADADA;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitools_info {\r\n");
      out.write("\t\t\t\tbackground-color: #0a318d;\r\n");
      out.write("\t\t\t\theight: 45px;\r\n");
      out.write("\t\t\t\tpadding: 0px 10px;\r\n");
      out.write("\t\t\t\twidth: 135px;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headeruserface {\r\n");
      out.write("\t\t\t\tbackground-image: url('/resources/images/logo.png');\r\n");
      out.write("\t\t\t\twidth: 40px;\r\n");
      out.write("\t\t\t\theight: 40px;\r\n");
      out.write("\t\t\t\tbackground-color: white;\r\n");
      out.write("\t\t\t\tmargin: 2px 10px 2px 2px;\r\n");
      out.write("\t\t\t\tborder-radius: 20px;\r\n");
      out.write("\t\t\t\tfloat: left;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitools_ulinfo {\r\n");
      out.write("\t\t\t\tbackground-Color: white;\r\n");
      out.write("\t\t\t\tborder-left: 1px solid #D1D1D1;\r\n");
      out.write("\t\t\t\tborder-right: 1px solid #D1D1D1;\r\n");
      out.write("\t\t\t\tborder-bottom: 1px solid #D1D1D1;\r\n");
      out.write("\t\t\t\tbox-shadow: 0px 0px 5px #D1D1D1;\r\n");
      out.write("\t\t\t\twidth: 160px;\r\n");
      out.write("\t\t\t\tmargin-left: -20px;\r\n");
      out.write("\t\t\t\tcolor: Black;\r\n");
      out.write("\t\t\t\tdisplay: none;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitools_ulinfo>li {\r\n");
      out.write("\t\t\t\theight: 35px;\r\n");
      out.write("\t\t\t\tbackground-color: White;\r\n");
      out.write("\t\t\t\tpadding: 0px 10px;\r\n");
      out.write("\t\t\t\tfont-size: 14px;\r\n");
      out.write("\t\t\t\tline-height: 36px;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.headerlitools_ulinfo>li:hover {\r\n");
      out.write("\t\t\t\tbackground-Color: #FEE188;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.dvcontent {\r\n");
      out.write("\t\t\t\tpadding: 0px 20px;\r\n");
      out.write("\t\t\t\tmargin: 45px auto 0px auto;\r\n");
      out.write("\t\t\t\toverflow: auto;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.dvpagerecord {\r\n");
      out.write("\t\t\t\tpadding: 6px 12px;\r\n");
      out.write("\t\t\t\tbackground-color: #EAEFF2;\r\n");
      out.write("\t\t\t\tcolor: #2283C5;\r\n");
      out.write("\t\t\t\tfloat: left;\r\n");
      out.write("\t\t\t\tborder: 1px solid #D9D9D9;\r\n");
      out.write("\t\t\t\ttext-decoration: underline;\r\n");
      out.write("\t\t\t\tfont-size: 14px;\r\n");
      out.write("\t\t\t\tcursor: pointer;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.dvpagerecord:hover {\r\n");
      out.write("\t\t\t\tcursor: pointer;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.dvpagerecord:hover i {\r\n");
      out.write("\t\t\t\ttext-decoration: underline;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.ulpageRecord {\r\n");
      out.write("\t\t\t\tmax-width: 165px;\r\n");
      out.write("\t\t\t\tfloat: left;\r\n");
      out.write("\t\t\t\twhite-space: nowrap;\r\n");
      out.write("\t\t\t\toverflow: hidden;\r\n");
      out.write("\t\t\t\tmargin: 0px;\r\n");
      out.write("\t\t\t\tpadding: 0px;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.ulpageRecord li {\r\n");
      out.write("\t\t\t\tmargin: 0px;\r\n");
      out.write("\t\t\t\tpadding: 6px 12px;\r\n");
      out.write("\t\t\t\tbackground-color: #EAEFF2;\r\n");
      out.write("\t\t\t\tcolor: #2283C5;\r\n");
      out.write("\t\t\t\tdisplay: inline-block;\r\n");
      out.write("\t\t\t\tfont-size: 14px;\r\n");
      out.write("\t\t\t\tborder: 1px solid #D9D9D9;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.ulpageRecord li:hover {\r\n");
      out.write("\t\t\t\ttext-decoration: underline;\r\n");
      out.write("\t\t\t\tcursor: pointer;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t.currentPage {\r\n");
      out.write("\t\t\t\tbackground-color: #2468a9 !important;\r\n");
      out.write("\t\t\t\tcolor: white !important;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t#warn {\r\n");
      out.write("\t\t\t\twidth: 300px;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t#warn tr {\r\n");
      out.write("\t\t\t\twidth: 300px;\r\n");
      out.write("\t\t\t\theight: 20px;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t#warn tr td {\r\n");
      out.write("\t\t\t\twidth: 90px;\r\n");
      out.write("\t\t\t\theight: 20px;\r\n");
      out.write("\t\t\t\ttext-align: center;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\tiframe {\r\n");
      out.write("\t\t\t\tborder: none;\r\n");
      out.write("\t\t\t}\r\n");
      out.write("\t\t</style>\r\n");
      out.write("\t\t<link href=\"css/menu.css\" rel=\"stylesheet\" />\r\n");
      out.write("\t</head>\r\n");
      out.write("\r\n");
      out.write("\t<body>\r\n");
      out.write("\t\t<div class=\"dvheader\">\r\n");
      out.write("\t\t\t<div class=\"dvheadertools\">\r\n");
      out.write("\t\t\t\t<span class=\"headerspantitle\">文章管理系统</span>\r\n");
      out.write("\t\t\t\t<ul class=\"headerultools\">\r\n");
      out.write("\t\t\t\t</ul>\r\n");
      out.write("\t\t\t</div>\r\n");
      out.write("\t\t</div>\r\n");
      out.write("\t\t<div class=\"dvcontent\">\r\n");
      out.write("\r\n");
      out.write("\t\t\t<ul class=\"ulleftmenu\" style=\"border-right: 1px solid #ddd;\">\r\n");
      out.write("\r\n");
      out.write("\t\t\t\t<li class=\"limenuitem\">\r\n");
      out.write("\t\t\t\t\t<i class=\"icon-cog menuicon\"></i>菜单<b class=\"arrow icon-angle-down arrow-down\"></b>\r\n");
      out.write("\t\t\t\t\t<ul class=\"ulleftsubitems\">\r\n");
      out.write("\t\t\t\t\t\t");
      out.write("\r\n");
      out.write("\t\t\t\t\t\t<a href=\"/article_admin\" target=\"right\">\r\n");
      out.write("\t\t\t\t\t\t\t<li>文章管理</li>\r\n");
      out.write("\t\t\t\t\t\t</a>\r\n");
      out.write("\t\t\t\t\t</ul>\r\n");
      out.write("\t\t\t\t</li>\r\n");
      out.write("\t\t\t</ul>\r\n");
      out.write("\t\t\t<div style=\"position: absolute; left: 191px; right: 20px; \">\r\n");
      out.write("\t\t\t\t<iframe scrolling=\"no\" width=\"100%\" height=\"1200\" name=\"right\" border=\"none\">\r\n");
      out.write("\t\t\t\t\t");
      org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "article_admin.jsp", out, false);
      out.write("\r\n");
      out.write("\t\t\t\t</iframe>\r\n");
      out.write("\t\t\t</div>\r\n");
      out.write("\t\t</div>\r\n");
      out.write("\t\t<script src=\"js/jquery-1.12.4.min.js\" type=\"text/javascript\"></script>\r\n");
      out.write("\t\t<script src=\"js/plugs/Jqueryplugs.js\" type=\"text/javascript\"></script>\r\n");
      out.write("\t\t<script src=\"js/_layout.js\"></script>\r\n");
      out.write("\r\n");
      out.write("\t</body>\r\n");
      out.write("</html>");
    } catch (java.lang.Throwable t) {
      if (!(t instanceof javax.servlet.jsp.SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else throw new ServletException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}