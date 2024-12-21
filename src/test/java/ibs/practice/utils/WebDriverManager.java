package ibs.practice.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class WebDriverManager {

    private static final Properties props = new Properties();

    // Статический блок: подгружаем config.properties один раз
    static {
        try (InputStream input = new FileInputStream("src/test/resources/config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить config.properties: " + e.getMessage(), e);
        }
    }

    /**
     * Создаёт WebDriver в зависимости от типа запуска:
     *  - local: ChromeDriver или FirefoxDriver
     *  - remote: Selenoid (RemoteWebDriver + DesiredCapabilities)
     */
    public static WebDriver createDriver() {
        // Считываем настройки из System.getProperty(...) или, если нет - берем из props
        String runType = System.getProperty("type.run", props.getProperty("type.run", "local"));
        String browser = System.getProperty("type.browser", props.getProperty("type.browser", "chrome"));
        String browserVersion = System.getProperty("browser.version", props.getProperty("browser.version", "109.0"));
        String selenoidUrl = System.getProperty("selenoid.url", props.getProperty("selenoid.url", "http://localhost:4444/wd/hub"));

        // enableVNC / enableVideo (true/false) - читаем как boolean
        boolean enableVNC = Boolean.parseBoolean(
                System.getProperty("enableVNC", props.getProperty("enableVNC", "true"))
        );
        boolean enableVideo = Boolean.parseBoolean(
                System.getProperty("enableVideo", props.getProperty("enableVideo", "false"))
        );

        // Пути к локальным драйверам
        String chromePath = System.getProperty("chrome.driver.path", props.getProperty("chrome.driver.path", "src/test/resources/chromedriver.exe"));
        String geckoPath = System.getProperty("gecko.driver.path", props.getProperty("gecko.driver.path", "src/test/resources/geckodriver.exe"));

        WebDriver driver;

        if ("local".equalsIgnoreCase(runType)) {
            // --- Локальный запуск ---
            switch (browser.toLowerCase()) {
                case "chrome":
                    System.setProperty("webdriver.chrome.driver", chromePath);
                    driver = new ChromeDriver();
                    break;
                case "firefox":
                    System.setProperty("webdriver.gecko.driver", geckoPath);
                    driver = new FirefoxDriver();
                    break;
                default:
                    throw new IllegalArgumentException("Неизвестный браузер для локального запуска: " + browser);
            }

        } else {
            // --- Удалённый запуск (Selenoid) ---
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability("browserName", browser); // chrome / firefox / etc.
            capabilities.setCapability("browserVersion", browserVersion);

            // Параметры Selenoid
            Map<String, Object> selenoidOptions = new HashMap<>();
            selenoidOptions.put("enableVNC", enableVNC);
            selenoidOptions.put("enableVideo", enableVideo);

            // В новых версиях Selenium использовать именно "selenoid:options"
            capabilities.setCapability("selenoid:options", selenoidOptions);

            try {
                driver = new RemoteWebDriver(
                        URI.create(selenoidUrl).toURL(),
                        capabilities
                );
            } catch (MalformedURLException e) {
                throw new RuntimeException("Некорректный URL для Selenoid: " + selenoidUrl, e);
            }
        }

        driver.manage().window().maximize();
        // При желании можете здесь выставить timeouts
        return driver;
    }
}
