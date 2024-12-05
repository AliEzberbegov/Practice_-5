package ibs.practice.steps;

import io.cucumber.java.ru.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AddProductSteps {

    private WebDriver driver;
    private static final Logger logger = LoggerFactory.getLogger(AddProductSteps.class);
    private static final String URL = "http://localhost:8080";
    private static final String DB_URL = "jdbc:h2:tcp://localhost:9092/mem:testdb";
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "pass";

    @Дано("стенд QualIT запущен и подключен к БД, открыта страница по адресу {string}")
    public void стенд_QualIT_запущен_и_подключен_к_БД(String url) {

        System.setProperty("webdriver.chrome.driver", "src/test/resources/chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get(url);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        logger.info("Тестирование стенда QualIT: {}", URL);

        // Открываем меню "Песочница"
        WebElement sandButton = driver.findElement(By.xpath("//li[@class='nav-item dropdown']"));
        sandButton.click();

        // Открываем страницу "Товары"
        WebElement productButton = driver.findElement(By.xpath("//a[@href='/food']"));
        productButton.click();
    }

    @Допустим("товары {string} и {string} отсутствуют в БД")
    public void товары_и_отсутствуют_в_БД(String productName, String productName1) throws SQLException {
        checkIfProductExistsInDB(productName, false);
        checkIfProductExistsInDB(productName1, false);
    }

    @Когда("пользователь добавляет товар с наименованием {string}, типом {string} и статусом чекбокса {string}")
    public void пользователь_добавляет_товар(String productName, String productType, String exoticStatus) {
        boolean isExotic = exoticStatus.equalsIgnoreCase("экзотический");
        addProduct(productName, productType, isExotic);
    }

    @Тогда("в списке товаров отображается товар {string}")
    public void в_списке_товаров_отображается(String productName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement productRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'" + productName + "')]")));
        assertTrue(productRow.isDisplayed(), "Продукт " + productName + " должен быть отображен в списке товаров.");
    }

    @И("товар {string} добавлен в БД")
    public void товар_добавлен_в_БД(String productName) throws SQLException {
        checkIfProductExistsInDB(productName, true);
    }

    @Тогда("выводится список товаров из БД с добавленными продуктами")
    public void вывод_списка_товаров_из_БД_с_новыми_товарами() throws SQLException {
        productsFromDB();
    }

    @Затем("выполняется удаление добавленных товаров {string} и {string} из базы данных")
    public void удаление_товаров_из_БД(String productName, String productName1) {
        try {
            deleteProductsFromDB(productName);
            deleteProductsFromDB(productName1);
            logger.info("Товары " + productName + " и " + productName1 + " успешно удалены из БД!");
            productsFromDB();

        } catch (Exception e) {
            logger.error("Ошибка во время удаления данных из БД: {}", e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @Затем("выполняется удаление товаров {string} и {string} через интерфейс стенда")
    public void сброс_данных_через_интерфейс(String productName, String productName1) throws InterruptedException {
        dataResetSandBoxMenu();
        logger.info("Сброс данных выполнен! Продукты " + productName + " и " + productName1 + " удалены из списка товаров.");

        if (driver != null) {
            Thread.sleep(3000);
            driver.quit();
        }
    }

    // Метод для добавления товара
    private void addProduct(String productName, String productType, boolean isExotic) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Открываем диалоговое окно для добавления товара
        WebElement addButton = driver.findElement(By.xpath("//button[@data-target='#editModal']"));
        addButton.click();
        logger.info("Открыто диалоговое окно добавления товара.");

        // Заполняем данные в диалоговом окне
        WebElement nameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='name']")));
        nameField.clear();
        nameField.sendKeys(productName);
        assertEquals(productName, nameField.getAttribute("value"),
                "Наименование продукта должно совпадать.");

        WebElement typeDropdown = driver.findElement(By.xpath("//select[@id='type']"));
        Select selectType = new Select(typeDropdown);
        selectType.selectByVisibleText(productType);
        String selectedType = selectType.getFirstSelectedOption().getText();
        assertEquals(productType, selectedType,
                "Тип продукта должен быть '" + productType + "'.");

        WebElement exoticCheckbox = driver.findElement(By.xpath("//input[@id='exotic']"));
        if (isExotic && !exoticCheckbox.isSelected()) {
            exoticCheckbox.click();
        } else if (!isExotic && exoticCheckbox.isSelected()) {
            exoticCheckbox.click();
        }
        assertEquals(isExotic, exoticCheckbox.isSelected(),
                "Чекбокс 'Экзотический' должен быть " + (isExotic ? "активирован" : "не активирован") + ".");

        // Сохраняем товар
        WebElement saveButton = driver.findElement(By.xpath("//button[@id='save']"));
        saveButton.click();
        logger.info("Продукт '{}' успешно добавлен.", productName);

        // Ожидаем закрытия диалогового окна
        wait.until(ExpectedConditions.invisibilityOf(saveButton));
    }

    // Метод для проверки наличия товаров в БД
    private void checkIfProductExistsInDB(String productName, boolean shouldExist) throws SQLException {

        // Соединение с БД
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {

            // Отправка и выполнение запроса
            String query = "SELECT * FROM food WHERE food_name = '" + productName + "';";
            try (ResultSet resultSet = statement.executeQuery(query)) {
                boolean exists = resultSet.next();

                if (shouldExist) {
                    assertTrue(exists, "Продукт '" + productName + "' должен существовать в БД.");
                    logger.info("Продукт '{}' найден в БД.", productName);
                } else {
                    assertFalse(exists, "Продукт '" + productName + "' не должен существовать в БД.");
                    logger.info("Продукт '{}' отсутствует в БД.", productName);
                }
            }
        }
    }

    // Метод для вывода всей таблицы продуктов БД
    private void productsFromDB() throws SQLException {

        // Соединение с БД
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {

            // Отправка и выполнение запроса
            String query = "SELECT FOOD_ID, FOOD_NAME, FOOD_TYPE, FOOD_EXOTIC FROM FOOD";
            try (ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    int food_id = resultSet.getInt("FOOD_ID");
                    String food_name = resultSet.getString("FOOD_NAME");
                    String food_type = resultSet.getString("FOOD_TYPE");
                    boolean food_exotic = resultSet.getBoolean("FOOD_EXOTIC");
                    logger.info("id:{}; name:{}; type:{}; exotic:{}.", food_id, food_name, food_type, food_exotic);
                }
            }
        }
    }

    // Метод для удаления товаров из БД
    private void deleteProductsFromDB(String productName) throws SQLException {

        // Соединение с БД
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {

            // Отправка запроса на удаление данных
            String query = "DELETE FROM food WHERE food_name = '" + productName + "';";
            statement.executeUpdate(query);
        }
    }

    //Метод для удаления товаров через интерфейс
    public void dataResetSandBoxMenu() throws InterruptedException{

        WebElement sandboxMenu = driver.findElement(By.xpath("//li[@class='nav-item dropdown']"));
        sandboxMenu.click();

        WebElement sandboxMenuClear = driver.findElement(By.xpath("//a[@id='reset']"));
        sandboxMenuClear.click();
    }
}

