package com.github.wycm;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeettestCrawler {
    private static String basePath = "src/main/resources/";
    private static String FULL_IMAGE_NAME = "full-image";
    private static String divClass = "mCaptchaImgDiv";
    private static String picExt = ".png";
    private static int[][] moveArray = new int[40][2];
    private static boolean moveArrayInit = false;
    private static String INDEX_URL = "http://captchas.wanmei.com/demo/mCaptcha";
    private static WebDriver driver;

    static {
        System.setProperty("webdriver.chrome.driver", "D:/workspace/chromedriver_win32/chromedriver.exe");
        //非windows操作系统选择如下路径
//        if (!System.getProperty("os.name").toLowerCase().contains("windows")){
//            System.setProperty("webdriver.chrome.driver", "D:/workspace/chromedriver_win32/chromedriver.exe");
//        }
        System.out.println(System.getProperty("os.name"));
        driver = new ChromeDriver();
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10; i++){
            try {
                invoke();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        driver.quit();
    }
    private static void invoke() throws IOException, InterruptedException {
        //设置input参数
        driver.get(INDEX_URL);

        //FIXME 通过[class=gt_slider_knob gt_show]
        By btnBy = By.cssSelector(".mCaptchaSlideBorder");
        waitForLoad(driver, btnBy);
        WebElement moveButton = driver.findElement(btnBy);
        By picDiv = By.cssSelector("."+divClass);
        waitForLoad(driver, picDiv);
        //获得原始图片
        String originImgName= getOriginImg(driver);
        //拉动滑块
        startDragBlock(originImgName,moveButton);
        //FIXME for test
        distance  = 200;

//        int i = 0;
//        while (i++ < 15){
//            int distance = getMoveDistance(driver);
//            //FIXME for test
//            distance  = 200;
//            move(driver, moveElemet, distance - 6);
//            By gtTypeBy = By.cssSelector(".gt_info_type");
//            By gtInfoBy = By.cssSelector(".gt_info_content");
//            waitForLoad(driver, gtTypeBy);
//            waitForLoad(driver, gtInfoBy);
//            String gtType = driver.findElement(gtTypeBy).getText();
//            String gtInfo = driver.findElement(gtInfoBy).getText();
//            System.out.println(gtType + "---" + gtInfo);
//            /**
//             * 再来一次：
//             * 验证失败：
//             */
//            if(!gtType.equals("再来一次:") && !gtType.equals("验证失败:")){
//                Thread.sleep(4000);
//                System.out.println(driver);
//                break;
//            }
//            Thread.sleep(4000);
//        }
    }

    /**
     * 移动
     * @param driver
     * @param element
     * @param distance
     * @throws InterruptedException
     */
    public static void move(WebDriver driver, WebElement element, int distance) throws InterruptedException {
        printLocation(element);
        System.out.println("应平移距离：" + distance);
        Actions actions = new Actions(driver);
        //按下鼠标左键
        new Actions(driver).clickAndHold(element).perform();

        int xMoveDistance = 0;
        //每次移动的量
        int xStep = 10;
        while(xMoveDistance<distance){
            if(xMoveDistance+xStep > distance){
                xStep = distance - xMoveDistance;
            }
            xMoveDistance+=xStep;
            actions.moveByOffset(xStep, 0).perform();
            //打印控件位置
            printLocation(element);
            System.out.println("move distance = "+xMoveDistance);
            //每0.5s移动一次
            Thread.sleep(500);
        }
        //松开鼠标左键
        actions.release(element).perform();
//        Thread.sleep(200);
//        printLocation(element);
//        actions.moveToElement(element, moveX, moveY).perform();
//        System.out.println(moveX + "--" + moveY);
        printLocation(element);
//        for (int i = 0; i < 22; i++){
//            int s = 10;
//            if (i % 2 == 0){
//                s = -10;
//            }
//            actions.moveToElement(element, s, 1).perform();
////            printLocation(element);
//            Thread.sleep(new Random().nextInt(100) + 150);
//        }
//
//        System.out.println(xDis + "--" + 1);
//        actions.moveByOffset(xDis, 1).perform();
//        printLocation(element);
//        Thread.sleep(200);

    }
    private static void printLocation(WebElement element){
        Point point  = element.getLocation();
        System.out.println(element.getAttribute("class")+" position = "+point.toString());
    }
    /**
     * 等待元素加载，10s超时
     * @param driver
     * @param by
     */
    public static void waitForLoad(final WebDriver driver, final By by){
        new WebDriverWait(driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                WebElement element = driver.findElement(by);
                if (element != null){
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 还原图片为未打乱状态
     * @param driver
     * @return
     * @throws IOException
     */
    public static String getOriginImg(WebDriver driver) throws IOException {
        String pageSource = driver.getPageSource();
        //获得图片URL
        String fullImageUrl = getFullImageUrl(pageSource);
        String initImageName = basePath + FULL_IMAGE_NAME + picExt;
        //将URL图片资源保存至本地文件
        FileUtils.copyURLToFile(new URL(fullImageUrl), new File(initImageName));
        initMoveArray(driver);
        String finalImageName = restoreImage(FULL_IMAGE_NAME,2,20);
        return finalImageName;
    }
    private static int difference(int[] a, int[] b){
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }
    /**
     * 获取move数组
     * @param driver
     */
    private static void initMoveArray(WebDriver driver){
        if (moveArrayInit){
            return;
        }
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elements = document.select("[class="+divClass+"]");
        int i = 0;
        for(Element element : elements){
            Pattern pattern = Pattern.compile(".*background-position: (-*\\d*)px (-*\\d*).*");

            Matcher matcher = pattern.matcher(element.toString());
            if (matcher.find()){
                String width = matcher.group(1);
                String height = matcher.group(2);
                moveArray[i][0] = Integer.parseInt(width);
                //FIXME is the i correct?
                moveArray[i++][1] = Integer.parseInt(height);
            } else {
                throw new RuntimeException("解析异常");
            }
        }
        moveArrayInit = true;
    }
    /**
     *还原图片
     * @param baseName
     */
    private static String restoreImage(String baseName,int row, int col) throws IOException {
        //把图片裁剪为row * col份
        int cutNum = row*col;
        for(int i = 0; i < cutNum; i++){
            ImageUtils.cutPic(basePath + baseName +picExt
                    ,basePath + "result/" + baseName + i + picExt, -moveArray[i][0], -moveArray[i][1], 13, 60);
        }
        //拼接图片
        String[] picColList = new String[col];//图片行向量
        String[] picRowList = new String[row];//图片列向量
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                picColList[j] = String.format(basePath+"result/"+baseName+"%d"+picExt,j+i*col);
            }
            //生成目标文件名
            picRowList[i] = String.format(basePath+"result/"+baseName+"row-%d"+picExt,i);
            //对图片列作拼接
            ImageUtils.jointImage(picColList,1,picRowList[i]);
        }
        String fileName = basePath+"result/"+baseName+"-final"+picExt;
        //对图片行作拼接
        ImageUtils.jointImage(picRowList,2,fileName);
        return fileName;

//        //删除产生的中间图片
//        for(int i = 0; i < 52; i++){
//            new File(basePath + "result/" + baseName + i + picExt).deleteOnExit();
//        }
//        new File(basePath + "result/" + baseName + "result1.jpg").deleteOnExit();
//        new File(basePath + "result/" + baseName + "result2.jpg").deleteOnExit();
    }
    /**
     * 获取原始图url
     * @param pageSource
     * @return
     */
    private static String getFullImageUrl(String pageSource){
        String url = null;
        Document document = Jsoup.parse(pageSource);
//        System.out.println("Document = "+document);
        String style = document.select("[class="+divClass+"]").first().attr("style");
        Pattern pattern = Pattern.compile("url\\(.*\\)");
        Matcher matcher = pattern.matcher(style);
        if (matcher.find()){
            url = matcher.group(0);
        }
        url = url.substring(4,url.lastIndexOf(')'));
        System.out.println(url);
        return url;
    }

    private static void startDragBlock(String originImgName,WebElement moveBtn){
        Actions actions = new Actions(driver);
        actions.clickAndHold(moveBtn);

    }


    /**
     * 确定拼图滑块的高度
     * @return
     */
//    private int findYposOfCubic(){
//
//    }
}
