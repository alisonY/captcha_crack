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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeettestCrawler {
    private static String basePath = "src/main/resources/";
    private static String FULL_IMAGE_NAME = "full-image";
    private static String divClass = "mCaptchaImgDiv";
    private static String picExt = ".png";
    private static int[][] moveArray = new int[40][2];
    private static boolean moveArrayInit = false;
    private static String INDEX_URL = "http://captchas.wanmei.com/demo/mCaptcha?capType=float";
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
            try {
                invoke();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//        driver.quit();
    }

    private static void invoke() throws IOException, InterruptedException {
        //设置input参数
        driver.get(INDEX_URL);
        Actions actions = new Actions(driver);
        //FIXME 通过[class=gt_slider_knob gt_show]
        By btnBy = By.cssSelector(".mCaptchaSlideBorder");
        waitForLoad(driver, btnBy);
        WebElement moveButton = driver.findElement(btnBy);
        //将鼠标移动至滑块上（浮动式滑验需要放置鼠标后才加载图片）
        actions.moveToElement(moveButton);
        //等待滑验图片加载
        By picDiv = By.cssSelector("." + divClass);
        waitForLoad(driver, picDiv);
        //获得未打乱的图片
        String finalImage = getOriginImg(driver);
        //获得滑块的Y轴坐标
        String initImageName = basePath + FULL_IMAGE_NAME + picExt;
        getBlockYPos(initImageName);
        int x = getMoveDis(finalImage,initImageName);
        System.out.println("需要移动的距离为："+x);
        move(driver,moveButton,x);

        //FIXME for test
        //        distance  = 200;

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
     *
     * @param driver
     * @param element
     * @param distance
     * @throws InterruptedException
     */
    public static void move(WebDriver driver, WebElement element, int distance) throws InterruptedException {
        //距离补偿
        distance = distance + 12;
        printLocation(element);
        System.out.println("应平移距离：" + distance);
        Actions actions = new Actions(driver);
        //按下鼠标左键
        new Actions(driver).clickAndHold(element).perform();

        int xMoveDistance = 0;
        //每次移动的量
        int xStep = 10;
        while (xMoveDistance < distance) {
            if (xMoveDistance + xStep > distance) {
                xStep = distance - xMoveDistance;
            }
            xMoveDistance += xStep;
            actions.moveByOffset(xStep, 0).perform();
            //打印控件位置
            printLocation(element);
            System.out.println("move distance = " + xMoveDistance);
            //每0.2s移动一次
            Thread.sleep(200);
        }

        //FIXME TEST
        By btnBy = By.cssSelector(".sliderImgOuterContainerWrapper");
        element = driver.findElement(btnBy);
        //松开鼠标左键
        actions.release().perform();
        printLocation(element);
    }

    private static void printLocation(WebElement element) {
        Point point = element.getLocation();
        System.out.println(element.getAttribute("class") + " position = " + point.toString());
    }

    /**
     * 等待元素加载，10s超时
     *
     * @param driver
     * @param by
     */
    public static void waitForLoad(final WebDriver driver, final By by) {
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

    /**
     * 还原图片为未打乱状态
     *
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
        String finalImageName = restoreImage(FULL_IMAGE_NAME, 2, 20);
        return finalImageName;
    }


    /**
     * 获取move数组
     *
     * @param driver
     */
    private static void initMoveArray(WebDriver driver) {
        if (moveArrayInit) {
            return;
        }
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elements = document.select("[class=" + divClass + "]");
        int i = 0;
        for (Element element : elements) {
            Pattern pattern = Pattern.compile(".*background-position: (-*\\d*)px (-*\\d*).*");

            Matcher matcher = pattern.matcher(element.toString());
            if (matcher.find()) {
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
     * 还原图片
     *
     * @param baseName
     */
    private static String restoreImage(String baseName, int row, int col) throws IOException {
        //把图片裁剪为row * col份
        int cutNum = row * col;
        for (int i = 0; i < cutNum; i++) {
            BufferedImage bImg = ImageUtils.getCutPic(basePath + baseName + picExt, -moveArray[i][0], -moveArray[i][1], 13, 60);
            //在本地存储图片
            ImageUtils.witeImg(bImg, basePath + "result/" + baseName + i + picExt);
    }
        //拼接图片
        String[] picColList = new String[col];//图片行向量
        String[] picRowList = new String[row];//图片列向量
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                picColList[j] = String.format(basePath + "result/" + baseName + "%d" + picExt, j + i * col);
            }
            //生成目标文件名
            picRowList[i] = String.format(basePath + "result/" + baseName + "row-%d" + picExt, i);
            //对图片列作拼接
            ImageUtils.jointImage(picColList, 1, picRowList[i]);
        }
        String fileName = basePath + "result/" + baseName + "-final" + picExt;
        //对图片行作拼接
        ImageUtils.jointImage(picRowList, 2, fileName);
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
     *
     * @param pageSource
     * @return
     */
    private static String getFullImageUrl(String pageSource) {
        String url = null;
        Document document = Jsoup.parse(pageSource);
        //        System.out.println("Document = "+document);
        String style = document.select("[class=" + divClass + "]").first().attr("style");
        Pattern pattern = Pattern.compile("url\\(.*\\)");
        Matcher matcher = pattern.matcher(style);
        if (matcher.find()) {
            url = matcher.group(0);
        }
        url = url.substring(4, url.lastIndexOf(')'));
        System.out.println(url);
        return url;
    }

    private static int getBlockYPos(String originImgName) {
        int width = 61;
        int height = 120;
        BufferedImage blockImg = ImageUtils.getCutPic(originImgName, 260, 0, width, height);
        int[][] gray = ImageUtils.getGray(blockImg);
        int middle = 0;
        for (int i = 0; i <width; i++) {
            int first = -1;//第一个不为0的像素块位置
            int last = -1;//最后一个不为0的像素块位置
            for (int j = 0; j < height - 1; j++) {
                if (gray[i][j] > 0 && first < 0) {
                    first = j;
                }
                if (gray[i][j] > 0 && gray[i][j + 1] == 0) {
                    last = j;
                }
            }
//            System.out.println("第"+i+"列:"+first+" -----  "+last);
            if(first !=-1&&last != -1){
                middle = (first+last)/2;
                break;
            }
        }


        //        BufferedImage grayImg = ImageUtils.generateGrayImage(gray,120,61);
        //        ImageUtils.showImage(blockImg);
        //        ImageUtils.witeImg(grayImg,basePath+"/test.jpg");

        return middle;
    }

    public static int getMoveDis(String finalImage,String initImageName){
        //完整图像
        BufferedImage fImg = ImageUtils.getPic(finalImage);
        //拼图块
        BufferedImage block = ImageUtils.getCutPic(initImageName,260,0,61,120);
        //获得灰度图像
        int[][] fImgGrayPixel= ImageUtils.getGray(fImg);
        int[][] blockGrayPixel = ImageUtils.getGray(block);
        //保存灰度图像
        ImageUtils.witeImg(ImageUtils.generateGrayImage(fImgGrayPixel),basePath+"final-gray"+picExt);
        ImageUtils.witeImg(ImageUtils.generateGrayImage(blockGrayPixel),basePath+"block-gray"+picExt);
        //对滑块部分做纯色处理
        for (int i = 0; i < 61; i++) {
            for (int j = 0; j < 120; j++) {
                if (blockGrayPixel[i][j]>10){
                    blockGrayPixel[i][j] = 225;
                }else{
                    blockGrayPixel[i][j] = 0;
                }
            }
        }

        //拉普拉斯变换
        fImgGrayPixel = ImageUtils.laplace(fImgGrayPixel);
        blockGrayPixel = ImageUtils.laplace(blockGrayPixel);
        //保存拉布拉斯变换结果
        ImageUtils.witeImg(ImageUtils.generateGrayImage(fImgGrayPixel),basePath+"final-gray-laplace"+picExt);
        ImageUtils.witeImg(ImageUtils.generateGrayImage(blockGrayPixel),basePath+"block-gray-laplace"+picExt);
        //匹配滑块
        return matchBlock(fImgGrayPixel,blockGrayPixel);
    }

    public static int matchBlock(int[][] img, int[][] block){
        int imgW = img.length;
        int imgH = img[0].length;
        int bW = block.length;
        int bH = block[0].length;
        int maxSum = 0;
        int matchPos = 0;
        for (int i = 0; i < imgW-bW+1; i++) {
            int sum = 0;
            for (int j = 0; j < bW; j++) {
                for (int k = 0; k < bH; k++) {
                    sum+= img[i+j][k]*block[j][k];
                }
            }
            if(sum>maxSum){
                maxSum = sum;
                matchPos = i;
            }
        }
        return matchPos;
    }





    /**
     * 确定拼图滑块的高度
     * @return
     */
    //    private int findYposOfCubic(){
    //
    //    }
}
