package com.github.wycm;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author WeiWang Zhang
 * @date 2017/10/9.
 * @time 12:05.
 */
public class PageOperation {
    private WebDriver driver;
    private Actions actions;
    public PageOperation(){
        System.setProperty("webdriver.chrome.driver", "D:/workspace/chromedriver_win32/chromedriver.exe");
        driver = new ChromeDriver();
        actions = new Actions(driver);
        //设置加载等待时间
        driver.manage().timeouts().pageLoadTimeout(-1, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
    }

    public void openPage(String url){
        driver.get(url);
    }
    public void closePage(){
        driver.close();
    }

    /**
     * 获取原始图url
     * @return
     */
    public String findImgUrl(String divClass) {
        String url = null;
        String pageSource = driver.getPageSource();
        Document document = Jsoup.parse(pageSource);
        String style = document.select("[class=" + divClass + "]").first().attr("style");
        Pattern pattern = Pattern.compile("url\\(.*\\)");
        Matcher matcher = pattern.matcher(style);
        if (matcher.find()) {
            url = matcher.group(0);
        }
        url = url.substring(4, url.lastIndexOf(')'));
        //        System.out.println("func:getFullImageUrl =" + url);
        return url;
    }
    /**
     * 得到原始图片
     * @return
     * @throws IOException
     */




    /**
     * 等待元素加载，10s超时
     *
     * @param by
     */
    public void waitForLoad(final By by) {
        new WebDriverWait(driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                WebElement element = driver.findElement(by);
                if (element != null) {
                    return true;
                }
                return false;
            }
        });
    }


}
