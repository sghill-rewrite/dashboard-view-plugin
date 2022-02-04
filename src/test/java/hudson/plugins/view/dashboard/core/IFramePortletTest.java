package hudson.plugins.view.dashboard.core;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.plugins.view.dashboard.Dashboard;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class IFramePortletTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  @Issue("SECURITY-2559")
  public void iframePortletValidation() throws Exception {
    j.createFreeStyleProject("bar");

    Dashboard dashboard = new Dashboard("dash1");
    dashboard.setIncludeRegex(".*");
    j.jenkins.addView(dashboard);

    for (String invalid :
      Arrays.asList("", "<img/src/onerror=alert(\"XSS\")>", "ftp://foo.com", "javascript:alert(1)")) {
      IframePortlet iframePortlet = new IframePortlet("bar", "");
      iframePortlet.setIframeSource(invalid);
      dashboard.getBottomPortlets().clear();
      dashboard.getBottomPortlets().add(iframePortlet);
      assertThat(iframePortlet.isIframeSourceValid(), is(false));
      HtmlPage page = j.createWebClient().goTo("view/dash1/");
      assertThat(findError(page), hasSize(1));
      assertThat(findIFrame(page), is(emptyIterable()));
    }

    for (String valid :
      Arrays.asList(j.getURL().toString(), j.getURL().toString() + "/job/bar")) {
      IframePortlet iframePortlet = new IframePortlet("bar", valid);
      iframePortlet.setIframeSource(valid);
      dashboard.getBottomPortlets().clear();
      dashboard.getBottomPortlets().add(iframePortlet);
      assertThat(valid + " is not valid", iframePortlet.isIframeSourceValid(), is(true));
      HtmlPage page = j.createWebClient().goTo("view/dash1/");
      assertThat(findError(page), is(emptyIterable()));
      assertThat(findIFrame(page), hasSize(1));
    }
  }

  private List<FrameWindow> findIFrame(HtmlPage page) {
    return page.getByXPath("//table[@id='portlet-bottomPortlets-0']//iframe");
  }

  private List<DomNode> findError(HtmlPage page) {
    return page.getByXPath("//div[@class='error']");
  }
}
