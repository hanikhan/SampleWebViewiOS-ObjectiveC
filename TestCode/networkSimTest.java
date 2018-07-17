import io.appium.java_client.ios.IOSDriver;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class networkSimTest {
    public static String userName = "<BS_USERNAME>";
    public static String accessKey = "<BS_KEY>";

    public static void main(String args[]) throws IOException, InterruptedException {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("device", "iPhone 7");
      
        //Sample app for speed test - Uses https://speedsmart.net/ inside an iOS native app's webview
        caps.setCapability("app", "bs://<hash-id>");

        IOSDriver driver = new IOSDriver(new URL("https://" + userName + ":" + accessKey + "@hub-cloud.browserstack.com/wd/hub"), caps);
        //Setting implicit wait for 30 seconds
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        //Switch to webview context
        Set<String> handles = driver.getContextHandles();
        for(String handle: handles){
            if(handle.contains("WEBVIEW")){
                driver.context(handle);
                break;
            }
        }

        //Start a network test for default network configuration
        driver.findElement(By.id("start_button")).click();

        //Wait until speed test completes
        WebDriverWait wait = new WebDriverWait(driver, 90);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='button_restart']")));

        //Get SessionID for the app automate session
        String sessionID=((RemoteWebDriver) driver).getSessionId().toString();

        //Declare new network setting
        String networkSpeed = "edge-good";

        //Set the new network setting for the test session using the API
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            String authStr = userName+":"+accessKey;
            // encode data on your side using BASE64
            byte[] bytesEncoded = Base64.encodeBase64(authStr .getBytes());
            String authEncoded = new String(bytesEncoded);
            RequestBody body = RequestBody.create(mediaType, "{\"networkProfile\":\""+networkSpeed+"\"}");
            Request request = new Request.Builder()
                    .url("https://api-cloud.browserstack.com/app-automate/sessions/"+sessionID+"/update_network.json")
                    .put(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Basic "+authEncoded)
                    .addHeader("cache-control", "no-cache")
                    .build();

            //Check if the network setting was successfully set on the device
            Response response = client.newCall(request).execute();
            try {
                JSONObject responseJson = new JSONObject(response.body().string());
                if(responseJson.get("message").toString().equalsIgnoreCase("Completed")){
                    System.out.println("Network Set Successfully: "+networkSpeed);
                }
            }catch (Exception jsonException){
                jsonException.printStackTrace();
            }
        }catch (Exception e){
            System.out.println(e);
        }

        //Adding sleep for better video logs
        Thread.sleep(5000);
        driver.findElement(By.xpath("//div[@class='button_restart']")).click();

        //wait for speed test with new network setting to complete
        wait = new WebDriverWait(driver, 90);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='button_restart']")));
        ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);

        //Quit the session
        driver.quit();
    }
}
