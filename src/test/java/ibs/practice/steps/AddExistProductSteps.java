package ibs.practice.steps;

import io.cucumber.java.ru.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


import static org.junit.jupiter.api.Assertions.*;

public class AddExistProductSteps {

    private WebDriver driver;
    private static final Logger logger = LoggerFactory.getLogger(AddExistProductSteps.class);
    private static final String URL = "http://localhost:8080";
    private static final String DB_URL = "jdbc:h2:tcp://localhost:9092/mem:testdb";
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "pass";

    @Дано("стенд QualIT запущен и подключен к БД, страница {string} открыта")
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

    @Допустим("товар {string} присутствует в БД")
    public void товары_присутствует_в_бд(String productName) throws SQLException {
        //Вывод продуктов, уже существующих в таблице товаров БД
        productsFromDB();
    }

    @Когда("пользователь добавляет существующий товар с наименованием {string}, типом {string} и статусом чекбокса {string}")
    public void пользователь_добавляет_существующий_товар(String productName, String productType, String exoticStatus) {
        boolean isExotic = exoticStatus.equalsIgnoreCase("экзотический");
        addProduct(productName, productType, isExotic);
    }

    @Тогда("продукт {string} успешно добавлен и отображается в списке товаров")
    public void продукт_успешно_добавлен(String productName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement productRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'" + productName + "')]")));
        assertTrue(productRow.isDisplayed(), "Продукт " + productName + " должен быть отображен в списке товаров.");
    }

    @И("продукт {string} найден в БД")
    public void продукт_найден_в_бд(String productName) throws SQLException {
        checkIfProductExistsInDB(productName);
    }

    @Тогда("выводятся все записи в БД с наименованием продукта {string}")
    public void вывод_продукта_из_бд(String productName) throws SQLException {
        searchMatchesInDB(productName);
    }

    @Затем("пользователь получает сообщение об ошибке дублирования продукта {string}")
    public void сообщение_об_ошибке(String productName) {
        logger.error("Возникла ошибка! Необходимо устранить проблему дублирования!");
        if (driver != null) {
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

    //Метод для проверки наличия товаров в БД
    private void checkIfProductExistsInDB(String productName) throws SQLException {

        //Соединение с БД
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        Statement statement = connection.createStatement();

        //Отправка и вывод запроса
        String products = "SELECT * FROM food WHERE food_name IN ('" + productName + "');";
        ResultSet resultSet = statement.executeQuery(products);
        if (resultSet.next()) {
            logger.info("Продукт '" + productName + "' найден в БД.");
        } else {
            logger.info("Продукт '" + productName + "' не найден в БД.");
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

    //Метод для поиска совпадений по наименованию товара
    private void searchMatchesInDB(String productName) throws SQLException {

        int countMatches = 0;

        //Соединение с БД
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        Statement statement = connection.createStatement();

        //Отправка и вывод запроса
        String products = "SELECT * from FOOD WHERE FOOD_NAME = '" + productName + "'";
        ResultSet resultSet = statement.executeQuery(products);
        while (resultSet.next()){
            int food_id = resultSet.getInt("FOOD_ID");
            String food_name = resultSet.getString("FOOD_NAME");
            String food_type = resultSet.getString("FOOD_TYPE");
            boolean food_exotic = resultSet.getBoolean("FOOD_EXOTIC");
            if (Objects.equals(food_name, "Помидор")) {
                countMatches++;
            }
            logger.info("id:" + food_id +
                    "; name:" + food_name +
                    "; type:" + food_type +
                    "; exotic:" + food_exotic + ".");
        }

        if (countMatches > 1) {
            logger.info("Товар " + productName +  " встречается в таблице более одного раза.");
        }
    }
}

