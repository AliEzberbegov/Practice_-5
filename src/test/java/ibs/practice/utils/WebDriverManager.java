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

    // Храним настройки в статическом блоке, чтобы прочитать их один раз при загрузке класса
    private static final Properties props = new Properties();

    static {
        try (InputStream input = new FileInputStream("src/test/resources/config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить config.properties: " + e.getMessage(), e);
        }
    }

    public static WebDriver createDriver() {

        // Сначала ищем переменные в System properties (переданные из Jenkins командой -Dtype.run=remote, и т.д.).
        // Если таких нет — читаем из config.properties (props).
        String runType = System.getProperty("type.run", props.getProperty("type.run", "local"));
        String browser = System.getProperty("type.browser", props.getProperty("type.browser", "chrome"));
        String selenoidUrl = System.getProperty("selenoid.url", props.getProperty("selenoid.url", "http://localhost:4444/wd/hub"));
        String browserVersion = System.getProperty("browser.version", props.getProperty("browser.version", "109.0"));

        WebDriver driver;

        // Определяем — локальный запуск или через Selenoid
        if ("local".equalsIgnoreCase(runType)) {

            // Локальный запуск
            switch (browser.toLowerCase()) {
                case "chrome":
                    System.setProperty("webdriver.chrome.driver", "src/test/resources/chromedriver.exe");
                    driver = new ChromeDriver();
                    break;

                case "firefox":
                    System.setProperty("webdriver.gecko.driver", "src/test/resources/geckodriver.exe");
                    driver = new FirefoxDriver();
                    break;

                default:
                    throw new IllegalArgumentException("Неизвестный браузер для локального запуска: " + browser);
            }

        } else {
            // Удалённый запуск (Selenoid)
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability("browserName", browser);      // chrome, firefox и т.д.
            capabilities.setCapability("browserVersion", browserVersion);

            // Параметры для Selenoid
            Map<String, Object> selenoidOptions = new HashMap<>();
            selenoidOptions.put("enableVNC", true);
            selenoidOptions.put("enableVideo", false);

            // Важно: в новых версиях Selenium нужно именно "selenoid:options"
            capabilities.setCapability("selenoid:options", selenoidOptions);

            try {
                driver = new RemoteWebDriver(
                        URI.create(selenoidUrl).toURL(),
                        capabilities
                );
            } catch (MalformedURLException e) {
                throw new RuntimeException("Некорректный адрес Selenoid: " + selenoidUrl, e);
            }
        }

        // Общие настройки веб-драйвера
        driver.manage().window().maximize();
        // Вы можете дополнительно установить timeouts и т.д. при желании

        return driver;
    }
}
