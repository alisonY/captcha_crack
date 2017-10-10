package com.github.wycm;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    private final String DEFAULT_ORIGIN_IMG_NAME = "origin-image";
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
     * @param divClass 包含图片的标签class
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
     * 得到原始图片，下载到本地
     * @param url 图片地址
     * @param filePath 本地图片路径
     * @return
     * @throws IOException
     */
    public String downloadImg(String url, String filePath) throws IOException{
        if (StringUtil.isBlank(filePath)){
            filePath = "src/main/resources/origin-image.png";
        }
        //将URL图片资源保存至本地文件
        FileUtils.copyURLToFile(new URL(url), new File(filePath));
        return filePath;
    }

    /**
     * 获取图片排列的偏移矩阵
     * @param divClass 包含图片的标签class
     * @param row 图片分割行数
     * @param col 图片分割列数
     */
    private void getOffsetMat(String divClass, int row, int col) {
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elements = document.select("[class=" + divClass + "]");
        int i = 0;
        Integer[][] offsetMat = new Integer[row*col][2];
        for (Element element : elements) {
            Pattern pattern = Pattern.compile(".*background-position: (-*\\d*)px (-*\\d*).*");
            Matcher matcher = pattern.matcher(element.toString());
            if (matcher.find()) {
                String width = matcher.group(1);
                String height = matcher.group(2);
                offsetMat[i][0] = Integer.parseInt(width);
                offsetMat[i++][1] = Integer.parseInt(height);
            } else {
                throw new RuntimeException("解析异常");
            }
        }
    }



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
